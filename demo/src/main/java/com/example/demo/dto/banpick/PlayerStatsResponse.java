package com.example.demo.dto.banpick;

import java.util.List;

public record PlayerStatsResponse(
        BanPickUserSummary user,
        Integer totalMatches,
        Integer wins,
        Integer losses,
        Double winRate,
        Integer rating,
        String rankCode,
        String rankLabel,
        List<HeroPickStatResponse> mostPickedHeroes
) {
}
