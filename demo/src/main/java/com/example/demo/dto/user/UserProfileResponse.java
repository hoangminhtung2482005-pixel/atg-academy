package com.example.demo.dto.user;

import com.example.demo.entity.User;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        String role,
        String level
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getRole(),
                user.resolveLevel()
        );
    }
}
