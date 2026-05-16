package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BanPickRatingRulesTest {

    @Test
    void gapModifierKeepsBaseDeltasWhenRatingsAreEqual() {
        BanPickRatingRules.RatingDeltaSnapshot snapshot = BanPickRatingRules.applyGapModifier(1000, 1000, 30, -20);

        assertThat(snapshot.winDelta()).isEqualTo(30);
        assertThat(snapshot.lossDelta()).isEqualTo(-20);
        assertThat(snapshot.modifierRatio()).isEqualTo(0.0);
        assertThat(snapshot.winnerWasUnderdog()).isFalse();
    }

    @Test
    void gapModifierRewardsUnderdogWinAndPunishesFavoriteLoss() {
        BanPickRatingRules.RatingDeltaSnapshot snapshot = BanPickRatingRules.applyGapModifier(1000, 1200, 24, -20);

        assertThat(snapshot.winDelta()).isEqualTo(34);
        assertThat(snapshot.lossDelta()).isEqualTo(-28);
        assertThat(snapshot.modifierRatio()).isEqualTo(0.4);
        assertThat(snapshot.winnerWasUnderdog()).isTrue();
    }

    @Test
    void gapModifierReducesFavoriteWinAndLightensUnderdogLoss() {
        BanPickRatingRules.RatingDeltaSnapshot snapshot = BanPickRatingRules.applyGapModifier(1200, 1000, 24, -20);

        assertThat(snapshot.winDelta()).isEqualTo(14);
        assertThat(snapshot.lossDelta()).isEqualTo(-12);
        assertThat(snapshot.modifierRatio()).isEqualTo(0.4);
        assertThat(snapshot.winnerWasUnderdog()).isFalse();
    }

    @Test
    void gapModifierCapsRatioAtFiftyPercent() {
        BanPickRatingRules.RatingDeltaSnapshot underdogSnapshot =
                BanPickRatingRules.applyGapModifier(1000, 2000, 30, -20);
        BanPickRatingRules.RatingDeltaSnapshot favoriteSnapshot =
                BanPickRatingRules.applyGapModifier(2000, 1000, 30, -20);

        assertThat(underdogSnapshot.modifierRatio()).isEqualTo(0.5);
        assertThat(underdogSnapshot.winDelta()).isEqualTo(45);
        assertThat(underdogSnapshot.lossDelta()).isEqualTo(-30);
        assertThat(favoriteSnapshot.modifierRatio()).isEqualTo(0.5);
        assertThat(favoriteSnapshot.winDelta()).isEqualTo(15);
        assertThat(favoriteSnapshot.lossDelta()).isEqualTo(-10);
    }
}
