package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record BanPickRatingSettingsSnapshot(
        int initialRating,
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
        boolean seasonSchedulerEnabled,
        List<Integer> seasonSoftResetMonths,
        List<Integer> seasonHardResetMonths,
        boolean seasonHardPriorityOverSoft,
        String resetConfirmationText,
        LocalDateTime updatedAt,
        String updatedBy
) {

    public static BanPickRatingSettingsSnapshot defaults() {
        return new BanPickRatingSettingsSnapshot(
                BanPickRatingDefaults.INITIAL_RATING,
                BanPickRatingDefaults.BASE_WIN_DELTA,
                BanPickRatingDefaults.BASE_LOSS_DELTA,
                BanPickRatingDefaults.MIN_RATING,
                BanPickRatingDefaults.MACRO_ENABLED,
                BanPickRatingDefaults.MACRO_ACTIVE_WINDOW_DAYS,
                BanPickRatingDefaults.MACRO_BALANCE_RATING,
                BanPickRatingDefaults.MACRO_ACTIVE_TOP_PERCENT,
                BanPickRatingDefaults.MACRO_RATING_STEP,
                BanPickRatingDefaults.MACRO_WIN_ADJUSTMENT_PER_STEP,
                BanPickRatingDefaults.MACRO_MIN_WIN_DELTA,
                BanPickRatingDefaults.MACRO_MINIMUM_ACTIVE_PLAYERS,
                BanPickRatingDefaults.GAP_ENABLED,
                BanPickRatingDefaults.GAP_RATING_DIFF_STEP,
                BanPickRatingDefaults.GAP_MODIFIER_PER_STEP,
                BanPickRatingDefaults.GAP_MAX_MODIFIER,
                BanPickRatingDefaults.ANTI_TRADING_ENABLED,
                BanPickRatingDefaults.ANTI_TRADING_WINDOW_HOURS,
                BanPickRatingDefaults.ANTI_TRADING_ALLOWED_RECENT_MATCHES,
                BanPickRatingDefaults.DODGE_ENABLED,
                BanPickRatingDefaults.DODGE_DISCONNECT_GRACE_SECONDS,
                BanPickRatingDefaults.DODGE_COOLDOWN_MINUTES,
                BanPickRatingDefaults.DODGE_APPLY_IN_DRAFT_ONLY,
                BanPickRatingDefaults.DODGE_REJECT_RESET_DURING_DRAFT,
                BanPickRatingDefaults.SEASON_SCHEDULER_ENABLED,
                BanPickRatingDefaults.SEASON_SOFT_RESET_MONTHS,
                BanPickRatingDefaults.SEASON_HARD_RESET_MONTHS,
                BanPickRatingDefaults.SEASON_HARD_PRIORITY_OVER_SOFT,
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                null,
                "SYSTEM_DEFAULT"
        );
    }

    public boolean changesReplaySensitiveRulesComparedTo(BanPickRatingSettingsSnapshot previous) {
        if (previous == null) {
            return false;
        }
        return initialRating != previous.initialRating
                || baseWinDelta != previous.baseWinDelta
                || baseLossDelta != previous.baseLossDelta
                || minRating != previous.minRating
                || macroEnabled != previous.macroEnabled
                || macroActiveWindowDays != previous.macroActiveWindowDays
                || macroBalanceRating != previous.macroBalanceRating
                || macroActiveTopPercent != previous.macroActiveTopPercent
                || macroRatingStep != previous.macroRatingStep
                || Double.compare(macroWinAdjustmentPerStep, previous.macroWinAdjustmentPerStep) != 0
                || macroMinWinDelta != previous.macroMinWinDelta
                || macroMinimumActivePlayers != previous.macroMinimumActivePlayers
                || gapEnabled != previous.gapEnabled
                || gapRatingDiffStep != previous.gapRatingDiffStep
                || Double.compare(gapModifierPerStep, previous.gapModifierPerStep) != 0
                || Double.compare(gapMaxModifier, previous.gapMaxModifier) != 0
                || antiTradingEnabled != previous.antiTradingEnabled
                || antiTradingWindowHours != previous.antiTradingWindowHours
                || antiTradingAllowedRecentMatches != previous.antiTradingAllowedRecentMatches;
    }

    public int seasonBaseRating() {
        return initialRating;
    }

    public BanPickRatingSettingsSnapshot withSeasonSchedulerEnabled(boolean schedulerEnabled) {
        return new BanPickRatingSettingsSnapshot(
                initialRating,
                baseWinDelta,
                baseLossDelta,
                minRating,
                macroEnabled,
                macroActiveWindowDays,
                macroBalanceRating,
                macroActiveTopPercent,
                macroRatingStep,
                macroWinAdjustmentPerStep,
                macroMinWinDelta,
                macroMinimumActivePlayers,
                gapEnabled,
                gapRatingDiffStep,
                gapModifierPerStep,
                gapMaxModifier,
                antiTradingEnabled,
                antiTradingWindowHours,
                antiTradingAllowedRecentMatches,
                dodgeEnabled,
                dodgeDisconnectGraceSeconds,
                dodgeCooldownMinutes,
                dodgeApplyInDraftOnly,
                dodgeRejectResetDuringDraft,
                schedulerEnabled,
                seasonSoftResetMonths,
                seasonHardResetMonths,
                seasonHardPriorityOverSoft,
                resetConfirmationText,
                updatedAt,
                updatedBy
        );
    }

    public BanPickRatingSettingsSnapshot withSeasonMonths(List<Integer> softMonths,
                                                         List<Integer> hardMonths,
                                                         boolean hardPriorityOverSoft) {
        return new BanPickRatingSettingsSnapshot(
                initialRating,
                baseWinDelta,
                baseLossDelta,
                minRating,
                macroEnabled,
                macroActiveWindowDays,
                macroBalanceRating,
                macroActiveTopPercent,
                macroRatingStep,
                macroWinAdjustmentPerStep,
                macroMinWinDelta,
                macroMinimumActivePlayers,
                gapEnabled,
                gapRatingDiffStep,
                gapModifierPerStep,
                gapMaxModifier,
                antiTradingEnabled,
                antiTradingWindowHours,
                antiTradingAllowedRecentMatches,
                dodgeEnabled,
                dodgeDisconnectGraceSeconds,
                dodgeCooldownMinutes,
                dodgeApplyInDraftOnly,
                dodgeRejectResetDuringDraft,
                seasonSchedulerEnabled,
                List.copyOf(Objects.requireNonNullElse(softMonths, List.of())),
                List.copyOf(Objects.requireNonNullElse(hardMonths, List.of())),
                hardPriorityOverSoft,
                resetConfirmationText,
                updatedAt,
                updatedBy
        );
    }
}
