package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsTournamentTeamResponse(
        Long id,
        Long tournamentId,
        Long teamId,
        String teamCode,
        String teamName,
        String logoUrl,
        String groupName,
        Integer seedNumber,
        String status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
