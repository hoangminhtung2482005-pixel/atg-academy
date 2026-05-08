package com.example.demo.dto.esports;

import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;

public record EsportsMatchGameLineupResponse(
        Long id,
        Long gameId,
        Long teamId,
        String teamCode,
        String teamName,
        String teamLogoUrl,
        BanPickTeamSide teamSide,
        Integer positionNumber,
        String laneRole,
        Long heroId,
        String heroName,
        String heroSlug,
        String heroAvatarUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
