package com.example.demo.service;

import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
public class BanPickRankedRoomContextService {

    static final BanPickSeriesType VIRTUAL_SERIES_FORMAT = BanPickSeriesType.BO7;
    static final List<String> REQUIRED_ROLE_CODES = List.of("DSL", "JGL", "MID", "ADL", "SUP");

    private final HeroRepository heroRepository;
    private final Random random;

    @Autowired
    public BanPickRankedRoomContextService(HeroRepository heroRepository) {
        this(heroRepository, new SecureRandom());
    }

    BanPickRankedRoomContextService(HeroRepository heroRepository, Random random) {
        this.heroRepository = heroRepository;
        this.random = random;
    }

    public void prepareRankedRoom(BanPickRoom room) {
        if (room == null) {
            return;
        }
        if (!isRanked(room)) {
            clearGeneratedContext(room);
            return;
        }
        room.setSeriesType(BanPickSeriesType.BO1);
        room.setVirtualSeriesFormat(VIRTUAL_SERIES_FORMAT);
        if (room.getUltimateBattle() == null) {
            room.setUltimateBattle(false);
        }
        if (room.getPrepDurationSeconds() == null) {
            room.setPrepDurationSeconds(0);
        }
    }

    public void initializeContextIfMissing(BanPickRoom room) {
        if (!isRanked(room)) {
            return;
        }
        prepareRankedRoom(room);
        if (room.getVirtualGameIndex() != null) {
            normalizeDerivedFields(room);
            return;
        }
        applyContext(room, random.nextInt(VIRTUAL_SERIES_FORMAT.getMaxGames()) + 1);
    }

    public void clearGeneratedContext(BanPickRoom room) {
        if (room == null) {
            return;
        }
        room.setVirtualSeriesFormat(isRanked(room) ? VIRTUAL_SERIES_FORMAT : null);
        room.setVirtualGameIndex(null);
        room.setUltimateBattle(false);
        room.setPrepDurationSeconds(0);
        room.setBluePreviousUsedHeroIds(null);
        room.setRedPreviousUsedHeroIds(null);
        clearPrepWindow(room);
    }

    public void clearPrepWindow(BanPickRoom room) {
        if (room == null) {
            return;
        }
        room.setPrepPhaseStartAt(null);
        room.setPrepPhaseEndAt(null);
    }

    public void startPrepWindowIfNeeded(BanPickRoom room, LocalDateTime now) {
        if (!isRanked(room) || room == null) {
            return;
        }
        int prepDurationSeconds = room.getPrepDurationSeconds() != null ? Math.max(0, room.getPrepDurationSeconds()) : 0;
        if (prepDurationSeconds <= 0 || now == null) {
            clearPrepWindow(room);
            return;
        }
        room.setPrepPhaseStartAt(now);
        room.setPrepPhaseEndAt(now.plusSeconds(prepDurationSeconds));
    }

    public boolean isPrepPhaseActive(BanPickRoom room, LocalDateTime now) {
        if (room == null || room.getPrepPhaseEndAt() == null) {
            return false;
        }
        return now == null || now.isBefore(room.getPrepPhaseEndAt());
    }

    void applyContext(BanPickRoom room, int virtualGameIndex) {
        if (room == null) {
            return;
        }
        if (!isRanked(room)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rank Mode context chỉ áp dụng cho room RANKED.");
        }
        if (virtualGameIndex < 1 || virtualGameIndex > VIRTUAL_SERIES_FORMAT.getMaxGames()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "virtualGameIndex phải nằm trong khoảng 1-7.");
        }

