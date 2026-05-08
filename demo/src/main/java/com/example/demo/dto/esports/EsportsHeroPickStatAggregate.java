package com.example.demo.dto.esports;

public record EsportsHeroPickStatAggregate(
        Long heroId,
        String heroName,
        String heroAvatarUrl,
        Long pickCount,
        Long pickWins,
        Long bluePickCount,
        Long blueWins,
        Long redPickCount,
        Long redWins
) {
}
