package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickSeasonResetType;

import java.math.BigDecimal;
import java.util.List;

public record BanPickSeasonResetPreviewResponse(
        BanPickSeasonResetType resetType,
        int baseRating,
        String formula,
        long affectedPlayerCount,
        RatingSummary before,
        RatingSummary after,
        List<PlayerRatingSample> samples
) {

    public record RatingSummary(
            Integer minRating,
            Integer maxRating,
            BigDecimal averageRating
    ) {
    }

    public record PlayerRatingSample(
            Long userId,
            String email,
            String displayName,
            int beforeRating,
            int afterRating
    ) {
    }
}
