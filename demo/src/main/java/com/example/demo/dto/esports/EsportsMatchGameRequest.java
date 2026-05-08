package com.example.demo.dto.esports;

public record EsportsMatchGameRequest(
        Integer gameNumber,
        Long blueTeamId,
        Long redTeamId,
        Long winnerTeamId,
        Integer durationSeconds,
        Long draftFormatId
) {
}
