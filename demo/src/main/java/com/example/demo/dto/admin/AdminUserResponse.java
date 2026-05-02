package com.example.demo.dto.admin;

import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String name,
        String avatarUrl,
        String role,
        UserStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE,
                user.getNote(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
