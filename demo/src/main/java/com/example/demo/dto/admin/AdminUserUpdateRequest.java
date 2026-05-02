package com.example.demo.dto.admin;

public record AdminUserUpdateRequest(
        String name,
        String avatarUrl,
        String role,
        String status,
        String note
) {
}
