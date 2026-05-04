package com.example.demo.dto.admin;

import com.example.demo.entity.HeroRole;

public record AdminHeroRoleOption(
        Long id,
        String code,
        String name
) {
    public static AdminHeroRoleOption from(HeroRole role) {
        return new AdminHeroRoleOption(role.getId(), role.getCode(), role.getName());
    }
}
