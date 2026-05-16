package com.example.demo.dto.esports;

public record EsportsHeroStatResponse(
        Long heroId,
        String heroName,
        String heroAvatarUrl,
        Long pickCount,
        Long pickWins,
        Long pickLosses,
        Double pickWinRate,
        Long bluePickCount,
        Long blueWins,
        Long blueLosses,
        Double blueWinRate,
        Long redPickCount,
        Long redWins,
        Long redLosses,
        Double redWinRate,
        Long banCount,
        Long blueBanCount,
        Long redBanCount,
        Long presenceCount,
        String heroIconUrl,
        Double pickRate,
        Long bluePickWins,
        Long bluePickLosses,
        Double bluePickWinRate,
        Long redPickWins,
        Long redPickLosses,
        Double redPickWinRate,
        Double banRate,
        Double presenceRate
) {
}
