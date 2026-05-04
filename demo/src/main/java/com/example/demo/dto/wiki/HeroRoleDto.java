package com.example.demo.dto.wiki;

import com.example.demo.entity.HeroRole;

public record HeroRoleDto(
        Long id,
        String code,
        String name
) {
    public static HeroRoleDto from(HeroRole role) {
        if (role == null) {
            return null;
        }
        return new HeroRoleDto(role.getId(), role.getCode(), role.getName());
    }
}
