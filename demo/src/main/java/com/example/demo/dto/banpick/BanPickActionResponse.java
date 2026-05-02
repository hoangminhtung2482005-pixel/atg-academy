package com.example.demo.dto.banpick;

import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;

public record BanPickActionResponse(
        Long id,
        BanPickUserSummary user,
        BanPickTeamSide teamSide,
        BanPickActionType actionType,
        Long heroId,
        String heroName,
        HeroSummaryDto hero,
        Integer phaseIndex,
        LocalDateTime confirmedAt,
        boolean isNew
) {
}
