package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsDraftTournamentScopeAggregate(
        Long tournamentId,
        String tournamentName,
        String tournamentTier,
        String franchiseCode,
        LocalDateTime latestMatchDate
) {
}
