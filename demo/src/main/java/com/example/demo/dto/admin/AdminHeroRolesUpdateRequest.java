package com.example.demo.dto.admin;

import java.util.List;

public record AdminHeroRolesUpdateRequest(
        Long primaryRoleId,
        List<Long> subRoleIds,
        List<String> roles
) {
}
