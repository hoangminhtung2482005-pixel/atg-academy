package com.example.demo.dto.admin;

public record AdminHeroBasicUpdateRequest(
        String name,
        String slug,
        String heroClass,
        java.util.List<String> classes,
        java.math.BigDecimal banPickScore,
        String description,
        String avatarUrl,
        String portraitUrl,
        String bannerUrl,
        String difficulty
) {
}
