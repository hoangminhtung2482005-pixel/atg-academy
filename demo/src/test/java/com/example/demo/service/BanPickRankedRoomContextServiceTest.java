package com.example.demo.service;

import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroRole;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanPickRankedRoomContextServiceTest {

    @Mock
    private HeroRepository heroRepository;

    private BanPickRankedRoomContextService service;

    @BeforeEach
    void setUp() {
        service = new BanPickRankedRoomContextService(heroRepository, new Random(0));
    }

    @Test
    void gameOneHasNoPreviousLocksAndNoPrep() {
        BanPickRoom room = rankedRoom();

        service.applyContext(room, 1);

        assertThat(room.getVirtualSeriesFormat()).isEqualTo(BanPickSeriesType.BO7);
        assertThat(room.getVirtualGameIndex()).isEqualTo(1);
        assertThat(room.getPrepDurationSeconds()).isZero();
        assertThat(room.getUltimateBattle()).isFalse();
        assertThat(parseHeroIds(room.getBluePreviousUsedHeroIds())).isEmpty();
        assertThat(parseHeroIds(room.getRedPreviousUsedHeroIds())).isEmpty();
    }

    @Test
    void gameTwoCreatesFivePreviousLocksPerSideWithUniqueHeroes() {
        BanPickRoom room = rankedRoom();
        Map<Long, String> roleByHeroId = roleByHeroId(heroPool(12));
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(heroPool(12));

        service.applyContext(room, 2);

        List<Long> blueHeroIds = parseHeroIds(room.getBluePreviousUsedHeroIds());
        List<Long> redHeroIds = parseHeroIds(room.getRedPreviousUsedHeroIds());

        assertThat(blueHeroIds).hasSize(5);
        assertThat(redHeroIds).hasSize(5);
        assertThat(new LinkedHashSet<>(blueHeroIds)).hasSize(5);
        assertThat(new LinkedHashSet<>(redHeroIds)).hasSize(5);
        assertThat(new LinkedHashSet<>(merge(blueHeroIds, redHeroIds))).hasSize(10);
        assertSingleBlockHasAllRoles(blueHeroIds, roleByHeroId);
        assertSingleBlockHasAllRoles(redHeroIds, roleByHeroId);
        assertThat(room.getPrepDurationSeconds()).isEqualTo(30);
    }

    @Test
    void gameSixCreatesTwentyFivePreviousLocksPerSideWithUniqueHeroesAndAllRoleBlocks() {
        BanPickRoom room = rankedRoom();
        Map<Long, String> roleByHeroId = roleByHeroId(heroPool(12));
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(heroPool(12));

        service.applyContext(room, 6);

        List<Long> blueHeroIds = parseHeroIds(room.getBluePreviousUsedHeroIds());
        List<Long> redHeroIds = parseHeroIds(room.getRedPreviousUsedHeroIds());

        assertThat(blueHeroIds).hasSize(25);
        assertThat(redHeroIds).hasSize(25);
        assertThat(new LinkedHashSet<>(merge(blueHeroIds, redHeroIds))).hasSize(50);
        assertEveryFiveHeroBlockHasAllRoles(blueHeroIds, roleByHeroId);
        assertEveryFiveHeroBlockHasAllRoles(redHeroIds, roleByHeroId);
        assertThat(room.getPrepDurationSeconds()).isEqualTo(50);
    }

    @Test
    void gameSevenMarksUltimateBattleAndNoPrep() {
        BanPickRoom room = rankedRoom();
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(heroPool(12));

        service.applyContext(room, 7);

        assertThat(room.getUltimateBattle()).isTrue();
        assertThat(room.getPrepDurationSeconds()).isZero();
        assertThat(parseHeroIds(room.getBluePreviousUsedHeroIds())).hasSize(30);
        assertThat(parseHeroIds(room.getRedPreviousUsedHeroIds())).hasSize(30);
    }

    @Test
    void initializeContextIfMissingDoesNotRerandomAfterFirstGeneration() {
        BanPickRoom room = rankedRoom();
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(heroPool(12));

        service.initializeContextIfMissing(room);
        Integer firstGameIndex = room.getVirtualGameIndex();
        String firstBlueHeroIds = room.getBluePreviousUsedHeroIds();
        String firstRedHeroIds = room.getRedPreviousUsedHeroIds();

        service.initializeContextIfMissing(room);

        assertThat(room.getVirtualGameIndex()).isEqualTo(firstGameIndex);
        assertThat(room.getBluePreviousUsedHeroIds()).isEqualTo(firstBlueHeroIds);
        assertThat(room.getRedPreviousUsedHeroIds()).isEqualTo(firstRedHeroIds);
    }

    @Test
    void simulationRoomDoesNotGenerateRankedContext() {
        BanPickRoom room = new BanPickRoom();
        room.setMode(BanPickMatchMode.SIMULATION);
        room.setSeriesType(BanPickSeriesType.BO3);

        service.prepareRankedRoom(room);
        service.initializeContextIfMissing(room);

        assertThat(room.getSeriesType()).isEqualTo(BanPickSeriesType.BO3);
        assertThat(room.getVirtualSeriesFormat()).isNull();
        assertThat(room.getVirtualGameIndex()).isNull();
        assertThat(room.getBluePreviousUsedHeroIds()).isNull();
        assertThat(room.getRedPreviousUsedHeroIds()).isNull();
    }

    @Test
    void insufficientRolePoolRaisesClearBlocker() {
        BanPickRoom room = rankedRoom();
        when(heroRepository.findAllByPrimaryRoleIsNotNullOrderByNameAsc()).thenReturn(heroPool(3));

        assertThatThrownBy(() -> service.applyContext(room, 4))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("heroes.primary_role_id");
                    assertThat(exception.getReason()).contains("role DSL");
                });
    }

    private static void assertEveryFiveHeroBlockHasAllRoles(List<Long> heroIds, Map<Long, String> roleByHeroId) {
        for (int index = 0; index < heroIds.size(); index += 5) {
            assertSingleBlockHasAllRoles(heroIds.subList(index, index + 5), roleByHeroId);
        }
    }

    private static void assertSingleBlockHasAllRoles(List<Long> heroIds, Map<Long, String> roleByHeroId) {
        assertThat(heroIds).hasSize(5);
        assertThat(heroIds.stream().map(roleByHeroId::get).toList())
                .containsExactlyElementsOf(BanPickRankedRoomContextService.REQUIRED_ROLE_CODES);
    }

    private static List<Long> merge(List<Long> left, List<Long> right) {
        List<Long> merged = new ArrayList<>(left);
        merged.addAll(right);
        return merged;
    }

    private static List<Long> parseHeroIds(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<Long> heroIds = new ArrayList<>();
        for (String token : encoded.split(",")) {
            heroIds.add(Long.parseLong(token.trim()));
        }
        return heroIds;
    }

    private static BanPickRoom rankedRoom() {
        BanPickRoom room = new BanPickRoom();
        room.setMode(BanPickMatchMode.RANKED);
        room.setSeriesType(BanPickSeriesType.BO3);
        return room;
    }

    private static Map<Long, String> roleByHeroId(List<Hero> heroes) {
        Map<Long, String> roleByHeroId = new LinkedHashMap<>();
        for (Hero hero : heroes) {
            roleByHeroId.put(hero.getId(), hero.getPrimaryRole().getCode());
        }
        return roleByHeroId;
    }

    private static List<Hero> heroPool(int heroesPerRole) {
        List<Hero> heroes = new ArrayList<>();
        long heroId = 1L;
        for (String roleCode : BanPickRankedRoomContextService.REQUIRED_ROLE_CODES) {
            for (int index = 1; index <= heroesPerRole; index += 1) {
                Hero hero = new Hero();
                hero.setId(heroId++);
                hero.setName(roleCode + "-" + index);
                hero.setPrimaryRole(role(roleCode));
                heroes.add(hero);
            }
        }
        return heroes;
    }

    private static HeroRole role(String code) {
        HeroRole role = new HeroRole();
        role.setCode(code);
        role.setName(code);
        return role;
    }
}