        prepareRankedRoom(room);
        VirtualPreviousUsedContext previousUsedContext = buildPreviousUsedContext(virtualGameIndex);
        room.setVirtualGameIndex(virtualGameIndex);
        room.setUltimateBattle(virtualGameIndex == VIRTUAL_SERIES_FORMAT.getMaxGames());
        room.setPrepDurationSeconds(resolvePrepDurationSeconds(virtualGameIndex));
        room.setBluePreviousUsedHeroIds(serializeHeroIds(previousUsedContext.blueHeroIds()));
        room.setRedPreviousUsedHeroIds(serializeHeroIds(previousUsedContext.redHeroIds()));
        clearPrepWindow(room);
    }

    int resolvePrepDurationSeconds(int virtualGameIndex) {
        return switch (virtualGameIndex) {
            case 2 -> 30;
            case 3 -> 35;
            case 4 -> 40;
            case 5 -> 45;
            case 6 -> 50;
            default -> 0;
        };
    }

    private void normalizeDerivedFields(BanPickRoom room) {
        room.setVirtualSeriesFormat(VIRTUAL_SERIES_FORMAT);
        int virtualGameIndex = room.getVirtualGameIndex() != null ? room.getVirtualGameIndex() : 1;
        room.setUltimateBattle(virtualGameIndex == VIRTUAL_SERIES_FORMAT.getMaxGames());
        room.setPrepDurationSeconds(resolvePrepDurationSeconds(virtualGameIndex));
    }

    private VirtualPreviousUsedContext buildPreviousUsedContext(int virtualGameIndex) {
        int previousGamesCount = Math.max(0, virtualGameIndex - 1);
        if (previousGamesCount == 0) {
            return new VirtualPreviousUsedContext(List.of(), List.of());
        }

        int requiredHeroesPerRole = previousGamesCount * 2;
        Map<String, List<Long>> shuffledHeroIdsByRole = loadShuffledHeroIdsByRole(requiredHeroesPerRole);
        List<Long> blueHeroIds = new ArrayList<>(previousGamesCount * REQUIRED_ROLE_CODES.size());
        List<Long> redHeroIds = new ArrayList<>(previousGamesCount * REQUIRED_ROLE_CODES.size());

        for (int gameIndex = 0; gameIndex < previousGamesCount; gameIndex += 1) {
            for (String roleCode : REQUIRED_ROLE_CODES) {
                List<Long> heroIds = shuffledHeroIdsByRole.get(roleCode);
                blueHeroIds.add(heroIds.get(gameIndex * 2));
                redHeroIds.add(heroIds.get(gameIndex * 2 + 1));
            }
        }
        return new VirtualPreviousUsedContext(blueHeroIds, redHeroIds);
    }

    private Map<String, List<Long>> loadShuffledHeroIdsByRole(int requiredHeroesPerRole) {
        Map<String, List<Long>> heroIdsByRole = new LinkedHashMap<>();
        REQUIRED_ROLE_CODES.forEach(roleCode -> heroIdsByRole.put(roleCode, new ArrayList<>()));

        for (Hero hero : heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()) {
            if (hero == null || hero.getId() == null || hero.getPrimaryRole() == null) {
                continue;
            }
            String roleCode = normalizeRoleCode(hero.getPrimaryRole().getCode());
            if (!StringUtils.hasText(roleCode)) {
                continue;
            }
            heroIdsByRole.get(roleCode).add(hero.getId());
        }

        for (String roleCode : REQUIRED_ROLE_CODES) {
            List<Long> heroIds = heroIdsByRole.get(roleCode);
            if (heroIds == null || heroIds.size() < requiredHeroesPerRole) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Rank Mode blocked: heroes.primary_role_id cho role "
                                + roleCode
                                + " cần ít nhất "
                                + requiredHeroesPerRole
                                + " hero unique, nhưng hiện chỉ có "
                                + (heroIds != null ? heroIds.size() : 0)
                                + "."
                );
            }
            Collections.shuffle(heroIds, random);
        }
        return heroIdsByRole;
    }

    private boolean isRanked(BanPickRoom room) {
        return room != null && BanPickMatchMode.defaultIfNull(room.getMode()) == BanPickMatchMode.RANKED;
    }

    private String normalizeRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return "";
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        return REQUIRED_ROLE_CODES.contains(normalized) ? normalized : "";
    }

    private String serializeHeroIds(List<Long> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) {
            return null;
        }
        return heroIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    record VirtualPreviousUsedContext(List<Long> blueHeroIds, List<Long> redHeroIds) {
    }
}
