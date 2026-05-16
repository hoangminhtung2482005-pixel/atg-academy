package com.example.demo.dto.user;

public record UserProfileUpdateRequest(
        String displayName,
        String level,
        String playerBadgeCode,
        String playerBadgeName,
        String playerBadgeIconUrl,
        String playerTitle
) {
}
