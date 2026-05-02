package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickTeamSide;

import java.util.List;

public record BanPickLineupReorderRequest(
        BanPickTeamSide teamSide,
        List<Long> heroIds
) {
}
