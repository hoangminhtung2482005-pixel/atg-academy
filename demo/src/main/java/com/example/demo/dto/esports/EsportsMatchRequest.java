package com.example.demo.dto.esports;

import java.time.LocalDateTime;

public record EsportsMatchRequest(
        LocalDateTime matchDate,
        String team1Code,
        String team2Code,
        Integer score1,
        Integer score2,
        String tier,
        String stage,
        Long tournamentId
) {
}
