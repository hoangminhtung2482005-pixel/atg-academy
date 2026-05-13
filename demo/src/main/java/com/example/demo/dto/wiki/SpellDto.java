package com.example.demo.dto.wiki;

public record SpellDto(
        String slug,
        String name,
        String iconUrl,
        String description
) {
}
