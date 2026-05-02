package com.example.demo.dto.admin;

import java.util.List;

public record AdminHeroRolesUpdateRequest(
        List<String> roles
) {
}
