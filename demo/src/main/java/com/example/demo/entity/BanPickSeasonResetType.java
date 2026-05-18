package com.example.demo.entity;

import java.util.Locale;

public enum BanPickSeasonResetType {
    NONE {
        @Override
        public int applyTo(int currentRating) {
            return safeRating(currentRating);
        }

        @Override
        public int applyTo(int currentRating, int baseRating, int minRating) {
            return safeRating(currentRating);
        }

        @Override
        public String formulaDescription() {
            return "No reset";
        }

        @Override
        public String formulaDescription(int baseRating) {
            return formulaDescription();
        }
    },
    SOFT {
        @Override
        public int applyTo(int currentRating) {
            return Math.max(MIN_RATING, (int) Math.round((safeRating(currentRating) + (double) BASE_RATING) / 2.0));
        }

        @Override
        public int applyTo(int currentRating, int baseRating, int minRating) {
            int safeBaseRating = Math.max(minRating, baseRating);
            return Math.max(minRating, (int) Math.round((safeRating(currentRating) + (double) safeBaseRating) / 2.0));
        }

        @Override
        public String formulaDescription() {
            return "round((currentRating + 1000) / 2)";
        }

        @Override
        public String formulaDescription(int baseRating) {
            return "round((currentRating + " + baseRating + ") / 2)";
        }
    },
    HARD {
        @Override
        public int applyTo(int currentRating) {
            return BASE_RATING;
        }

        @Override
        public int applyTo(int currentRating, int baseRating, int minRating) {
            return Math.max(minRating, baseRating);
        }

        @Override
        public String formulaDescription() {
            return "1000";
        }

        @Override
        public String formulaDescription(int baseRating) {
            return String.valueOf(baseRating);
        }
    };

    public static final int BASE_RATING = 1000;
    private static final int MIN_RATING = 0;

    public abstract int applyTo(int currentRating);

    public abstract int applyTo(int currentRating, int baseRating, int minRating);

    public abstract String formulaDescription();

    public abstract String formulaDescription(int baseRating);

    public boolean isExecutable() {
        return this != NONE;
    }

    public static BanPickSeasonResetType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type phai la SOFT hoac HARD.");
        }
        try {
            BanPickSeasonResetType type = BanPickSeasonResetType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (!type.isExecutable()) {
                throw new IllegalArgumentException("type phai la SOFT hoac HARD.");
            }
            return type;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("type phai la SOFT hoac HARD.");
        }
    }

    private static int safeRating(int currentRating) {
        return Math.max(MIN_RATING, currentRating);
    }
}
