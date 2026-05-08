package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsDraftTournamentAggregate(
        String tournamentTier,
        LocalDateTime latestMatchDate
) {
}
