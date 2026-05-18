package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickMatchMode;
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
        BanPickMatchMode mode,
        BanPickSeriesType virtualSeriesFormat,
        Integer virtualGameIndex,
        Boolean ultimateBattle,
        Integer prepDurationSeconds,
        LocalDateTime prepPhaseStartAt,
        LocalDateTime prepPhaseEndAt,
        List<Long> bluePreviousUsedHeroIds,
        List<Long> redPreviousUsedHeroIds,
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
        /**
         * Strategy pool for the current user only (hero IDs they want to prioritize).
         * Null when the state is broadcast to all (withoutCurrentUserSide).
         * Strategy pool is private per player and not shared with the opponent.
         */
        List<Long> myStrategyPool,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long draftHistoryId,
        BanPickPhaseResponse currentPhase,
        List<BanPickParticipantResponse> participants,
        List<BanPickActionResponse> actions
) {
    /**
     * Returns a copy of this state without the current user's side and strategy pool.
     * Used when broadcasting to all subscribers so each player only sees their own pool.
     */
    public BanPickRoomStateResponse withoutCurrentUserSide() {
        if (currentUserSide == null && myStrategyPool == null) {
            return this;
        }
        // Strip strategy pools from all participant entries when broadcasting
        List<BanPickParticipantResponse> strippedParticipants = participants == null ? null :
                participants.stream()
                        .map(p -> new BanPickParticipantResponse(
                                p.user(), p.role(), p.teamSide(), p.joinedAt(), null))
                        .toList();
        return new BanPickRoomStateResponse(
                id,
                roomCode,
                mode,
                virtualSeriesFormat,
                virtualGameIndex,
                ultimateBattle,
                prepDurationSeconds,
                prepPhaseStartAt,
                prepPhaseEndAt,
                bluePreviousUsedHeroIds,
                redPreviousUsedHeroIds,
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
                null,
                createdAt,
                updatedAt,
                draftHistoryId,
                currentPhase,
                strippedParticipants,
                actions
        );
    }
}
