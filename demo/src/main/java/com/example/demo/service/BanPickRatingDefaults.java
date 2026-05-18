package com.example.demo.service;

import java.util.List;

public final class BanPickRatingDefaults {

    public static final int INITIAL_RATING = 1000;
    public static final int BASE_WIN_DELTA = 30;
    public static final int BASE_LOSS_DELTA = -20;
    public static final int MIN_RATING = 0;

    public static final boolean MACRO_ENABLED = true;
    public static final int MACRO_ACTIVE_WINDOW_DAYS = 30;
    public static final int MACRO_BALANCE_RATING = 1500;
    public static final int MACRO_ACTIVE_TOP_PERCENT = 50;
    public static final int MACRO_RATING_STEP = 10;
    public static final double MACRO_WIN_ADJUSTMENT_PER_STEP = 0.02;
    public static final int MACRO_MIN_WIN_DELTA = 20;
    public static final int MACRO_MINIMUM_ACTIVE_PLAYERS = 4;

    public static final boolean GAP_ENABLED = true;
    public static final int GAP_RATING_DIFF_STEP = 10;
    public static final double GAP_MODIFIER_PER_STEP = 0.02;
    public static final double GAP_MAX_MODIFIER = 0.50;
    public static final int MIN_FINAL_WIN_DELTA = 0;

    public static final boolean ANTI_TRADING_ENABLED = true;
    public static final int ANTI_TRADING_WINDOW_HOURS = 48;
    public static final int ANTI_TRADING_ALLOWED_RECENT_MATCHES = 2;

    public static final boolean DODGE_ENABLED = true;
    public static final int DODGE_DISCONNECT_GRACE_SECONDS = 10;
    public static final int DODGE_COOLDOWN_MINUTES = 5;
    public static final boolean DODGE_APPLY_IN_DRAFT_ONLY = true;
    public static final boolean DODGE_REJECT_RESET_DURING_DRAFT = true;

    public static final boolean SEASON_SCHEDULER_ENABLED = false;
    public static final List<Integer> SEASON_SOFT_RESET_MONTHS = List.of(2, 4, 8, 10);
    public static final List<Integer> SEASON_HARD_RESET_MONTHS = List.of(6, 12);
    public static final boolean SEASON_HARD_PRIORITY_OVER_SOFT = true;
    public static final String SEASON_RESET_CONFIRMATION_TEXT = "RESET SOLO RANK";

    private BanPickRatingDefaults() {
    }
}
