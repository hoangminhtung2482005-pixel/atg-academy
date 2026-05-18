package com.example.demo.service;

final class BanPickRatingRules {

    static final int INITIAL_RATING = BanPickRatingDefaults.INITIAL_RATING;
    static final int BASE_WIN_DELTA = BanPickRatingDefaults.BASE_WIN_DELTA;
    static final int BASE_LOSS_DELTA = BanPickRatingDefaults.BASE_LOSS_DELTA;
    static final int MIN_RATING = BanPickRatingDefaults.MIN_RATING;

    static final int BALANCE_RATING = BanPickRatingDefaults.MACRO_BALANCE_RATING;
    static final int RATING_STEP = BanPickRatingDefaults.MACRO_RATING_STEP;
    static final double WIN_ADJUSTMENT_PER_STEP = BanPickRatingDefaults.MACRO_WIN_ADJUSTMENT_PER_STEP;
    static final int MIN_WIN_DELTA = BanPickRatingDefaults.MACRO_MIN_WIN_DELTA;
    static final int ACTIVE_WINDOW_DAYS = BanPickRatingDefaults.MACRO_ACTIVE_WINDOW_DAYS;
    static final int MIN_ACTIVE_PLAYERS = BanPickRatingDefaults.MACRO_MINIMUM_ACTIVE_PLAYERS;

    static final int GAP_RATING_STEP = BanPickRatingDefaults.GAP_RATING_DIFF_STEP;
    static final double GAP_ADJUSTMENT_PER_STEP = BanPickRatingDefaults.GAP_MODIFIER_PER_STEP;
    static final double MAX_GAP_MODIFIER_RATIO = BanPickRatingDefaults.GAP_MAX_MODIFIER;
    static final int MIN_FINAL_WIN_DELTA = BanPickRatingDefaults.MIN_FINAL_WIN_DELTA;
    static final int MAX_RATED_PAIR_MATCHES_PER_48H = BanPickRatingDefaults.ANTI_TRADING_ALLOWED_RECENT_MATCHES;
    static final int ANTI_WIN_TRADING_RESET_HOURS = BanPickRatingDefaults.ANTI_TRADING_WINDOW_HOURS;

    private BanPickRatingRules() {
    }

    static RatingDeltaSnapshot applyGapModifier(int winnerRatingBefore,
                                                int loserRatingBefore,
                                                int baseWinDelta,
                                                int baseLossDelta) {
        return applyGapModifier(
                winnerRatingBefore,
                loserRatingBefore,
                baseWinDelta,
                baseLossDelta,
                MIN_RATING,
                GAP_RATING_STEP,
                GAP_ADJUSTMENT_PER_STEP,
                MAX_GAP_MODIFIER_RATIO
        );
    }

    static RatingDeltaSnapshot applyGapModifier(int winnerRatingBefore,
                                                int loserRatingBefore,
                                                int baseWinDelta,
                                                int baseLossDelta,
                                                int minRating,
                                                int gapRatingStep,
                                                double gapAdjustmentPerStep,
                                                double maxGapModifierRatio) {
        int safeMinimumRating = Math.max(0, minRating);
        int safeWinnerRating = Math.max(safeMinimumRating, winnerRatingBefore);
        int safeLoserRating = Math.max(safeMinimumRating, loserRatingBefore);
        int safeWinDelta = Math.max(MIN_FINAL_WIN_DELTA, baseWinDelta);
        int safeLossDelta = -Math.abs(baseLossDelta);

        double modifierRatio = resolveGapModifierRatio(
                safeWinnerRating,
                safeLoserRating,
                gapRatingStep,
                gapAdjustmentPerStep,
                maxGapModifierRatio
        );
        boolean winnerWasUnderdog = safeWinnerRating < safeLoserRating;
        double multiplier = winnerWasUnderdog ? 1.0 + modifierRatio : 1.0 - modifierRatio;

        if (!Double.isFinite(multiplier)) {
            return new RatingDeltaSnapshot(safeWinDelta, safeLossDelta, 0.0, false);
        }

        int adjustedWinDelta = Math.max(MIN_FINAL_WIN_DELTA, (int) Math.round(safeWinDelta * multiplier));
        int adjustedLossDelta = (int) Math.round(safeLossDelta * multiplier);
        if (adjustedLossDelta > 0) {
            adjustedLossDelta = safeLossDelta;
        }

        return new RatingDeltaSnapshot(
                adjustedWinDelta,
                adjustedLossDelta,
                modifierRatio,
                winnerWasUnderdog
        );
    }

    static RatingDeltaSnapshot noRatingChange() {
        return new RatingDeltaSnapshot(0, 0, 0.0, false);
    }

    private static double resolveGapModifierRatio(int winnerRatingBefore,
                                                  int loserRatingBefore,
                                                  int gapRatingStep,
                                                  double gapAdjustmentPerStep,
                                                  double maxGapModifierRatio) {
        if (gapRatingStep <= 0 || !Double.isFinite(gapAdjustmentPerStep) || gapAdjustmentPerStep < 0.0) {
            return 0.0;
        }
        double rawRatio = (Math.abs((double) winnerRatingBefore - loserRatingBefore) / gapRatingStep)
                * gapAdjustmentPerStep;
        if (!Double.isFinite(rawRatio)) {
            return 0.0;
        }
        return Math.min(Math.max(0.0, maxGapModifierRatio), Math.max(0.0, rawRatio));
    }

    record RatingDeltaSnapshot(
            int winDelta,
            int lossDelta,
            double modifierRatio,
            boolean winnerWasUnderdog
    ) {
    }
}
