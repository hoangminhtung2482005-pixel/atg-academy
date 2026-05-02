package com.example.demo.dto.admin;

import java.util.List;

public record AdminHeroDetailResponse(
        AdminHeroResponse hero,
        List<AdminHeroRoleOption> availableRoles,
        List<AdminHeroAttributeResponse> availableAttributes,
        List<String> availableClasses,
        List<String> difficulties,
        List<String> suggestedRoles
) {
}
