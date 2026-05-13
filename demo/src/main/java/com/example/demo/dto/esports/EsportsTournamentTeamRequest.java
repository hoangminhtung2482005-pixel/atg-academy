package com.example.demo.dto.esports;

public record EsportsTournamentTeamRequest(
        Long teamId,
        String groupName,
        Integer seedNumber,
        String status,
        String note
) {
}
