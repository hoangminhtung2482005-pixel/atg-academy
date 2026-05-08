package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsMatchGameResponse(
        Long id,
        Long matchId,
        Integer gameNumber,
        Long blueTeamId,
        String blueTeamCode,
        String blueTeamName,
        String blueTeamLogoUrl,
        Long redTeamId,
        String redTeamCode,
        String redTeamName,
        String redTeamLogoUrl,
        Long winnerTeamId,
        String winnerTeamCode,
        String winnerTeamName,
        String winnerTeamLogoUrl,
        Integer durationSeconds,
        Long draftFormatId,
        String draftFormatCode,
        String draftFormatName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
