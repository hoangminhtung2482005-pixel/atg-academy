package com.example.demo.dto.esports;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EsportsTournamentResponse(
        Long id,
        Long franchiseId,
        String franchiseCode,
        String franchiseName,
        String name,
        String slug,
        Integer seasonYear,
        String splitName,
        String tierLevel,
        Integer aerTier,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String description,
        String logoUrl,
        Long teamCount,
        Long linkedMatchCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
