package com.example.demo.dto.user;

import com.example.demo.entity.User;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String role,
        String level,
        String playerBadgeCode,
        String playerBadgeName,
        String playerBadgeIconUrl,
        String playerTitle
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getRole(),
                user.resolveLevel(),
                user.resolvePlayerBadgeCode(),
                user.resolvePlayerBadgeName(),
                user.resolvePlayerBadgeIconUrl(),
                user.resolvePlayerTitle()
        );
    }
}
