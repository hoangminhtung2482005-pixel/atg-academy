package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.DraftHistoryEndReason;

import java.time.LocalDateTime;
import java.util.List;

public record DraftHistoryResponse(
        Long id,
        String roomCode,
        BanPickUserSummary blueUser,
        BanPickUserSummary redUser,
        BanPickUserSummary winnerUser,
        BanPickTeamSide winnerSide,
        BanPickUserSummary dodgedUser,
        DraftHistoryEndReason endReason,
        List<String> bluePicks,
        List<String> redPicks,
        List<String> blueBans,
        List<String> redBans,
        LocalDateTime createdAt,
        LocalDateTime resultRecordedAt
) {
}
