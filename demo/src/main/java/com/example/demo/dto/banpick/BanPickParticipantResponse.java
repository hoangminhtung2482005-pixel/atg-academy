package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickParticipantRole;
import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;

public record BanPickParticipantResponse(
        BanPickUserSummary user,
        BanPickParticipantRole role,
        BanPickTeamSide teamSide,
        LocalDateTime joinedAt
) {
}
