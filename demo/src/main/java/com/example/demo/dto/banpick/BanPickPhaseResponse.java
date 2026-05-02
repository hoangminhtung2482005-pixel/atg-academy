package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;

public record BanPickPhaseResponse(
        Integer phaseIndex,
        BanPickTeamSide teamSide,
        BanPickActionType actionType,
        Integer count,
        String label
) {
}
