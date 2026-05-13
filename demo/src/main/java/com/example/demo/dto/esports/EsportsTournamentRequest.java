package com.example.demo.dto.esports;

import java.time.LocalDate;

public record EsportsTournamentRequest(
        Long franchiseId,
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
        String logoUrl
) {
}
