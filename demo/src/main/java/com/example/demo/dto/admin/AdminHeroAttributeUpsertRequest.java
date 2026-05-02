package com.example.demo.dto.admin;

public record AdminHeroAttributeUpsertRequest(
        String name,
        String description,
        String iconUrl,
        Integer sortOrder
) {
}
