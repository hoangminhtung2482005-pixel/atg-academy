package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

public record BanPickConfirmRequest(
        BanPickTeamSide teamSide,
        BanPickActionType actionType,
        Long heroId,
        String heroName
) {
}
