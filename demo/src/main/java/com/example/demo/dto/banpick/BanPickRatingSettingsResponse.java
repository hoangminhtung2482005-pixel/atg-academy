package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickSeasonResetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BanPickRatingSettingsResponse(
        BaseSettings base,
        MacroSettings macro,
        GapSettings gap,
        AntiTradingSettings antiTrading,
        DodgeSettings dodge,
        SeasonalResetSettings seasonalReset,
        Diagnostics diagnostics
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
            Integer allowedRecentMatches,
            Integer blockedWinDelta,
            Integer blockedLossDelta
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
            Boolean hardPriorityOverSoft,
            String confirmationText
    ) {
    }

    public record Diagnostics(
            Integer currentMacroWinDelta,
            Integer currentActivePlayerCount,
            Integer currentActivePoolSize,
            BigDecimal activePoolAverageRating,
            LastResetLog lastResetLog,
            NextScheduledReset nextScheduledReset,
            LocalDateTime updatedAt,
            String updatedBy,
            boolean replayAnchorAdvanced
    ) {
    }

    public record LastResetLog(
            Long id,
            BanPickSeasonResetType resetType,
            LocalDate scheduledDate,
            LocalDateTime executedAt,
            Integer affectedPlayers,
            Integer baseRating,
            String formula,
            String executedBy,
            String note
    ) {
    }

    public record NextScheduledReset(
            LocalDate scheduledDate,
            BanPickSeasonResetType resetType
    ) {
    }
}
