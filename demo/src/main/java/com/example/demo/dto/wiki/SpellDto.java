package com.example.demo.dto.wiki;

import com.example.demo.entity.Spell;

import java.time.LocalDateTime;

public record SpellDto(
        Long id,
        String name,
        String slug,
        String iconUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SpellDto from(Spell spell) {
        if (spell == null) {
            return null;
        }
        return new SpellDto(
                spell.getId(),
                spell.getName(),
                spell.getSlug(),
                spell.getIconUrl(),
                spell.getCreatedAt(),
                spell.getUpdatedAt()
        );
    }
}
