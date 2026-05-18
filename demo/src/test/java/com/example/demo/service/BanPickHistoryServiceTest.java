package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickProfileResponse;
import com.example.demo.dto.banpick.DraftHistoryResponse;
import com.example.demo.dto.banpick.PlayerStatsResponse;
import com.example.demo.dto.banpick.RecordDraftWinnerRequest;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.DraftHistoryEndReason;
import com.example.demo.entity.Hero;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.PlayerStatsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BanPickHistoryServiceTest {

    @Mock
    private DraftHistoryRepository draftHistoryRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HeroRepository heroRepository;

    private final Map<Long, Hero> heroesById = new LinkedHashMap<>();
    private final Map<Long, PlayerStats> statsByUserId = new LinkedHashMap<>();
    private final Map<Long, DraftHistory> historiesById = new LinkedHashMap<>();
    private final AtomicLong historyIds = new AtomicLong(1);
    private final AtomicLong statsIds = new AtomicLong(1);

    private BanPickHistoryService service;

    @BeforeEach
    void setUp() {
        service = new BanPickHistoryService(
                draftHistoryRepository,
                playerStatsRepository,
                userRepository,
                heroRepository,
                new BanPickRankService(),
                new BanPickMacroEconomyService(
                        draftHistoryRepository,
                        playerStatsRepository,
                        Clock.fixed(Instant.parse("2026-05-16T05:00:00Z"), ZoneId.of("Asia/Saigon"))
                )
        );

        when(draftHistoryRepository.findFirstByRoomCodeOrderByCreatedAtDesc(anyString())).thenAnswer(invocation -> {
            String roomCode = invocation.getArgument(0);
            return historiesById.values().stream()
                    .filter(history -> roomCode.equals(history.getRoomCode()))
                    .max(Comparator.comparing(DraftHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(DraftHistory::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        });
        when(draftHistoryRepository.findById(any(Long.class))).thenAnswer(invocation ->
                Optional.ofNullable(historiesById.get(invocation.getArgument(0))));
        when(draftHistoryRepository.save(any(DraftHistory.class))).thenAnswer(invocation -> {
            DraftHistory history = invocation.getArgument(0);
            if (history.getId() == null) {
                history.setId(historyIds.getAndIncrement());
            }
            if (history.getCreatedAt() == null) {
                history.setCreatedAt(LocalDateTime.now());
            }
            persistHistory(history);
            return history;
        });
        when(draftHistoryRepository.findByParticipantOrderByRecentDesc(any(User.class))).thenAnswer(invocation ->
                recentHistoriesFor(invocation.getArgument(0), Integer.MAX_VALUE));
        when(draftHistoryRepository.findRecentByParticipantOrderByRecentDesc(any(User.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    Pageable pageable = invocation.getArgument(1);
                    return recentHistoriesFor(user, pageable.getPageSize());
                });
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    LocalDateTime windowStart = invocation.getArgument(0);
                    LocalDateTime windowEnd = invocation.getArgument(1);
                    return historiesById.values().stream()
                            .filter(this::isRankedHistory)
                            .filter(history -> {
                                LocalDateTime recordedAt = sortTime(history);
                                return recordedAt != null
                                        && !recordedAt.isBefore(windowStart)
                                        && recordedAt.isBefore(windowEnd);
                            })
                            .sorted(Comparator
                                    .comparing(this::sortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(DraftHistory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(DraftHistory::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                });
        when(draftHistoryRepository.countCompletedPairMatchesWithinWindow(any(Long.class), any(Long.class),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    Long lowerUserId = invocation.getArgument(0);
                    Long higherUserId = invocation.getArgument(1);
                    LocalDateTime windowStart = invocation.getArgument(2);
                    LocalDateTime windowEnd = invocation.getArgument(3);
                    return historiesById.values().stream()
                            .filter(this::isRankedHistory)
                            .filter(history -> matchesPair(history, lowerUserId, higherUserId))
                            .filter(history -> {
                                LocalDateTime recordedAt = sortTime(history);
                                return recordedAt != null
                                        && !recordedAt.isBefore(windowStart)
                                        && recordedAt.isBefore(windowEnd);
                            })
                            .count();
                });
        doAnswer(invocation -> {
            for (Long historyId : invocation.<Iterable<Long>>getArgument(0)) {
                historiesById.remove(historyId);
            }
            return null;
        }).when(draftHistoryRepository).deleteAllByIdInBatch(anyCollection());

        when(playerStatsRepository.findByUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return Optional.ofNullable(statsByUserId.get(user.getId()));
        });
        when(playerStatsRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(statsByUserId.values()));
        when(playerStatsRepository.save(any(PlayerStats.class))).thenAnswer(invocation -> {
            PlayerStats stats = invocation.getArgument(0);
            if (stats.getId() == null) {
                stats.setId(statsIds.getAndIncrement());
            }
            statsByUserId.put(stats.getUser().getId(), stats);
            return stats;
        });
        doAnswer(invocation -> {
            PlayerStats stats = invocation.getArgument(0);
            if (stats != null && stats.getUser() != null) {
                statsByUserId.remove(stats.getUser().getId());
            }
            return null;
        }).when(playerStatsRepository).delete(any(PlayerStats.class));

        when(heroRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<Hero> heroes = new ArrayList<>();
            for (Long id : ids) {
                Hero hero = heroesById.get(id);
                if (hero != null) {
                    heroes.add(hero);
                }
            }
            return heroes;
        });
    }

    @Test
    void recordFinishedDraftAutoAssignsBlueWinnerAndUpdatesStats() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM1", blueUser, redUser, "3,1,2,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00"),
                hero(11L, "Rouie", "0.00"),
                hero(12L, "Tulen", "0.00")
        );

        DraftHistory history = service.recordFinishedDraft(room, List.of(
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.BAN, 12L, 1),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        ));

        assertThat(history.getWinnerUser()).isEqualTo(blueUser);
        assertThat(history.getResultRecordedAt()).isNotNull();
        assertThat(history.getWinRatingDelta()).isEqualTo(30);
        assertThat(history.getLossRatingDelta()).isEqualTo(-20);
        assertThat(history.getBluePicks()).isEqualTo("Alice\nAoi\nZata\nRyoma\nThane");

        PlayerStats blueStats = statsByUserId.get(blueUser.getId());
        PlayerStats redStats = statsByUserId.get(redUser.getId());
        assertThat(blueStats.getTotalMatches()).isEqualTo(1);
        assertThat(blueStats.getWins()).isEqualTo(1);
        assertThat(blueStats.getLosses()).isEqualTo(0);
        assertThat(blueStats.getRating()).isEqualTo(1030);
        assertThat(redStats.getTotalMatches()).isEqualTo(1);
        assertThat(redStats.getWins()).isEqualTo(0);
        assertThat(redStats.getLosses()).isEqualTo(1);
        assertThat(redStats.getRating()).isEqualTo(980);
    }

    @Test
    void recordFinishedDraftSimulationModeSkipsRankStatsAndStoresZeroDeltaSnapshot() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM-SIM", blueUser, redUser, "3,1,2,4,5", "6,7,8,9,10");
        room.setMode(BanPickMatchMode.SIMULATION);
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00")
        );

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getMode()).isEqualTo(BanPickMatchMode.SIMULATION);
        assertThat(history.getWinnerUser()).isEqualTo(blueUser);
        assertThat(history.getWinRatingDelta()).isZero();
        assertThat(history.getLossRatingDelta()).isZero();
        assertThat(statsByUserId).isEmpty();
        verify(playerStatsRepository, never()).save(any(PlayerStats.class));
    }

    @Test
    void getProfileBuildsStatsFromRankedOnlyButKeepsSimulationHistoryVisible() {
        User user = user(1L, "mixed-mode@example.com");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        DraftHistory rankedWin = storedHistory(
                1L,
                "RANKED-WIN",
                user,
                opponent,
                user,
                LocalDateTime.of(2026, 5, 10, 12, 0),
                "Aoi",
                "Enemy"
        );
        persistHistory(rankedWin);

        DraftHistory simulationLoss = storedHistory(
                2L,
                "SIM-LOSS",
                user,
                opponent,
                opponent,
                LocalDateTime.of(2026, 5, 10, 12, 30),
                "Alice",
                "Enemy"
        );
        simulationLoss.setMode(BanPickMatchMode.SIMULATION);
        simulationLoss.setWinRatingDelta(0);
        simulationLoss.setLossRatingDelta(0);
        persistHistory(simulationLoss);

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.history()).hasSize(2);
        assertThat(profile.history()).extracting(DraftHistoryResponse::mode)
                .containsExactly(BanPickMatchMode.SIMULATION, BanPickMatchMode.RANKED);
        assertThat(profile.stats().totalMatches()).isEqualTo(1);
        assertThat(profile.stats().wins()).isEqualTo(1);
        assertThat(profile.stats().losses()).isZero();
        assertThat(profile.stats().rating()).isEqualTo(1030);
        assertThat(profile.playerCard().elo()).isEqualTo(1030);
    }

    @Test
    void recordFinishedDraftAutoAssignsRedWinnerWhenRedScoreHigher() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM2", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "2.00"),
                hero(2L, "Zata", "2.00"),
                hero(3L, "Alice", "2.00"),
                hero(4L, "Ryoma", "2.00"),
                hero(5L, "Thane", "2.00"),
                hero(6L, "Krixi", "6.00"),
                hero(7L, "Grakk", "6.00"),
                hero(8L, "Slimz", "6.00"),
                hero(9L, "Arthur", "6.00"),
                hero(10L, "Mina", "6.00")
        );

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getWinnerUser()).isEqualTo(redUser);
        assertThat(history.getWinRatingDelta()).isEqualTo(30);
        assertThat(history.getLossRatingDelta()).isEqualTo(-20);
        assertThat(statsByUserId.get(redUser.getId()).getWins()).isEqualTo(1);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(1030);
        assertThat(statsByUserId.get(blueUser.getId()).getLosses()).isEqualTo(1);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(980);
    }

    @Test
    void recordFinishedDraftAppliesMacroAdjustedWinDeltaAndStoresSnapshot() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        User activeThree = user(3L, "active-three@example.com");
        User activeFour = user(4L, "active-four@example.com");
        BanPickRoom room = finishedRoom("ROOM-MACRO", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "7.00"),
                hero(2L, "Zata", "7.00"),
                hero(3L, "Alice", "7.00"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "7.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "5.00"),
                hero(8L, "Slimz", "5.00"),
                hero(9L, "Arthur", "5.00"),
                hero(10L, "Mina", "5.00")
        );

        statsByUserId.put(blueUser.getId(), statsRow(blueUser, 8, 4, 4, 1600));
        statsByUserId.put(redUser.getId(), statsRow(redUser, 8, 4, 4, 1600));
        statsByUserId.put(activeThree.getId(), statsRow(activeThree, 8, 4, 4, 1600));
        statsByUserId.put(activeFour.getId(), statsRow(activeFour, 8, 4, 4, 1600));
        persistHistory(storedHistory(1L, "ACTIVE-1", blueUser, activeThree, null, LocalDateTime.of(2026, 5, 10, 12, 0), "Blue Tie", "Enemy"));
        persistHistory(storedHistory(2L, "ACTIVE-2", redUser, activeFour, null, LocalDateTime.of(2026, 5, 11, 12, 0), "Red Tie", "Enemy"));

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getWinnerUser()).isEqualTo(blueUser);
        assertThat(history.getWinRatingDelta()).isEqualTo(24);
        assertThat(history.getLossRatingDelta()).isEqualTo(-20);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1024);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(980);
    }

    @Test
    void recordFinishedDraftKeepsSecondPairMatchRatedWithinFortyEightHours() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM-SECOND-PAIR", blueUser, redUser, "3,1,2,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00"),
                hero(11L, "Rouie", "0.00"),
                hero(12L, "Tulen", "0.00")
        );

        DraftHistory previousMatch = storedHistory(
                1L,
                "PAIR-FIRST",
                blueUser,
                redUser,
                blueUser,
                LocalDateTime.now().minusHours(2),
                "First Blue",
                "First Red"
        );
        previousMatch.setWinRatingDelta(30);
        previousMatch.setLossRatingDelta(-20);
        persistHistory(previousMatch);

        DraftHistory history = service.recordFinishedDraft(room, List.of(
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.BAN, 12L, 1),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        ));

        assertThat(history.getWinnerUser()).isEqualTo(blueUser);
        assertThat(history.getWinRatingDelta()).isEqualTo(27);
        assertThat(history.getLossRatingDelta()).isEqualTo(-18);
        assertThat(statsByUserId.get(blueUser.getId()).getTotalMatches()).isEqualTo(2);
        assertThat(statsByUserId.get(blueUser.getId()).getWins()).isEqualTo(2);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1057);
        assertThat(statsByUserId.get(redUser.getId()).getLosses()).isEqualTo(2);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(962);
    }

    @Test
    void recordFinishedDraftBlocksThirdPairMatchWithinFortyEightHoursButStillUpdatesWinLoss() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM-THIRD-PAIR", blueUser, redUser, "3,1,2,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00"),
                hero(11L, "Rouie", "0.00"),
                hero(12L, "Tulen", "0.00")
        );

        persistHistory(pairWinHistory(1L, "PAIR-FIRST", blueUser, redUser, blueUser, LocalDateTime.now().minusHours(3)));
        persistHistory(pairWinHistory(2L, "PAIR-SECOND", blueUser, redUser, blueUser, LocalDateTime.now().minusHours(1)));

        DraftHistory blockedHistory = service.recordFinishedDraft(room, List.of(
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.BAN, 12L, 1),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        ));

        assertThat(blockedHistory.getWinnerUser()).isEqualTo(blueUser);
        assertThat(blockedHistory.getWinRatingDelta()).isZero();
        assertThat(blockedHistory.getLossRatingDelta()).isZero();
        assertThat(statsByUserId.get(blueUser.getId()).getTotalMatches()).isEqualTo(3);
        assertThat(statsByUserId.get(blueUser.getId()).getWins()).isEqualTo(3);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1060);
        assertThat(statsByUserId.get(redUser.getId()).getLosses()).isEqualTo(3);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(960);

        DraftHistory repeated = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));
        assertThat(repeated).isSameAs(blockedHistory);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1060);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(960);
    }

    @Test
    void recordFinishedDraftTreatsReversedSidesAsSameAntiWinTradingPair() {
        User playerA = user(1L, "a@example.com");
        User playerB = user(2L, "b@example.com");
        BanPickRoom room = finishedRoom("ROOM-SIDE-FLIP", playerA, playerB, "3,1,2,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00"),
                hero(11L, "Rouie", "0.00"),
                hero(12L, "Tulen", "0.00")
        );

        persistHistory(pairWinHistory(1L, "PAIR-FLIP-1", playerA, playerB, playerA, LocalDateTime.now().minusHours(4)));
        persistHistory(pairWinHistory(2L, "PAIR-FLIP-2", playerB, playerA, playerA, LocalDateTime.now().minusHours(2)));

        DraftHistory blockedHistory = service.recordFinishedDraft(room, List.of(
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.BAN, 12L, 1),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        ));

        assertThat(blockedHistory.getWinRatingDelta()).isZero();
        assertThat(blockedHistory.getLossRatingDelta()).isZero();
        assertThat(statsByUserId.get(playerA.getId()).getRating()).isEqualTo(1060);
        assertThat(statsByUserId.get(playerB.getId()).getRating()).isEqualTo(960);
    }

    @Test
    void recordFinishedDraftResetsPairStreakAfterFortyEightHours() {
        User playerA = user(1L, "a@example.com");
        User playerB = user(2L, "b@example.com");
        BanPickRoom room = finishedRoom("ROOM-RESET-48H", playerA, playerB, "3,1,2,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "8.50"),
                hero(2L, "Zata", "8.00"),
                hero(3L, "Alice", "7.50"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "6.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "4.00"),
                hero(8L, "Slimz", "4.00"),
                hero(9L, "Arthur", "3.00"),
                hero(10L, "Mina", "2.00"),
                hero(11L, "Rouie", "0.00"),
                hero(12L, "Tulen", "0.00")
        );

        persistHistory(pairWinHistory(1L, "PAIR-OLD-1", playerA, playerB, playerA, LocalDateTime.now().minusHours(60)));
        persistHistory(pairWinHistory(2L, "PAIR-OLD-2", playerA, playerB, playerB, LocalDateTime.now().minusHours(55)));

        DraftHistory ratedHistory = service.recordFinishedDraft(room, List.of(
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.BAN, 12L, 1),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, playerA, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, playerB, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        ));

        assertThat(ratedHistory.getWinnerUser()).isEqualTo(playerA);
        assertThat(ratedHistory.getWinRatingDelta()).isEqualTo(30);
        assertThat(ratedHistory.getLossRatingDelta()).isEqualTo(-20);
        assertThat(statsByUserId.get(playerA.getId()).getWins()).isEqualTo(2);
        assertThat(statsByUserId.get(playerA.getId()).getLosses()).isEqualTo(1);
        assertThat(statsByUserId.get(playerA.getId()).getRating()).isEqualTo(1040);
        assertThat(statsByUserId.get(playerB.getId()).getWins()).isEqualTo(1);
        assertThat(statsByUserId.get(playerB.getId()).getLosses()).isEqualTo(2);
        assertThat(statsByUserId.get(playerB.getId()).getRating()).isEqualTo(990);
    }

    @Test
    void recordFinishedDraftAppliesGapModifierWhenUnderdogWins() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        User activeThree = user(3L, "active-three@example.com");
        User activeFour = user(4L, "active-four@example.com");
        User activeFive = user(5L, "active-five@example.com");
        User activeSix = user(6L, "active-six@example.com");
        User legacyOpponent = user(7L, "legacy-opponent@example.com");
        BanPickRoom room = finishedRoom("ROOM-GAP-UPSET", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "7.00"),
                hero(2L, "Zata", "7.00"),
                hero(3L, "Alice", "7.00"),
                hero(4L, "Ryoma", "7.00"),
                hero(5L, "Thane", "7.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "5.00"),
                hero(8L, "Slimz", "5.00"),
                hero(9L, "Arthur", "5.00"),
                hero(10L, "Mina", "5.00")
        );

        seedMacroActivePoolAtRating(1600, activeThree, activeFour, activeFive, activeSix);
        seedHistoricWins(redUser, legacyOpponent, 10, 20, LocalDateTime.of(2026, 3, 1, 12, 0));

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getWinnerUser()).isEqualTo(blueUser);
        assertThat(history.getWinRatingDelta()).isEqualTo(34);
        assertThat(history.getLossRatingDelta()).isEqualTo(-28);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1034);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(1172);
    }

    @Test
    void recordFinishedDraftAppliesGapModifierWhenFavoriteWins() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        User activeThree = user(3L, "active-three@example.com");
        User activeFour = user(4L, "active-four@example.com");
        User activeFive = user(5L, "active-five@example.com");
        User activeSix = user(6L, "active-six@example.com");
        User legacyOpponent = user(7L, "legacy-opponent@example.com");
        BanPickRoom room = finishedRoom("ROOM-GAP-FAVORITE", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "5.00"),
                hero(2L, "Zata", "5.00"),
                hero(3L, "Alice", "5.00"),
                hero(4L, "Ryoma", "5.00"),
                hero(5L, "Thane", "5.00"),
                hero(6L, "Krixi", "7.00"),
                hero(7L, "Grakk", "7.00"),
                hero(8L, "Slimz", "7.00"),
                hero(9L, "Arthur", "7.00"),
                hero(10L, "Mina", "7.00")
        );

        seedMacroActivePoolAtRating(1600, activeThree, activeFour, activeFive, activeSix);
        seedHistoricWins(redUser, legacyOpponent, 10, 20, LocalDateTime.of(2026, 3, 1, 12, 0));

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getWinnerUser()).isEqualTo(redUser);
        assertThat(history.getWinRatingDelta()).isEqualTo(14);
        assertThat(history.getLossRatingDelta()).isEqualTo(-12);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(1214);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(988);
    }

    @Test
    void recordFinishedDraftKeepsTieWithoutWinnerOrRatingChange() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM3", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        registerHeroes(
                hero(1L, "Aoi", "5.00"),
                hero(2L, "Zata", "5.00"),
                hero(3L, "Alice", "5.00"),
                hero(4L, "Ryoma", "5.00"),
                hero(5L, "Thane", "5.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "5.00"),
                hero(8L, "Slimz", "5.00"),
                hero(9L, "Arthur", "5.00"),
                hero(10L, "Mina", "5.00")
        );

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history.getWinnerUser()).isNull();
        assertThat(history.getResultRecordedAt()).isNotNull();
        assertThat(history.getWinRatingDelta()).isZero();
        assertThat(history.getLossRatingDelta()).isZero();
        assertThat(statsByUserId.get(blueUser.getId()).getTotalMatches()).isEqualTo(1);
        assertThat(statsByUserId.get(blueUser.getId()).getWins()).isEqualTo(0);
        assertThat(statsByUserId.get(blueUser.getId()).getLosses()).isEqualTo(0);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1000);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(1000);
    }

    @Test
    void recordFinishedDraftForcedWinnerStoresDodgeMetadataAndUpdatesStats() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM-DODGE-TIMEOUT", blueUser, redUser, null, null);
        registerHeroes(
                hero(6L, "Krixi", "5.00"),
                hero(11L, "Rouie", "0.00")
        );

        DraftHistory history = service.recordFinishedDraft(
                room,
                List.of(
                        action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.BAN, 11L, 0),
                        action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5)
                ),
                redUser,
                DraftHistoryEndReason.DODGE_TIMEOUT,
                blueUser
        );

        assertThat(history.getWinnerUser()).isEqualTo(redUser);
        assertThat(history.getDodgedUser()).isEqualTo(blueUser);
        assertThat(history.getEndReason()).isEqualTo(DraftHistoryEndReason.DODGE_TIMEOUT);
        assertThat(history.getRedPicks()).isEqualTo("Krixi");
        assertThat(history.getWinRatingDelta()).isEqualTo(30);
        assertThat(history.getLossRatingDelta()).isEqualTo(-20);
        assertThat(statsByUserId.get(redUser.getId()).getWins()).isEqualTo(1);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(1030);
        assertThat(statsByUserId.get(blueUser.getId()).getLosses()).isEqualTo(1);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(980);
    }

    @Test
    void recordFinishedDraftForcedWinnerStillBlocksRatingWhenPairIsAlreadyCapped() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM-DODGE-BLOCKED", blueUser, redUser, null, null);
        registerHeroes(hero(6L, "Krixi", "5.00"));

        persistHistory(pairWinHistory(1L, "PAIR-FIRST", blueUser, redUser, blueUser, LocalDateTime.now().minusHours(3)));
        persistHistory(pairWinHistory(2L, "PAIR-SECOND", blueUser, redUser, blueUser, LocalDateTime.now().minusHours(1)));

        DraftHistory history = service.recordFinishedDraft(
                room,
                List.of(action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5)),
                redUser,
                DraftHistoryEndReason.DODGE_DISCONNECT,
                blueUser
        );

        assertThat(history.getWinnerUser()).isEqualTo(redUser);
        assertThat(history.getDodgedUser()).isEqualTo(blueUser);
        assertThat(history.getEndReason()).isEqualTo(DraftHistoryEndReason.DODGE_DISCONNECT);
        assertThat(history.getWinRatingDelta()).isZero();
        assertThat(history.getLossRatingDelta()).isZero();
        assertThat(statsByUserId.get(blueUser.getId()).getWins()).isEqualTo(2);
        assertThat(statsByUserId.get(blueUser.getId()).getLosses()).isEqualTo(1);
        assertThat(statsByUserId.get(blueUser.getId()).getRating()).isEqualTo(1060);
        assertThat(statsByUserId.get(redUser.getId()).getWins()).isEqualTo(1);
        assertThat(statsByUserId.get(redUser.getId()).getLosses()).isEqualTo(2);
        assertThat(statsByUserId.get(redUser.getId()).getRating()).isEqualTo(960);
    }

    @Test
    void recordFinishedDraftSkipsDuplicateFinishForSameRoomSnapshot() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM4", blueUser, redUser, "1,2,3,4,5", "6,7,8,9,10");
        DraftHistory existingHistory = storedHistory(
                99L,
                "ROOM4",
                blueUser,
                redUser,
                null,
                LocalDateTime.of(2026, 5, 15, 10, 0, 1),
                "Blue Pick",
                "Red Pick"
        );
        persistHistory(existingHistory);

        DraftHistory history = service.recordFinishedDraft(room, draftActions(room, blueUser, redUser));

        assertThat(history).isSameAs(existingHistory);
        verify(draftHistoryRepository, never()).save(any(DraftHistory.class));
        verify(playerStatsRepository, never()).save(any(PlayerStats.class));
    }

    @Test
    void recordFinishedDraftForcedWinnerAlsoSkipsDuplicateFinishForSameRoomSnapshot() {
        User blueUser = user(1L, "blue@example.com");
        User redUser = user(2L, "red@example.com");
        BanPickRoom room = finishedRoom("ROOM4-DODGE", blueUser, redUser, null, null);
        DraftHistory existingHistory = storedHistory(
                100L,
                "ROOM4-DODGE",
                blueUser,
                redUser,
                redUser,
                LocalDateTime.of(2026, 5, 15, 10, 0, 1),
                "",
                "Krixi"
        );
        existingHistory.setDodgedUser(blueUser);
        existingHistory.setEndReason(DraftHistoryEndReason.DODGE_TIMEOUT);
        persistHistory(existingHistory);

        DraftHistory history = service.recordFinishedDraft(
                room,
                List.of(action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5)),
                redUser,
                DraftHistoryEndReason.DODGE_TIMEOUT,
                blueUser
        );

        assertThat(history).isSameAs(existingHistory);
        verify(draftHistoryRepository, never()).save(any(DraftHistory.class));
        verify(playerStatsRepository, never()).save(any(PlayerStats.class));
    }

    @Test
    void getProfileDefaultsNewPlayerToInitialRatingWhenStatsRowIsMissing() {
        User user = user(1L, "new-player@example.com");
        user.setDisplayName("New Player");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.history()).isEmpty();
        assertThat(profile.stats().totalMatches()).isEqualTo(0);
        assertThat(profile.stats().wins()).isEqualTo(0);
        assertThat(profile.stats().losses()).isEqualTo(0);
        assertThat(profile.stats().winRate()).isEqualTo(0.0);
        assertThat(profile.stats().rating()).isEqualTo(1000);
        assertThat(profile.stats().rankCode()).isEqualTo("UNRANKED");
        assertThat(profile.stats().rankLabel()).isEqualTo("Unranked");
        assertThat(profile.playerCard().elo()).isEqualTo(1000);
        assertThat(profile.playerCard().rankCode()).isEqualTo("UNRANKED");
        assertThat(profile.playerCard().rankLabel()).isEqualTo("Unranked");
    }

    @Test
    void getProfileUsesOnlyMostRecentFiftyHistoriesForStatsAndHistoryList() {
        User user = user(1L, "player@example.com");
        user.setDisplayName("Solo Tester");
        user.setAvatarUrl("https://cdn.example.com/player.png");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        statsByUserId.put(user.getId(), statsRow(user, 51, 1, 0, 1030));
        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 10, 0);
        persistHistory(storedHistory(1L, "OLD-1", user, opponent, user, baseTime, "Old Hero", "Enemy Hero"));
        for (long id = 2; id <= 51; id += 1) {
            persistHistory(storedHistory(
                    id,
                    "RECENT-" + id,
                    user,
                    opponent,
                    null,
                    baseTime.plusMinutes(id),
                    "Hero " + id,
                    "Enemy " + id
            ));
        }

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.history()).hasSize(50);
        assertThat(profile.history()).extracting(DraftHistoryResponse::roomCode).doesNotContain("OLD-1");
        assertThat(profile.stats().totalMatches()).isEqualTo(50);
        assertThat(profile.stats().wins()).isEqualTo(0);
        assertThat(profile.stats().losses()).isEqualTo(0);
        assertThat(profile.stats().rating()).isEqualTo(1000);
        assertThat(profile.stats().winRate()).isEqualTo(0.0);
        assertThat(profile.playerCard()).isNotNull();
        assertThat(profile.playerCard().avatarUrl()).isEqualTo("https://cdn.example.com/player.png");
        assertThat(profile.playerCard().displayName()).isEqualTo("Solo Tester");
        assertThat(profile.playerCard().elo()).isEqualTo(1000);
        assertThat(profile.playerCard().rankCode()).isEqualTo("D");
        assertThat(profile.playerCard().rankLabel()).isEqualTo("Rank D");
        assertThat(profile.playerCard().badgeCode()).isEqualTo("default");
        assertThat(profile.playerCard().badgeName()).isEqualTo("ATG Player");
        assertThat(profile.playerCard().badgeIconUrl()).isNull();
        assertThat(profile.playerCard().title()).isEqualTo("✦ Tân Binh Ban/Pick ✦");
    }

    @Test
    void getProfileBuildsPlayerCardFromStoredBadgeAndTitleWhileKeepingBackendRank() {
        User user = user(1L, "climber@example.com");
        user.setDisplayName("Rank Climber");
        user.setAvatarUrl("https://cdn.example.com/climber.png");
        user.setPlayerBadgeCode("meta-reader");
        user.setPlayerBadgeName("Meta Reader");
        user.setPlayerBadgeIconUrl("https://cdn.example.com/meta-reader.png");
        user.setPlayerTitle("✦ Đọc Meta Như Mở Sách ✦");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        statsByUserId.put(3L, statsRow(user(3L, "pool-d@example.com"), 6, 1, 5, 1000));
        statsByUserId.put(4L, statsRow(user(4L, "pool-c@example.com"), 6, 2, 4, 1100));
        statsByUserId.put(5L, statsRow(user(5L, "pool-a@example.com"), 6, 4, 2, 1200));
        statsByUserId.put(6L, statsRow(user(6L, "pool-s@example.com"), 6, 5, 1, 1300));

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 12, 0);
        for (long id = 1; id <= 5; id += 1) {
            persistHistory(storedHistory(
                    id,
                    "WIN-" + id,
                    user,
                    opponent,
                    user,
                    baseTime.plusMinutes(id),
                    "Hero " + id,
                    "Enemy " + id
            ));
        }

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.stats().totalMatches()).isEqualTo(5);
        assertThat(profile.stats().wins()).isEqualTo(5);
        assertThat(profile.stats().losses()).isEqualTo(0);
        assertThat(profile.stats().rating()).isEqualTo(1150);
        assertThat(profile.stats().rankCode()).isEqualTo("B");
        assertThat(profile.stats().rankLabel()).isEqualTo("Rank B");
        assertThat(profile.playerCard().displayName()).isEqualTo("Rank Climber");
        assertThat(profile.playerCard().avatarUrl()).isEqualTo("https://cdn.example.com/climber.png");
        assertThat(profile.playerCard().elo()).isEqualTo(1150);
        assertThat(profile.playerCard().rankCode()).isEqualTo("B");
        assertThat(profile.playerCard().rankLabel()).isEqualTo("Rank B");
        assertThat(profile.playerCard().badgeCode()).isEqualTo("meta-reader");
        assertThat(profile.playerCard().badgeName()).isEqualTo("Meta Reader");
        assertThat(profile.playerCard().badgeIconUrl()).isEqualTo("https://cdn.example.com/meta-reader.png");
        assertThat(profile.playerCard().title()).isEqualTo("✦ Đọc Meta Như Mở Sách ✦");
    }

    @Test
    void getProfileCalculatesWinRateFromDecisiveMatchesOnlyWhenRecentWindowContainsTie() {
        User user = user(1L, "winrate@example.com");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 2, 9, 0);
        persistHistory(storedHistory(1L, "DECISIVE-WIN", user, opponent, user, baseTime.plusMinutes(1), "Aoi", "Enemy"));
        persistHistory(storedHistory(2L, "RECENT-TIE", user, opponent, null, baseTime.plusMinutes(2), "Alice", "Enemy"));

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.stats().totalMatches()).isEqualTo(2);
        assertThat(profile.stats().wins()).isEqualTo(1);
        assertThat(profile.stats().losses()).isEqualTo(0);
        assertThat(profile.stats().winRate()).isEqualTo(100.0);
        assertThat(profile.stats().rating()).isEqualTo(1030);
        assertThat(profile.playerCard().elo()).isEqualTo(1030);
    }

    @Test
    void getProfileRebuildsRatingFromLatestFiftyUsingStoredFinalDeltaSnapshots() {
        User user = user(1L, "rolling@example.com");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 3, 8, 0);
        DraftHistory outsideWindowWin = storedHistory(1L, "OUTSIDE-WINDOW-WIN", user, opponent, user, baseTime, "Old Hero", "Enemy");
        outsideWindowWin.setWinRatingDelta(60);
        persistHistory(outsideWindowWin);
        for (long id = 2; id <= 26; id += 1) {
            DraftHistory lossHistory = storedHistory(
                    id,
                    "LOSS-" + id,
                    user,
                    opponent,
                    opponent,
                    baseTime.plusMinutes(id),
                    "Loss Hero " + id,
                    "Enemy " + id
            );
            lossHistory.setLossRatingDelta(-12);
            persistHistory(lossHistory);
        }
        for (long id = 27; id <= 51; id += 1) {
            DraftHistory winHistory = storedHistory(
                    id,
                    "WIN-" + id,
                    user,
                    opponent,
                    user,
                    baseTime.plusMinutes(id),
                    "Win Hero " + id,
                    "Enemy " + id
            );
            winHistory.setWinRatingDelta(34);
            persistHistory(winHistory);
        }

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.history()).hasSize(50);
        assertThat(profile.history()).extracting(DraftHistoryResponse::roomCode).doesNotContain("OUTSIDE-WINDOW-WIN");
        assertThat(profile.stats().totalMatches()).isEqualTo(50);
        assertThat(profile.stats().wins()).isEqualTo(25);
        assertThat(profile.stats().losses()).isEqualTo(25);
        assertThat(profile.stats().winRate()).isEqualTo(50.0);
        assertThat(profile.stats().rating()).isEqualTo(1550);
        assertThat(profile.playerCard().elo()).isEqualTo(1550);
    }

    @Test
    void getProfileRebuildSkipsBlockedPairHistoryWithZeroDeltaSnapshot() {
        User user = user(1L, "blocked-replay@example.com");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 3, 8, 0);
        DraftHistory ratedWin = storedHistory(1L, "RATED-WIN", user, opponent, user, baseTime, "Aoi", "Enemy");
        ratedWin.setWinRatingDelta(30);
        ratedWin.setLossRatingDelta(-20);
        persistHistory(ratedWin);

        DraftHistory blockedWin = storedHistory(2L, "BLOCKED-WIN", user, opponent, user, baseTime.plusMinutes(1), "Alice", "Enemy");
        blockedWin.setWinRatingDelta(0);
        blockedWin.setLossRatingDelta(0);
        persistHistory(blockedWin);

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.stats().totalMatches()).isEqualTo(2);
        assertThat(profile.stats().wins()).isEqualTo(2);
        assertThat(profile.stats().losses()).isEqualTo(0);
        assertThat(profile.stats().rating()).isEqualTo(1030);
        assertThat(profile.playerCard().elo()).isEqualTo(1030);
    }

    @Test
    void getProfileKeepsRatingAtZeroWhenLossesReachFloor() {
        User user = user(1L, "floor@example.com");
        User opponent = user(2L, "opponent@example.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 4, 7, 0);
        for (long id = 1; id <= 50; id += 1) {
            persistHistory(storedHistory(
                    id,
                    "LOSS-" + id,
                    user,
                    opponent,
                    opponent,
                    baseTime.plusMinutes(id),
                    "Hero " + id,
                    "Enemy " + id
            ));
        }

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.stats().totalMatches()).isEqualTo(50);
        assertThat(profile.stats().wins()).isEqualTo(0);
        assertThat(profile.stats().losses()).isEqualTo(50);
        assertThat(profile.stats().winRate()).isEqualTo(0.0);
        assertThat(profile.stats().rating()).isEqualTo(0);
        assertThat(profile.playerCard().elo()).isEqualTo(0);
    }

    @Test
    void recordFinishedDraftDeletesOnlyHistoriesOutsideTopFiftyForBothParticipants() {
        User player = user(1L, "player@example.com");
        User sharedOpponent = user(2L, "shared@example.com");
        User protectedOpponent = user(3L, "protected@example.com");
        BanPickRoom room = finishedRoom("LATEST", player, sharedOpponent, "1,2,3,4,5", "6,7,8,9,10");
        registerBalancedDraftHeroes();

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 0, 0);
        persistHistory(storedHistory(
                1L,
                "UNIQUE-OLDEST",
                player,
                protectedOpponent,
                null,
                baseTime.plusMinutes(1),
                "Protected Hero",
                "Protected Enemy"
        ));
        for (long id = 2; id <= 51; id += 1) {
            persistHistory(storedHistory(
                    id,
                    "SHARED-" + id,
                    player,
                    sharedOpponent,
                    null,
                    baseTime.plusMinutes(id),
                    "Shared Hero",
                    "Shared Enemy"
            ));
        }

        DraftHistory latestHistory = service.recordFinishedDraft(room, draftActions(room, player, sharedOpponent));

        assertThat(latestHistory.getId()).isEqualTo(52L);
        assertThat(historiesById).containsKey(1L);
        assertThat(historiesById).doesNotContainKey(2L);
        assertThat(historiesById).containsKey(52L);
        assertThat(statsByUserId.get(player.getId()).getTotalMatches()).isEqualTo(50);
        assertThat(statsByUserId.get(sharedOpponent.getId()).getTotalMatches()).isEqualTo(50);
    }

    @Test
    void getProfileUsesRatingAnchorAndSkipsReplayBeforeReset() {
        User user = user(1L, "anchored@example.com");
        User opponent = user(2L, "opponent@example.com");
        LocalDateTime anchorAt = LocalDateTime.of(2026, 5, 10, 0, 0);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        PlayerStats anchoredStats = statsRow(user, 4, 2, 2, 1200);
        anchoredStats.setRatingAnchor(1200);
        anchoredStats.setRatingAnchorAt(anchorAt);
        anchoredStats.setLastResetType(com.example.demo.entity.BanPickSeasonResetType.SOFT);
        statsByUserId.put(user.getId(), anchoredStats);

        DraftHistory preResetWin = storedHistory(1L, "PRE-RESET-WIN", user, opponent, user, anchorAt.minusHours(2), "Aoi", "Enemy");
        preResetWin.setWinRatingDelta(30);
        persistHistory(preResetWin);

        DraftHistory preResetLoss = storedHistory(2L, "PRE-RESET-LOSS", user, opponent, opponent, anchorAt.minusHours(1), "Alice", "Enemy");
        preResetLoss.setLossRatingDelta(-20);
        persistHistory(preResetLoss);

        DraftHistory postResetWin = storedHistory(3L, "POST-RESET-WIN", user, opponent, user, anchorAt.plusHours(1), "Ryoma", "Enemy");
        postResetWin.setWinRatingDelta(34);
        persistHistory(postResetWin);

        DraftHistory postResetLoss = storedHistory(4L, "POST-RESET-LOSS", user, opponent, opponent, anchorAt.plusHours(2), "Thane", "Enemy");
        postResetLoss.setLossRatingDelta(-18);
        persistHistory(postResetLoss);

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.history()).hasSize(4);
        assertThat(profile.stats().rating()).isEqualTo(1216);
        assertThat(profile.stats().totalMatches()).isEqualTo(4);
        assertThat(profile.stats().wins()).isEqualTo(2);
        assertThat(profile.stats().losses()).isEqualTo(2);
    }

    @Test
    void getProfileReturnsAnchorRatingImmediatelyAfterResetWhenNoPostResetHistoryExists() {
        User user = user(1L, "reset-now@example.com");
        User opponent = user(2L, "opponent@example.com");
        LocalDateTime anchorAt = LocalDateTime.of(2026, 6, 1, 0, 0);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        PlayerStats anchoredStats = statsRow(user, 2, 1, 1, 1350);
        anchoredStats.setRatingAnchor(1350);
        anchoredStats.setRatingAnchorAt(anchorAt);
        anchoredStats.setLastResetType(com.example.demo.entity.BanPickSeasonResetType.HARD);
        statsByUserId.put(user.getId(), anchoredStats);

        persistHistory(storedHistory(1L, "PRE-RESET-ONLY", user, opponent, user, anchorAt.minusMinutes(5), "Aoi", "Enemy"));

        BanPickProfileResponse profile = service.getProfile(principal(user.getEmail()));

        assertThat(profile.stats().rating()).isEqualTo(1350);
        assertThat(profile.history()).hasSize(1);
    }

    @Test
    void getLeaderboardUsesPercentileRanksFromBackendForEachPlayer() {
        User rankD = user(1L, "rank-d@example.com");
        User rankC = user(2L, "rank-c@example.com");
        User rankB = user(3L, "rank-b@example.com");
        User rankA = user(4L, "rank-a@example.com");
        User rankS = user(5L, "rank-s@example.com");
        statsByUserId.put(rankD.getId(), statsRow(rankD, 10, 2, 8, 1000));
        statsByUserId.put(rankC.getId(), statsRow(rankC, 10, 4, 6, 1100));
        statsByUserId.put(rankB.getId(), statsRow(rankB, 10, 5, 5, 1200));
        statsByUserId.put(rankA.getId(), statsRow(rankA, 10, 6, 4, 1300));
        statsByUserId.put(rankS.getId(), statsRow(rankS, 10, 8, 2, 1400));

        var leaderboard = service.getLeaderboard();

        assertThat(leaderboard).extracting(response -> response.user().id())
                .containsExactly(5L, 4L, 3L, 2L, 1L);
        assertThat(leaderboard).extracting(PlayerStatsResponse::rankCode)
                .containsExactly("S", "A", "B", "C", "D");
        assertThat(leaderboard).extracting(PlayerStatsResponse::rankLabel)
                .containsExactly("Rank S", "Rank A", "Rank B", "Rank C", "Rank D");
    }

    @Test
    void manualWinnerRecordingEndpointIsRejectedInService() {
        assertThatThrownBy(() -> service.recordWinner(
                1L,
                new RecordDraftWinnerRequest(BanPickTeamSide.BLUE),
                principal("judge@example.com")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.GONE);
            assertThat(exception.getReason()).isEqualTo("Winner is calculated automatically from Ban/Pick score.");
        });
    }

    private void registerHeroes(Hero... heroes) {
        for (Hero hero : heroes) {
            heroesById.put(hero.getId(), hero);
        }
    }

    private void registerBalancedDraftHeroes() {
        registerHeroes(
                hero(1L, "Aoi", "5.00"),
                hero(2L, "Zata", "5.00"),
                hero(3L, "Alice", "5.00"),
                hero(4L, "Ryoma", "5.00"),
                hero(5L, "Thane", "5.00"),
                hero(6L, "Krixi", "5.00"),
                hero(7L, "Grakk", "5.00"),
                hero(8L, "Slimz", "5.00"),
                hero(9L, "Arthur", "5.00"),
                hero(10L, "Mina", "5.00")
        );
    }

    private void seedMacroActivePoolAtRating(int rating, User... users) {
        if (users == null || users.length == 0) {
            return;
        }

        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 10, 12, 0);
        for (int index = 0; index < users.length; index += 1) {
            User user = users[index];
            statsByUserId.put(user.getId(), statsRow(user, 8, 4, 4, rating));
            if (index + 1 >= users.length) {
                break;
            }
            User opponent = users[index + 1];
            statsByUserId.put(opponent.getId(), statsRow(opponent, 8, 4, 4, rating));
            persistHistory(storedHistory(
                    historyIds.getAndIncrement(),
                    "ACTIVE-" + index,
                    user,
                    opponent,
                    null,
                    baseTime.plusMinutes(index),
                    "Pool Blue " + index,
                    "Pool Red " + index
            ));
            index += 1;
        }
    }

    private void seedHistoricWins(User winner,
                                  User loser,
                                  int count,
                                  int winDelta,
                                  LocalDateTime baseTime) {
        if (winner == null || loser == null || count <= 0) {
            return;
        }

        for (int index = 0; index < count; index += 1) {
            DraftHistory history = storedHistory(
                    historyIds.getAndIncrement(),
                    "LEGACY-WIN-" + (index + 1),
                    winner,
                    loser,
                    winner,
                    baseTime.plusMinutes(index),
                    "Legacy Blue " + index,
                    "Legacy Red " + index
            );
            history.setWinRatingDelta(winDelta);
            persistHistory(history);
        }
    }

    private void persistHistory(DraftHistory history) {
        historiesById.put(history.getId(), history);
        historyIds.set(Math.max(historyIds.get(), history.getId() + 1));
    }

    private List<DraftHistory> recentHistoriesFor(User user, int limit) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return historiesById.values().stream()
                .filter(history -> isSameUser(history.getBlueUser(), user) || isSameUser(history.getRedUser(), user))
                .sorted(Comparator
                        .comparing(this::sortTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DraftHistory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DraftHistory::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private LocalDateTime sortTime(DraftHistory history) {
        return history.getResultRecordedAt() != null ? history.getResultRecordedAt() : history.getCreatedAt();
    }

    private boolean matchesPair(DraftHistory history, Long lowerUserId, Long higherUserId) {
        if (history == null || lowerUserId == null || higherUserId == null
                || history.getBlueUser() == null || history.getRedUser() == null
                || history.getBlueUser().getId() == null || history.getRedUser().getId() == null) {
            return false;
        }

        long pairLower = Math.min(history.getBlueUser().getId(), history.getRedUser().getId());
        long pairHigher = Math.max(history.getBlueUser().getId(), history.getRedUser().getId());
        return pairLower == lowerUserId && pairHigher == higherUserId;
    }

    private boolean isRankedHistory(DraftHistory history) {
        return history != null && BanPickMatchMode.defaultIfNull(history.getMode()) == BanPickMatchMode.RANKED;
    }

    private boolean isSameUser(User first, User second) {
        return first != null && second != null && first.getId() != null && first.getId().equals(second.getId());
    }

    private static BanPickRoom finishedRoom(String roomCode,
                                            User blueUser,
                                            User redUser,
                                            String bluePickOrder,
                                            String redPickOrder) {
        BanPickRoom room = new BanPickRoom();
        room.setRoomCode(roomCode);
        room.setMode(BanPickMatchMode.RANKED);
        room.setBlueUser(blueUser);
        room.setRedUser(redUser);
        room.setBluePickOrder(bluePickOrder);
        room.setRedPickOrder(redPickOrder);
        room.setUpdatedAt(LocalDateTime.of(2026, 5, 15, 10, 0));
        return room;
    }

    private static List<BanPickAction> draftActions(BanPickRoom room, User blueUser, User redUser) {
        return List.of(
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 1L, 4),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 2L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 3L, 6),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 4L, 13),
                action(room, blueUser, BanPickTeamSide.BLUE, BanPickActionType.PICK, 5L, 13),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 6L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 7L, 5),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 8L, 7),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 9L, 12),
                action(room, redUser, BanPickTeamSide.RED, BanPickActionType.PICK, 10L, 14)
        );
    }

    private static BanPickAction action(BanPickRoom room,
                                        User user,
                                        BanPickTeamSide teamSide,
                                        BanPickActionType actionType,
                                        Long heroId,
                                        int phaseIndex) {
        BanPickAction action = new BanPickAction();
        action.setRoom(room);
        action.setUser(user);
        action.setTeamSide(teamSide);
        action.setActionType(actionType);
        action.setHeroId(heroId);
        action.setPhaseIndex(phaseIndex);
        action.setConfirmedAt(LocalDateTime.now());
        return action;
    }

    private static DraftHistory storedHistory(Long id,
                                              String roomCode,
                                              User blueUser,
                                              User redUser,
                                              User winnerUser,
                                              LocalDateTime recordedAt,
                                              String bluePick,
                                              String redPick) {
        DraftHistory history = new DraftHistory();
        history.setId(id);
        history.setRoomCode(roomCode);
        history.setMode(BanPickMatchMode.RANKED);
        history.setBlueUser(blueUser);
        history.setRedUser(redUser);
        history.setWinnerUser(winnerUser);
        history.setDodgedUser(null);
        history.setEndReason(DraftHistoryEndReason.NORMAL);
        history.setBluePicks(bluePick);
        history.setRedPicks(redPick);
        history.setBlueBans("");
        history.setRedBans("");
        history.setCreatedAt(recordedAt);
        history.setResultRecordedAt(recordedAt);
        history.setWinRatingDelta(30);
        history.setLossRatingDelta(-20);
        return history;
    }

    private static DraftHistory pairWinHistory(Long id,
                                               String roomCode,
                                               User blueUser,
                                               User redUser,
                                               User winnerUser,
                                               LocalDateTime recordedAt) {
        DraftHistory history = storedHistory(
                id,
                roomCode,
                blueUser,
                redUser,
                winnerUser,
                recordedAt,
                "Blue Pair Pick",
                "Red Pair Pick"
        );
        history.setWinRatingDelta(30);
        history.setLossRatingDelta(-20);
        return history;
    }

    private static PlayerStats statsRow(User user,
                                        int totalMatches,
                                        int wins,
                                        int losses,
                                        int rating) {
        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        stats.setTotalMatches(totalMatches);
        stats.setWins(wins);
        stats.setLosses(losses);
        stats.setRating(rating);
        return stats;
    }

    private static Hero hero(Long id, String name, String banPickScore) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setBanPickScore(new BigDecimal(banPickScore));
        return hero;
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(email);
        return user;
    }

    private static GoogleUserPrincipal principal(String email) {
        return new GoogleUserPrincipal(email, email, "", "User");
    }
}
