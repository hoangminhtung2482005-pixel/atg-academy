package com.example.demo.dto.esports;

public record EsportsTournamentOptionResponse(
        Long tournamentId,
        String tournamentName,
        String tournamentTier,
        String franchiseCode,
        boolean legacyScope
) {
}
