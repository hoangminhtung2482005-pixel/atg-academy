package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickParticipantRole;
import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;
import java.util.List;

public record BanPickParticipantResponse(
        BanPickUserSummary user,
        BanPickParticipantRole role,
        BanPickTeamSide teamSide,
        LocalDateTime joinedAt,
        /**
         * Strategy pool for this participant (hero IDs they want to prioritize).
         * Only populated for the current user's own participant entry.
         * Null for opponent entries to keep pool private.
         */
        List<Long> strategyPool
) {
}
