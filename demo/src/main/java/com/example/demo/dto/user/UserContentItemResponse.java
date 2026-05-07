package com.example.demo.dto.user;

import java.time.LocalDateTime;

public record UserContentItemResponse(
        Long id,
        String title,
        String type,
        String status,
        String detailUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
}
