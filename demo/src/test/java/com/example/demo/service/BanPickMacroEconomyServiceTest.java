package com.example.demo.service;

import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanPickMacroEconomyServiceTest {

    @Mock
    private DraftHistoryRepository draftHistoryRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    private BanPickMacroEconomyService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-16T05:00:00Z"), ZoneId.of("Asia/Saigon"));
        service = new BanPickMacroEconomyService(draftHistoryRepository, playerStatsRepository, clock);
    }

    @Test
    void currentSnapshotFallsBackToBaseWinDeltaWhenNoActivePlayersExist() {
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.activePlayerCount()).isEqualTo(0);
        assertThat(snapshot.activePoolSize()).isEqualTo(0);
        assertThat(snapshot.averageRating()).isNull();
        assertThat(snapshot.winDelta()).isEqualTo(30);
    }

    @Test
    void currentSnapshotFallsBackToBaseWinDeltaWhenActivePoolIsTooSmall() {
        User blue = user(1L);
        User red = user(2L);
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(completedHistory(1L, blue, red, LocalDateTime.of(2026, 5, 10, 12, 0))));

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.activePlayerCount()).isEqualTo(2);
        assertThat(snapshot.activePoolSize()).isEqualTo(0);
        assertThat(snapshot.averageRating()).isNull();
        assertThat(snapshot.winDelta()).isEqualTo(30);
    }

    @Test
    void currentSnapshotUsesTopActivePoolAverageForBalancedServer() {
        User userOne = user(1L);
        User userTwo = user(2L);
        User userThree = user(3L);
        User userFour = user(4L);
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        completedHistory(1L, userOne, userTwo, LocalDateTime.of(2026, 5, 10, 12, 0)),
                        completedHistory(2L, userOne, userTwo, LocalDateTime.of(2026, 5, 11, 12, 0)),
                        completedHistory(3L, userOne, userThree, LocalDateTime.of(2026, 5, 12, 12, 0)),
                        completedHistory(4L, userTwo, userFour, LocalDateTime.of(2026, 5, 13, 12, 0))
                ));
        when(playerStatsRepository.findAll()).thenReturn(List.of(
                statsRow(userOne, 1600),
                statsRow(userTwo, 1400),
                statsRow(userThree, 2200),
                statsRow(userFour, 800)
        ));

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.activePlayerCount()).isEqualTo(4);
        assertThat(snapshot.activePoolSize()).isEqualTo(2);
        assertThat(snapshot.averageRating()).isEqualTo(1500.0);
        assertThat(snapshot.winDelta()).isEqualTo(30);
    }

    @Test
    void currentSnapshotReducesWinDeltaWhenServerRatingIsInflated() {
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(fourActivePlayersWithEqualActivity());
        when(playerStatsRepository.findAll()).thenReturn(List.of(
                statsRow(user(1L), 1600),
                statsRow(user(2L), 1600),
                statsRow(user(3L), 1600),
                statsRow(user(4L), 1600)
        ));

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.averageRating()).isEqualTo(1600.0);
        assertThat(snapshot.winDelta()).isEqualTo(24);
    }

    @Test
    void currentSnapshotNeverDropsWinDeltaBelowConfiguredFloor() {
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(fourActivePlayersWithEqualActivity());
        when(playerStatsRepository.findAll()).thenReturn(List.of(
                statsRow(user(1L), 2200),
                statsRow(user(2L), 2200),
                statsRow(user(3L), 2200),
                statsRow(user(4L), 2200)
        ));

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.averageRating()).isEqualTo(2200.0);
        assertThat(snapshot.winDelta()).isEqualTo(20);
    }

    @Test
    void currentSnapshotIncreasesWinDeltaWhenServerRatingIsDeflated() {
        when(draftHistoryRepository.findCompletedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(fourActivePlayersWithEqualActivity());
        when(playerStatsRepository.findAll()).thenReturn(List.of(
                statsRow(user(1L), 1400),
                statsRow(user(2L), 1400),
                statsRow(user(3L), 1400),
                statsRow(user(4L), 1400)
        ));

        BanPickMacroEconomyService.MacroEconomySnapshot snapshot = service.getCurrentSnapshot();

        assertThat(snapshot.averageRating()).isEqualTo(1400.0);
        assertThat(snapshot.winDelta()).isEqualTo(36);
    }

    private static List<DraftHistory> fourActivePlayersWithEqualActivity() {
        return List.of(
                completedHistory(1L, user(1L), user(2L), LocalDateTime.of(2026, 5, 10, 12, 0)),
                completedHistory(2L, user(3L), user(4L), LocalDateTime.of(2026, 5, 11, 12, 0))
        );
    }

    private static DraftHistory completedHistory(Long id, User blueUser, User redUser, LocalDateTime recordedAt) {
        DraftHistory history = new DraftHistory();
        history.setId(id);
        history.setRoomCode("ROOM-" + id);
        history.setMode(BanPickMatchMode.RANKED);
        history.setBlueUser(blueUser);
        history.setRedUser(redUser);
        history.setCreatedAt(recordedAt);
        history.setResultRecordedAt(recordedAt);
        history.setWinRatingDelta(30);
        history.setLossRatingDelta(-20);
        return history;
    }

    private static PlayerStats statsRow(User user, int rating) {
        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        stats.setTotalMatches(10);
        stats.setWins(5);
        stats.setLosses(5);
        stats.setRating(rating);
        return stats;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("macro-" + id + "@example.com");
        user.setName("Macro " + id);
        return user;
    }
}
