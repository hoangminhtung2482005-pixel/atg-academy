package com.example.demo.dto.esports;

import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;

public record EsportsDraftActionResponse(
        Long id,
        Long gameId,
        Long teamId,
        String teamCode,
        String teamName,
        String teamLogoUrl,
        Long heroId,
        String heroName,
        String heroSlug,
        String heroAvatarUrl,
        BanPickActionType actionType,
        Integer stepNumber,
        BanPickTeamSide teamSide,
        LocalDateTime createdAt
) {
}
