package com.example.demo.dto.esports;

public record EsportsHeroBanStatResponse(
        Long heroId,
        String heroName,
        String heroAvatarUrl,
        Long banCount,
        String tournamentName
) {
}
