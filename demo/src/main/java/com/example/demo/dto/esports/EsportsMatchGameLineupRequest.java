package com.example.demo.dto.esports;

import com.example.demo.entity.BanPickTeamSide;

public record EsportsMatchGameLineupRequest(
        Long teamId,
        BanPickTeamSide teamSide,
        Integer positionNumber,
        String laneRole,
        Long heroId
) {
}
