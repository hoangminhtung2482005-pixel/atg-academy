package com.example.demo.dto.esports;

public record EsportsHeroBanStatAggregate(
        Long heroId,
        String heroName,
        String heroAvatarUrl,
        Long banCount
) {
}
