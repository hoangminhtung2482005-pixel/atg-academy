package com.example.demo.service;

final class BanPickRatingRules {

    static final int INITIAL_RATING = 1000;
    static final int BASE_WIN_DELTA = 30;
    static final int BASE_LOSS_DELTA = -20;
    static final int MIN_RATING = 0;

    static final int BALANCE_RATING = 1500;
    static final int RATING_STEP = 10;
    static final double WIN_ADJUSTMENT_PER_STEP = 0.02;
    static final int MIN_WIN_DELTA = 20;
    static final int ACTIVE_WINDOW_DAYS = 30;
    static final int MIN_ACTIVE_PLAYERS = 4;

    static final int GAP_RATING_STEP = 10;
    static final double GAP_ADJUSTMENT_PER_STEP = 0.02;
    static final double MAX_GAP_MODIFIER_RATIO = 0.50;
    static final int MIN_FINAL_WIN_DELTA = 0;
    static final int MAX_RATED_PAIR_MATCHES_PER_48H = 2;
    static final int ANTI_WIN_TRADING_RESET_HOURS = 48;

    private BanPickRatingRules() {
    }

    static RatingDeltaSnapshot applyGapModifier(int winnerRatingBefore,
                                                int loserRatingBefore,
                                                int baseWinDelta,
                                                int baseLossDelta) {
        int safeWinnerRating = Math.max(MIN_RATING, winnerRatingBefore);
        int safeLoserRating = Math.max(MIN_RATING, loserRatingBefore);
        int safeWinDelta = Math.max(MIN_FINAL_WIN_DELTA, baseWinDelta);
        int safeLossDelta = -Math.abs(baseLossDelta);

        double modifierRatio = resolveGapModifierRatio(safeWinnerRating, safeLoserRating);
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

    private static double resolveGapModifierRatio(int winnerRatingBefore, int loserRatingBefore) {
        double rawRatio = (Math.abs((double) winnerRatingBefore - loserRatingBefore) / GAP_RATING_STEP)
                * GAP_ADJUSTMENT_PER_STEP;
        if (!Double.isFinite(rawRatio)) {
            return 0.0;
        }
        return Math.min(MAX_GAP_MODIFIER_RATIO, Math.max(0.0, rawRatio));
    }

    record RatingDeltaSnapshot(
            int winDelta,
            int lossDelta,
            double modifierRatio,
            boolean winnerWasUnderdog
    ) {
    }
}
