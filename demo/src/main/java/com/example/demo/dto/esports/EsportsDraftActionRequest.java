package com.example.demo.dto.esports;

import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

public record EsportsDraftActionRequest(
        Long teamId,
        Long heroId,
        BanPickActionType actionType,
        Integer stepNumber,
        BanPickTeamSide teamSide
) {
}
