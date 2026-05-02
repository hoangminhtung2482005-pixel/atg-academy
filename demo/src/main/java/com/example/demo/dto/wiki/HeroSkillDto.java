package com.example.demo.dto.wiki;

import com.example.demo.entity.HeroSkill;
import com.example.demo.entity.HeroSkillType;

public record HeroSkillDto(
        Long id,
        String name,
        HeroSkillType skillType,
        String description,
        String cooldown,
        String manaCost,
        String iconUrl,
        Integer sortOrder
) {
    public static HeroSkillDto from(HeroSkill skill) {
        if (skill == null) {
            return null;
        }

        return new HeroSkillDto(
                skill.getId(),
                skill.getName(),
                skill.getSkillType(),
                skill.getDescription(),
                skill.getCooldown(),
                skill.getManaCost(),
                skill.getIconUrl(),
                skill.getSortOrder()
        );
    }
}
