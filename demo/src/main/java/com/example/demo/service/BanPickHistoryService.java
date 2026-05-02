package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickProfileResponse;
import com.example.demo.dto.banpick.BanPickUserSummary;
import com.example.demo.dto.banpick.DraftHistoryResponse;
import com.example.demo.dto.banpick.HeroPickStatResponse;
import com.example.demo.dto.banpick.PlayerStatsResponse;
import com.example.demo.dto.banpick.RecordDraftWinnerRequest;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.Hero;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.PlayerStatsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class BanPickHistoryService {

    private static final int STARTING_RATING = 1000;
    private static final int RATING_DELTA = 15;
    private static final int LEADERBOARD_LIMIT = 50;

    private final DraftHistoryRepository draftHistoryRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final UserRepository userRepository;
    private final HeroRepository heroRepository;

    public BanPickHistoryService(DraftHistoryRepository draftHistoryRepository,
                                 PlayerStatsRepository playerStatsRepository,
                                 UserRepository userRepository,
                                 HeroRepository heroRepository) {
        this.draftHistoryRepository = draftHistoryRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.userRepository = userRepository;
        this.heroRepository = heroRepository;
    }

    @Transactional
    public DraftHistory recordFinishedDraft(BanPickRoom room, List<BanPickAction> actions) {
        if (room.getBlueUser() == null || room.getRedUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot save draft history without assigned sides");
        }

        List<String> bluePicks = heroNames(room, actions, BanPickTeamSide.BLUE, BanPickActionType.PICK);
        List<String> redPicks = heroNames(room, actions, BanPickTeamSide.RED, BanPickActionType.PICK);
        List<String> blueBans = heroNames(room, actions, BanPickTeamSide.BLUE, BanPickActionType.BAN);
        List<String> redBans = heroNames(room, actions, BanPickTeamSide.RED, BanPickActionType.BAN);

        DraftHistory history = new DraftHistory();
        history.setRoomCode(room.getRoomCode());
        history.setBlueUser(room.getBlueUser());
        history.setRedUser(room.getRedUser());
        history.setBluePicks(serializeList(bluePicks));
        history.setRedPicks(serializeList(redPicks));
        history.setBlueBans(serializeList(blueBans));
        history.setRedBans(serializeList(redBans));

        DraftHistory savedHistory = draftHistoryRepository.save(history);
        recordDraftStats(room.getBlueUser(), bluePicks);
        recordDraftStats(room.getRedUser(), redPicks);
        return savedHistory;
    }

    @Transactional(readOnly = true)
    public List<DraftHistoryResponse> getCurrentUserHistory(GoogleUserPrincipal principal) {
        User user = findUser(principal);
        return draftHistoryRepository.findByBlueUserOrRedUserOrderByCreatedAtDesc(user, user).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DraftHistoryResponse getHistory(Long id) {
        return toHistoryResponse(findHistory(id));
    }

    @Transactional
    public DraftHistoryResponse recordWinner(Long historyId,
                                             RecordDraftWinnerRequest request,
                                             GoogleUserPrincipal principal) {
        if (request == null || request.winnerSide() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "winnerSide is required");
        }
        User actor = findUser(principal);
        DraftHistory history = findHistory(historyId);
        if (!isSameUser(actor, history.getBlueUser()) && !isSameUser(actor, history.getRedUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only draft participants can record result");
        }
        if (history.getWinnerUser() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Draft result has already been recorded");
        }

        User winner = request.winnerSide() == BanPickTeamSide.BLUE ? history.getBlueUser() : history.getRedUser();
        User loser = request.winnerSide() == BanPickTeamSide.BLUE ? history.getRedUser() : history.getBlueUser();
        history.setWinnerUser(winner);
        history.setResultRecordedAt(LocalDateTime.now());

        PlayerStats winnerStats = getOrCreateStats(winner);
        PlayerStats loserStats = getOrCreateStats(loser);
        winnerStats.setWins(safeInt(winnerStats.getWins()) + 1);
        winnerStats.setRating(safeRating(winnerStats.getRating()) + RATING_DELTA);
        loserStats.setLosses(safeInt(loserStats.getLosses()) + 1);
        loserStats.setRating(Math.max(0, safeRating(loserStats.getRating()) - RATING_DELTA));
        playerStatsRepository.save(winnerStats);
        playerStatsRepository.save(loserStats);

        return toHistoryResponse(draftHistoryRepository.save(history));
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> getLeaderboard() {
        return playerStatsRepository.findAllByOrderByRatingDescTotalMatchesDesc(PageRequest.of(0, LEADERBOARD_LIMIT))
                .stream()
                .map(this::toStatsResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BanPickProfileResponse getProfile(GoogleUserPrincipal principal) {
        User user = findUser(principal);
        PlayerStats stats = playerStatsRepository.findByUser(user).orElseGet(() -> defaultStats(user));
        List<DraftHistoryResponse> history = draftHistoryRepository
                .findByBlueUserOrRedUserOrderByCreatedAtDesc(user, user)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
        return new BanPickProfileResponse(toUserSummary(user), toStatsResponse(stats), history);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findLatestHistoryIdByRoomCode(String roomCode) {
        return draftHistoryRepository.findFirstByRoomCodeOrderByCreatedAtDesc(roomCode)
                .map(DraftHistory::getId);
    }

    private void recordDraftStats(User user, List<String> picks) {
        PlayerStats stats = getOrCreateStats(user);
        stats.setTotalMatches(safeInt(stats.getTotalMatches()) + 1);
        Map<String, Integer> pickCounts = parsePickCounts(stats.getPickedHeroCounts());
        for (String pick : picks) {
            if (pick == null || pick.isBlank()) continue;
            pickCounts.merge(pick, 1, Integer::sum);
        }
        stats.setPickedHeroCounts(serializePickCounts(pickCounts));
        playerStatsRepository.save(stats);
    }

    private PlayerStats getOrCreateStats(User user) {
        return playerStatsRepository.findByUser(user).orElseGet(() -> {
            PlayerStats stats = new PlayerStats();
            stats.setUser(user);
            stats.setTotalMatches(0);
            stats.setWins(0);
            stats.setLosses(0);
            stats.setRating(STARTING_RATING);
            return playerStatsRepository.save(stats);
        });
    }

    private PlayerStats defaultStats(User user) {
        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        stats.setTotalMatches(0);
        stats.setWins(0);
        stats.setLosses(0);
        stats.setRating(STARTING_RATING);
        return stats;
    }

    private DraftHistory findHistory(Long id) {
        return draftHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft history not found"));
    }

    private User findUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap"));
    }

    private List<String> heroNames(BanPickRoom room,
                                   List<BanPickAction> actions,
                                   BanPickTeamSide teamSide,
                                   BanPickActionType actionType) {
        if (actionType == BanPickActionType.PICK) {
            List<Long> orderedHeroIds = parseHeroIdList(teamSide == BanPickTeamSide.BLUE
                    ? room.getBluePickOrder()
                    : room.getRedPickOrder());
            if (!orderedHeroIds.isEmpty()) {
                return orderedHeroIds.stream()
                        .map(heroId -> heroRepository.findById(heroId)
                                .map(Hero::getName)
                                .orElse("Hero #" + heroId))
                        .toList();
            }
        }
        return actions.stream()
                .filter(action -> action.getTeamSide() == teamSide && action.getActionType() == actionType)
                .sorted(Comparator.comparing(BanPickAction::getConfirmedAt))
                .map(action -> heroRepository.findById(action.getHeroId())
                        .map(Hero::getName)
                        .orElse("Hero #" + action.getHeroId()))
                .toList();
    }

    private DraftHistoryResponse toHistoryResponse(DraftHistory history) {
        return new DraftHistoryResponse(
                history.getId(),
                history.getRoomCode(),
                toUserSummary(history.getBlueUser()),
                toUserSummary(history.getRedUser()),
                toUserSummary(history.getWinnerUser()),
                winnerSide(history),
                deserializeList(history.getBluePicks()),
                deserializeList(history.getRedPicks()),
                deserializeList(history.getBlueBans()),
                deserializeList(history.getRedBans()),
                history.getCreatedAt(),
                history.getResultRecordedAt(),
                "/ban-pick/result/" + history.getId()
        );
    }

    private PlayerStatsResponse toStatsResponse(PlayerStats stats) {
        int totalMatches = safeInt(stats.getTotalMatches());
        int wins = safeInt(stats.getWins());
        int losses = safeInt(stats.getLosses());
        double winRate = totalMatches == 0 ? 0.0 : Math.round((wins * 10000.0 / totalMatches)) / 100.0;
        return new PlayerStatsResponse(
                toUserSummary(stats.getUser()),
                totalMatches,
                wins,
                losses,
                winRate,
                safeRating(stats.getRating()),
                mostPickedHeroes(stats.getPickedHeroCounts())
        );
    }

    private BanPickTeamSide winnerSide(DraftHistory history) {
        if (history.getWinnerUser() == null) return null;
        if (isSameUser(history.getWinnerUser(), history.getBlueUser())) return BanPickTeamSide.BLUE;
        if (isSameUser(history.getWinnerUser(), history.getRedUser())) return BanPickTeamSide.RED;
        return null;
    }

    private BanPickUserSummary toUserSummary(User user) {
        if (user == null) return null;
        return new BanPickUserSummary(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getAvatarUrl()
        );
    }

    private List<HeroPickStatResponse> mostPickedHeroes(String value) {
        return parsePickCounts(value).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new HeroPickStatResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Integer> parsePickCounts(String value) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : deserializeList(value)) {
            int separator = line.lastIndexOf('\t');
            if (separator <= 0 || separator >= line.length() - 1) continue;
            String heroName = line.substring(0, separator);
            int count;
            try {
                count = Integer.parseInt(line.substring(separator + 1));
            } catch (NumberFormatException ex) {
                continue;
            }
            counts.put(heroName, count);
        }
        return counts;
    }

    private String serializePickCounts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "\t" + entry.getValue())
                .reduce((first, second) -> first + "\n" + second)
                .orElse("");
    }

    private List<Long> parseHeroIdList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> heroIds = new ArrayList<>();
        for (String token : value.split(",")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                heroIds.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // Keep valid ids and ignore malformed entries.
            }
        }
        return heroIds;
    }

    private String serializeList(List<String> values) {
        return String.join("\n", values);
    }

    private List<String> deserializeList(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> values = new ArrayList<>();
        for (String item : value.split("\\R")) {
            if (!item.isBlank()) values.add(item.trim());
        }
        return values;
    }

    private boolean isSameUser(User first, User second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int safeRating(Integer value) {
        return value != null ? value : STARTING_RATING;
    }
}
