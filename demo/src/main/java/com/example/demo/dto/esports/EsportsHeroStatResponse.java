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
        Long presenceCount
) {
}
