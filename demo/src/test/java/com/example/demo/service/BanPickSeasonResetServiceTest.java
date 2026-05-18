package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickSeasonResetExecuteResponse;
import com.example.demo.dto.banpick.BanPickSeasonResetPreviewResponse;
import com.example.demo.entity.BanPickRankResetLog;
import com.example.demo.entity.BanPickSeasonResetType;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickRankResetLogRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BanPickSeasonResetServiceTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private BanPickRankResetLogRepository resetLogRepository;

    private final Map<Long, PlayerStats> statsByUserId = new LinkedHashMap<>();
    private final Map<LocalDate, BanPickRankResetLog> logsByDate = new LinkedHashMap<>();
    private final AtomicLong logIds = new AtomicLong(1);
    private final AtomicReference<BanPickRatingSettingsSnapshot> currentSettings =
            new AtomicReference<>(BanPickRatingSettingsSnapshot.defaults().withSeasonSchedulerEnabled(true));

    private BanPickSeasonResetService service;

    @BeforeEach
    void setUp() {
        service = new BanPickSeasonResetService(
                playerStatsRepository,
                resetLogRepository,
                new BanPickSeasonResetSchedule(),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneId.of("Asia/Saigon")),
                currentSettings::get
        );

        when(playerStatsRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(statsByUserId.values()));
        when(playerStatsRepository.saveAll(any())).thenAnswer(invocation -> {
            for (PlayerStats stats : invocation.<Iterable<PlayerStats>>getArgument(0)) {
                statsByUserId.put(stats.getUser().getId(), stats);
            }
            return invocation.getArgument(0);
        });
        when(resetLogRepository.existsByScheduledDate(any(LocalDate.class))).thenAnswer(invocation ->
                logsByDate.containsKey(invocation.getArgument(0)));
        when(resetLogRepository.save(any(BanPickRankResetLog.class))).thenAnswer(invocation -> {
            BanPickRankResetLog log = invocation.getArgument(0);
            if (log.getId() == null) {
                log.setId(logIds.getAndIncrement());
            }
            logsByDate.put(log.getScheduledDate(), log);
            return log;
        });
    }

    @Test
    void previewSoftResetUsesRoundFormulaAndDoesNotMutateRows() {
        seedStats(
                statsRow(1L, "low@example.com", 400),
                statsRow(2L, "mid@example.com", 1400),
                statsRow(3L, "high@example.com", 2000),
                statsRow(4L, "cap@example.com", 4000)
        );

        BanPickSeasonResetPreviewResponse preview = service.previewReset(BanPickSeasonResetType.SOFT);

        assertThat(preview.resetType()).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(preview.affectedPlayerCount()).isEqualTo(4);
        assertThat(preview.before().minRating()).isEqualTo(400);
        assertThat(preview.before().maxRating()).isEqualTo(4000);
        assertThat(preview.before().averageRating()).isEqualByComparingTo(new BigDecimal("1950.00"));
        assertThat(preview.after().minRating()).isEqualTo(700);
        assertThat(preview.after().maxRating()).isEqualTo(2500);
        assertThat(preview.after().averageRating()).isEqualByComparingTo(new BigDecimal("1475.00"));
        assertThat(preview.samples()).extracting(BanPickSeasonResetPreviewResponse.PlayerRatingSample::afterRating)
                .containsExactly(2500, 1500, 1200, 700);
        assertThat(statsByUserId.values()).extracting(PlayerStats::getRating)
                .containsExactly(400, 1400, 2000, 4000);
        verify(playerStatsRepository, never()).saveAll(any());
    }

    @Test
    void previewHardResetShowsAllPlayersReturningToBaseRating() {
        seedStats(
                statsRow(1L, "one@example.com", 900),
                statsRow(2L, "two@example.com", 1600)
        );

        BanPickSeasonResetPreviewResponse preview = service.previewReset(BanPickSeasonResetType.HARD);

        assertThat(preview.after().minRating()).isEqualTo(1000);
        assertThat(preview.after().maxRating()).isEqualTo(1000);
        assertThat(preview.samples()).extracting(BanPickSeasonResetPreviewResponse.PlayerRatingSample::afterRating)
                .containsOnly(1000);
    }

    @Test
    void executeResetRequiresExactConfirmationText() {
        seedStats(statsRow(1L, "user@example.com", 1400));

        assertThatThrownBy(() -> service.executeReset(BanPickSeasonResetType.SOFT, "WRONG", "admin@example.com", "note"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT);

        assertThat(statsByUserId.get(1L).getRating()).isEqualTo(1400);
        assertThat(logsByDate).isEmpty();
    }

    @Test
    void executeSoftResetUpdatesRatingAnchorsAndWritesAuditLog() {
        seedStats(
                statsRow(1L, "low@example.com", 400),
                statsRow(2L, "high@example.com", 2000)
        );

        BanPickSeasonResetExecuteResponse response = service.executeReset(
                BanPickSeasonResetType.SOFT,
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                "admin@example.com",
                "Season soft reset"
        );

        assertThat(response.executed()).isTrue();
        assertThat(response.resetType()).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(response.scheduledDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.executedBy()).isEqualTo("admin@example.com");
        assertThat(response.preview().after().averageRating()).isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(statsByUserId.get(1L).getRating()).isEqualTo(700);
        assertThat(statsByUserId.get(1L).getRatingAnchor()).isEqualTo(700);
        assertThat(statsByUserId.get(1L).getRatingAnchorAt()).isEqualTo(response.executedAt());
        assertThat(statsByUserId.get(1L).getLastResetType()).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(statsByUserId.get(2L).getRating()).isEqualTo(1500);
        assertThat(logsByDate).containsKey(LocalDate.of(2026, 6, 1));
        assertThat(logsByDate.get(LocalDate.of(2026, 6, 1)).getFormula()).isEqualTo("round((currentRating + 1000) / 2)");
    }

    @Test
    void executeHardResetSetsAllRatingsToBase() {
        seedStats(
                statsRow(1L, "one@example.com", 400),
                statsRow(2L, "two@example.com", 4000)
        );

        BanPickSeasonResetExecuteResponse response = service.executeReset(
                BanPickSeasonResetType.HARD,
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                "admin@example.com",
                null
        );

        assertThat(response.preview().after().averageRating()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(statsByUserId.values()).extracting(PlayerStats::getRating).containsOnly(1000);
        assertThat(statsByUserId.values()).extracting(PlayerStats::getLastResetType).containsOnly(BanPickSeasonResetType.HARD);
    }

    @Test
    void executeResetRejectsDuplicateScheduledDate() {
        seedStats(statsRow(1L, "user@example.com", 1200));

        service.executeReset(
                BanPickSeasonResetType.SOFT,
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                "admin@example.com",
                null
        );

        assertThatThrownBy(() -> service.executeReset(
                BanPickSeasonResetType.HARD,
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                "admin@example.com",
                null
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2026-06-01");
    }

    @Test
    void scheduledResetRunsOnlyWhenEnabledAndDue() {
        BanPickSeasonResetService disabledService = new BanPickSeasonResetService(
                playerStatsRepository,
                resetLogRepository,
                new BanPickSeasonResetSchedule(),
                Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneId.of("Asia/Saigon")),
                () -> BanPickRatingSettingsSnapshot.defaults().withSeasonSchedulerEnabled(false)
        );
        seedStats(statsRow(1L, "user@example.com", 1800));

        assertThat(disabledService.runScheduledResetIfDue(LocalDate.of(2026, 6, 1))).isNull();
        assertThat(service.runScheduledResetIfDue(LocalDate.of(2026, 6, 2))).isNull();

        BanPickSeasonResetExecuteResponse scheduled = service.runScheduledResetIfDue(LocalDate.of(2026, 6, 1));

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.resetType()).isEqualTo(BanPickSeasonResetType.HARD);
        assertThat(scheduled.executedBy()).isEqualTo(BanPickSeasonResetService.SYSTEM_EXECUTOR);
        assertThat(statsByUserId.get(1L).getRating()).isEqualTo(1000);
        assertThat(service.runScheduledResetIfDue(LocalDate.of(2026, 6, 1))).isNull();
    }

    private void seedStats(PlayerStats... statsRows) {
        statsByUserId.clear();
        logsByDate.clear();
        logIds.set(1);
        for (PlayerStats stats : statsRows) {
            statsByUserId.put(stats.getUser().getId(), stats);
        }
    }

    private static PlayerStats statsRow(Long userId, String email, int rating) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setName("User " + userId);
        user.setDisplayName("Player " + userId);

        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        stats.setTotalMatches(10);
        stats.setWins(5);
        stats.setLosses(5);
        stats.setRating(rating);
        return stats;
    }
}
