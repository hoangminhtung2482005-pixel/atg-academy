package com.example.demo.dto.admin;

import com.example.demo.entity.HeroAttribute;

public record AdminHeroAttributeResponse(
        Long id,
        String name,
        String description,
        String iconUrl,
        Integer sortOrder,
        long usageCount
) {
    public static AdminHeroAttributeResponse from(HeroAttribute attribute, long usageCount) {
        return new AdminHeroAttributeResponse(
                attribute.getId(),
                attribute.getName(),
                attribute.getDescription(),
                attribute.getIconUrl(),
                attribute.getSortOrder(),
                usageCount
        );
    }
}
