package com.example.demo.dto.esports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EsportsGameDraftResponse(
        Long id,
        Long matchId,
        Integer gameNumber,
        TeamSummary blueTeam,
        TeamSummary redTeam,
        TeamSummary winnerTeam,
        Integer durationSeconds,
        String durationText,
        String draftFormatCode,
        String source,
        List<HeroSummary> blueBans,
        List<HeroSummary> redBans,
        Map<String, HeroSummary> blueLineup,
        Map<String, HeroSummary> redLineup,
        DraftCompleteness draftCompleteness,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record TeamSummary(
            Long id,
            String teamCode,
            String teamName,
            String logoUrl
    ) {
    }

    public record HeroSummary(
            Long id,
            String name,
            String slug,
            String avatarUrl
    ) {
    }

    public record DraftCompleteness(
            int banCount,
            int pickCount,
            boolean complete,
            String status,
            List<String> missingFields
    ) {
    }
}
