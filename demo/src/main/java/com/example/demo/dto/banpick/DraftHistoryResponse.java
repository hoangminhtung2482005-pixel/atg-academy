package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;
import java.util.List;

public record DraftHistoryResponse(
        Long id,
        String roomCode,
        BanPickUserSummary blueUser,
        BanPickUserSummary redUser,
        BanPickUserSummary winnerUser,
        BanPickTeamSide winnerSide,
        List<String> bluePicks,
        List<String> redPicks,
        List<String> blueBans,
        List<String> redBans,
        LocalDateTime createdAt,
        LocalDateTime resultRecordedAt,
        String shareUrl
) {
}
