package com.example.demo.dto.esports;

public record EsportsHeroBanBreakdownAggregate(
        Long heroId,
        String heroName,
        String heroAvatarUrl,
        Long banCount,
        Long blueBanCount,
        Long redBanCount
) {
}
