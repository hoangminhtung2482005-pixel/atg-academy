package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickTeamSide;

public record BanPickLineupConfirmRequest(
        BanPickTeamSide teamSide
) {
}
