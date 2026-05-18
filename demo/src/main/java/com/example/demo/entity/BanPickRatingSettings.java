package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ban_pick_rating_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanPickRatingSettings {

    @Id
    private Long id;

    @Column(name = "initial_rating", nullable = false)
    private Integer initialRating;

    @Column(name = "base_win_delta", nullable = false)
    private Integer baseWinDelta;

    @Column(name = "base_loss_delta", nullable = false)
    private Integer baseLossDelta;

    @Column(name = "min_rating", nullable = false)
    private Integer minRating;

    @Column(name = "macro_enabled", nullable = false)
    private Boolean macroEnabled;

    @Column(name = "macro_active_window_days", nullable = false)
    private Integer macroActiveWindowDays;

    @Column(name = "macro_balance_rating", nullable = false)
    private Integer macroBalanceRating;

    @Column(name = "macro_active_top_percent", nullable = false)
    private Integer macroActiveTopPercent;

    @Column(name = "macro_rating_step", nullable = false)
    private Integer macroRatingStep;

    @Column(name = "macro_win_adjustment_per_step", nullable = false, precision = 8, scale = 4)
    private BigDecimal macroWinAdjustmentPerStep;

    @Column(name = "macro_min_win_delta", nullable = false)
    private Integer macroMinWinDelta;

    @Column(name = "macro_minimum_active_players", nullable = false)
    private Integer macroMinimumActivePlayers;

    @Column(name = "gap_enabled", nullable = false)
    private Boolean gapEnabled;

    @Column(name = "gap_rating_diff_step", nullable = false)
    private Integer gapRatingDiffStep;

    @Column(name = "gap_modifier_per_step", nullable = false, precision = 8, scale = 4)
    private BigDecimal gapModifierPerStep;

    @Column(name = "gap_max_modifier", nullable = false, precision = 8, scale = 4)
    private BigDecimal gapMaxModifier;

    @Column(name = "anti_trading_enabled", nullable = false)
    private Boolean antiTradingEnabled;

    @Column(name = "anti_trading_window_hours", nullable = false)
    private Integer antiTradingWindowHours;

    @Column(name = "anti_trading_allowed_recent_matches", nullable = false)
    private Integer antiTradingAllowedRecentMatches;

    @Column(name = "dodge_enabled", nullable = false)
    private Boolean dodgeEnabled;

    @Column(name = "dodge_disconnect_grace_seconds", nullable = false)
    private Integer dodgeDisconnectGraceSeconds;

    @Column(name = "dodge_cooldown_minutes", nullable = false)
    private Integer dodgeCooldownMinutes;

    @Column(name = "dodge_apply_in_draft_only", nullable = false)
    private Boolean dodgeApplyInDraftOnly;

    @Column(name = "dodge_reject_reset_during_draft", nullable = false)
    private Boolean dodgeRejectResetDuringDraft;

    @Column(name = "season_scheduler_enabled", nullable = false)
    private Boolean seasonSchedulerEnabled;

    @Column(name = "season_soft_reset_months", nullable = false, length = 32)
    private String seasonSoftResetMonths;

    @Column(name = "season_hard_reset_months", nullable = false, length = 32)
    private String seasonHardResetMonths;

    @Column(name = "season_hard_priority_over_soft", nullable = false)
    private Boolean seasonHardPriorityOverSoft;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
