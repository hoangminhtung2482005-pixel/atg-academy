package com.example.demo.dto.banpick;

import java.util.List;

public record BanPickRatingSettingsUpdateRequest(
        BaseSettings base,
        MacroSettings macro,
        GapSettings gap,
        AntiTradingSettings antiTrading,
        DodgeSettings dodge,
        SeasonalResetSettings seasonalReset
) {

    public record BaseSettings(
            Integer initialRating,
            Integer baseWinDelta,
            Integer baseLossDelta,
            Integer minRating
    ) {
    }

    public record MacroSettings(
            Boolean enabled,
            Integer activeWindowDays,
            Integer balanceRating,
            Integer activeTopPercent,
            Integer ratingStep,
            Double winAdjustmentPerStep,
            Integer minWinDelta,
            Integer minimumActivePlayers
    ) {
    }

    public record GapSettings(
            Boolean enabled,
            Integer ratingDiffStep,
            Double modifierPerStep,
            Double maxModifier
    ) {
    }

    public record AntiTradingSettings(
            Boolean enabled,
            Integer windowHours,
            Integer allowedRecentMatches
    ) {
    }

    public record DodgeSettings(
            Boolean enabled,
            Integer disconnectGraceSeconds,
            Integer cooldownMinutes,
            Boolean applyInDraftOnly,
            Boolean rejectResetDuringDraft
    ) {
    }

    public record SeasonalResetSettings(
            Boolean schedulerEnabled,
            List<Integer> softResetMonths,
            List<Integer> hardResetMonths,
            Boolean hardPriorityOverSoft
    ) {
    }
}
