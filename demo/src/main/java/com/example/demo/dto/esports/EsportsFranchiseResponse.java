package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsFranchiseResponse(
        Long id,
        String code,
        String name,
        String tierLevel,
        String region,
        String description,
        String logoUrl,
        Integer displayOrder,
        Boolean active,
        Long tournamentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
