package com.example.demo.dto.esports;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record EsportsGameDraftRequest(
        Integer gameNumber,
        Long blueTeamId,
        Long redTeamId,
        Long winnerTeamId,
        Integer durationSeconds,
        String draftFormatCode,
        String source,
        List<Long> blueBans,
        List<Long> redBans,
        LineupRequest blueLineup,
        LineupRequest redLineup
) {

    public record LineupRequest(
            @JsonAlias("DSL") Long dsl,
            @JsonAlias("JGL") Long jgl,
            @JsonAlias("MID") Long mid,
            @JsonAlias("ADL") Long adl,
            @JsonAlias("SUP") Long sup
    ) {
    }
}
