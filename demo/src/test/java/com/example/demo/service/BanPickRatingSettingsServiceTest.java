package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickRatingSettingsResponse;
import com.example.demo.dto.banpick.BanPickRatingSettingsUpdateRequest;
import com.example.demo.entity.BanPickRatingSettings;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickRankResetLogRepository;
import com.example.demo.repository.BanPickRatingSettingsRepository;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BanPickRatingSettingsServiceTest {

    @Mock
    private BanPickRatingSettingsRepository settingsRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private DraftHistoryRepository draftHistoryRepository;

    @Mock
    private BanPickRankResetLogRepository resetLogRepository;

    private final Map<Long, PlayerStats> statsByUserId = new LinkedHashMap<>();
    private BanPickRatingSettings storedSettings;
    private BanPickRatingSettingsService service;

    @BeforeEach
    void setUp() {
        service = new BanPickRatingSettingsService(
                settingsRepository,
                playerStatsRepository,
                draftHistoryRepository,
                resetLogRepository,
                new BanPickSeasonResetSchedule(),
                Clock.fixed(Instant.parse("2026-05-17T17:30:00Z"), ZoneId.of("Asia/Saigon"))
        );

        when(settingsRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(storedSettings));
    }

    @Test
    void getSettingsViewFallsBackToDefaultsWhenDbRowMissing() {
        stubDiagnosticsDependencies();

        BanPickRatingSettingsResponse response = service.getSettingsView();

        assertThat(response.base().initialRating()).isEqualTo(1000);
        assertThat(response.base().baseWinDelta()).isEqualTo(30);
        assertThat(response.macro().enabled()).isTrue();
        assertThat(response.dodge().cooldownMinutes()).isEqualTo(5);
        assertThat(response.seasonalReset().schedulerEnabled()).isFalse();
        assertThat(response.seasonalReset().softResetMonths()).containsExactly(2, 4, 8, 10);
        assertThat(response.seasonalReset().hardResetMonths()).containsExactly(6, 12);
        assertThat(response.seasonalReset().confirmationText()).isEqualTo(BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT);
        assertThat(response.diagnostics().nextScheduledReset()).isNotNull();
        assertThat(response.diagnostics().nextScheduledReset().scheduledDate().toString()).isEqualTo("2026-06-01");
        assertThat(response.diagnostics().nextScheduledReset().resetType().name()).isEqualTo("HARD");
        assertThat(response.diagnostics().replayAnchorAdvanced()).isFalse();
    }

    @Test
    void updateSettingsRejectsOutOfRangeValues() {
        BanPickRatingSettingsUpdateRequest invalidRequest = buildRequest(
                1000,
                30,
                -20,
                0,
                true,
                30,
                1500,
                101,
                10,
                0.02,
                20,
                4,
                true,
                10,
                0.02,
                0.5,
                true,
                48,
                2,
                true,
                10,
                5,
                true,
                true,
                false,
                List.of(2, 4, 8, 10),
                List.of(6, 12),
                true
        );

        assertThatThrownBy(() -> service.updateSettings(invalidRequest, "admin@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("macro.activeTopPercent");
    }

    @Test
    void updateSettingsAdvancesReplayBarrierWhenRatingRulesChange() {
        seedStats(
                statsRow(1L, "alpha@example.com", 1200),
                statsRow(2L, "beta@example.com", 800)
        );
        stubSaveSettings();
        stubReplayBarrierDependencies();
        stubDiagnosticsDependencies();

        BanPickRatingSettingsResponse response = service.updateSettings(
                buildRequest(
                        1000,
                        35,
                        -20,
                        0,
                        true,
                        30,
                        1500,
                        50,
                        10,
                        0.02,
                        20,
                        4,
                        true,
                        10,
                        0.02,
                        0.5,
                        true,
                        48,
                        2,
                        true,
                        10,
                        5,
                        true,
                        true,
                        false,
                        List.of(2, 4, 8, 10),
                        List.of(6, 12),
                        true
                ),
                "admin@example.com"
        );

        assertThat(response.base().baseWinDelta()).isEqualTo(35);
        assertThat(response.diagnostics().replayAnchorAdvanced()).isTrue();
        assertThat(statsByUserId.get(1L).getRatingAnchor()).isEqualTo(1200);
        assertThat(statsByUserId.get(2L).getRatingAnchor()).isEqualTo(800);
        assertThat(statsByUserId.get(1L).getRatingAnchorAt()).isEqualTo(response.diagnostics().updatedAt());
        assertThat(storedSettings.getUpdatedBy()).isEqualTo("admin@example.com");
    }

    @Test
    void updateSettingsSkipsReplayBarrierWhenOnlySchedulerFlagChanges() {
        seedStats(statsRow(1L, "user@example.com", 1500));
        stubSaveSettings();
        stubDiagnosticsDependencies();

        BanPickRatingSettingsResponse response = service.updateSettings(
                buildRequest(
                        1000,
                        30,
                        -20,
                        0,
                        true,
                        30,
                        1500,
                        50,
                        10,
                        0.02,
                        20,
                        4,
                        true,
                        10,
                        0.02,
                        0.5,
                        true,
                        48,
                        2,
                        true,
                        10,
                        5,
                        true,
                        true,
                        true,
                        List.of(2, 4, 8, 10),
                        List.of(6, 12),
                        true
                ),
                "admin@example.com"
        );

        assertThat(response.seasonalReset().schedulerEnabled()).isTrue();
        assertThat(response.diagnostics().replayAnchorAdvanced()).isFalse();
        verify(playerStatsRepository, never()).saveAll(any());
    }

    private void stubSaveSettings() {
        when(settingsRepository.save(any(BanPickRatingSettings.class))).thenAnswer(invocation -> {
            storedSettings = invocation.getArgument(0);
            return storedSettings;
        });
    }

    private void stubReplayBarrierDependencies() {
        when(playerStatsRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(statsByUserId.values()));
        when(playerStatsRepository.saveAll(any())).thenAnswer(invocation -> {
            for (PlayerStats stats : invocation.<Iterable<PlayerStats>>getArgument(0)) {
                statsByUserId.put(stats.getUser().getId(), stats);
            }
            return invocation.getArgument(0);
        });
    }

    private void stubDiagnosticsDependencies() {
        when(draftHistoryRepository.findCompletedBetween(any(), any())).thenReturn(List.of());
        when(resetLogRepository.findFirstByOrderByExecutedAtDescIdDesc()).thenReturn(Optional.empty());
    }

    private void seedStats(PlayerStats... statsRows) {
        statsByUserId.clear();
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
        stats.setTotalMatches(12);
        stats.setWins(6);
        stats.setLosses(6);
        stats.setRating(rating);
        return stats;
    }

    private static BanPickRatingSettingsUpdateRequest buildRequest(int initialRating,
                                                                   int baseWinDelta,
                                                                   int baseLossDelta,
                                                                   int minRating,
                                                                   boolean macroEnabled,
                                                                   int macroActiveWindowDays,
                                                                   int macroBalanceRating,
                                                                   int macroActiveTopPercent,
                                                                   int macroRatingStep,
                                                                   double macroWinAdjustmentPerStep,
                                                                   int macroMinWinDelta,
                                                                   int macroMinimumActivePlayers,
                                                                   boolean gapEnabled,
                                                                   int gapRatingDiffStep,
                                                                   double gapModifierPerStep,
                                                                   double gapMaxModifier,
                                                                   boolean antiTradingEnabled,
                                                                   int antiTradingWindowHours,
                                                                   int antiTradingAllowedRecentMatches,
                                                                   boolean dodgeEnabled,
                                                                   int dodgeDisconnectGraceSeconds,
                                                                   int dodgeCooldownMinutes,
                                                                   boolean dodgeApplyInDraftOnly,
                                                                   boolean dodgeRejectResetDuringDraft,
                                                                   boolean schedulerEnabled,
                                                                   List<Integer> softResetMonths,
                                                                   List<Integer> hardResetMonths,
                                                                   boolean hardPriorityOverSoft) {
        return new BanPickRatingSettingsUpdateRequest(
                new BanPickRatingSettingsUpdateRequest.BaseSettings(
                        initialRating,
                        baseWinDelta,
                        baseLossDelta,
                        minRating
                ),
                new BanPickRatingSettingsUpdateRequest.MacroSettings(
                        macroEnabled,
                        macroActiveWindowDays,
                        macroBalanceRating,
                        macroActiveTopPercent,
                        macroRatingStep,
                        macroWinAdjustmentPerStep,
                        macroMinWinDelta,
                        macroMinimumActivePlayers
                ),
                new BanPickRatingSettingsUpdateRequest.GapSettings(
                        gapEnabled,
                        gapRatingDiffStep,
                        gapModifierPerStep,
                        gapMaxModifier
                ),
                new BanPickRatingSettingsUpdateRequest.AntiTradingSettings(
                        antiTradingEnabled,
                        antiTradingWindowHours,
                        antiTradingAllowedRecentMatches
                ),
                new BanPickRatingSettingsUpdateRequest.DodgeSettings(
                        dodgeEnabled,
                        dodgeDisconnectGraceSeconds,
                        dodgeCooldownMinutes,
                        dodgeApplyInDraftOnly,
                        dodgeRejectResetDuringDraft
                ),
                new BanPickRatingSettingsUpdateRequest.SeasonalResetSettings(
                        schedulerEnabled,
                        softResetMonths,
                        hardResetMonths,
                        hardPriorityOverSoft
                )
        );
    }
}
