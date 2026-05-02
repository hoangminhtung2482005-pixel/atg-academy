package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickRoomStatus;
import com.example.demo.entity.BanPickSeriesType;
import com.example.demo.entity.BanPickPhaseType;
import com.example.demo.entity.BanPickTeamSide;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BanPickRoomStateResponse(
        Long id,
        String roomCode,
        BanPickRoomStatus status,
        BanPickPhaseType phaseType,
        BanPickSeriesType seriesType,
        Integer currentGameNumber,
        Integer maxGames,
        Map<Integer, List<Long>> blueUsedPicksByGame,
        Map<Integer, List<Long>> redUsedPicksByGame,
        List<Long> blueUsedPicks,
        List<Long> redUsedPicks,
        Map<String, List<Long>> usedHeroesByTeam,
        Boolean isFinalGame,
        Boolean bo7ResetActive,
        BanPickUserSummary hostUser,
        BanPickUserSummary guestUser,
        BanPickUserSummary blueUser,
        BanPickUserSummary redUser,
        Boolean hostReady,
        Boolean guestReady,
        Integer currentPhaseIndex,
        Integer currentPhaseSelectedCount,
        Integer phaseDurationSeconds,
        LocalDateTime timerStartedAt,
        LocalDateTime phaseDeadlineAt,
        LocalDateTime lineupDeadlineAt,
        Boolean blueLineupConfirmed,
        Boolean redLineupConfirmed,
        List<Long> bluePickOrder,
        List<Long> redPickOrder,
        BanPickTeamSide currentUserSide,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long draftHistoryId,
        BanPickPhaseResponse currentPhase,
        List<BanPickParticipantResponse> participants,
        List<BanPickActionResponse> actions
) {
    public BanPickRoomStateResponse withoutCurrentUserSide() {
        if (currentUserSide == null) {
            return this;
        }
        return new BanPickRoomStateResponse(
                id,
                roomCode,
                status,
                phaseType,
                seriesType,
                currentGameNumber,
                maxGames,
                blueUsedPicksByGame,
                redUsedPicksByGame,
                blueUsedPicks,
                redUsedPicks,
                usedHeroesByTeam,
                isFinalGame,
                bo7ResetActive,
                hostUser,
                guestUser,
                blueUser,
                redUser,
                hostReady,
                guestReady,
                currentPhaseIndex,
                currentPhaseSelectedCount,
                phaseDurationSeconds,
                timerStartedAt,
                phaseDeadlineAt,
                lineupDeadlineAt,
                blueLineupConfirmed,
                redLineupConfirmed,
                bluePickOrder,
                redPickOrder,
                null,
                createdAt,
                updatedAt,
                draftHistoryId,
                currentPhase,
                participants,
                actions
        );
    }
}
