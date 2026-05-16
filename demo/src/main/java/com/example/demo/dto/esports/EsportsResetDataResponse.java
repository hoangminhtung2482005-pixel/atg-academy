package com.example.demo.dto.esports;

public record EsportsResetDataResponse(
        boolean reset,
        String backupFile,
        long deletedGameDrafts,
        long deletedMatches,
        long deletedPlayerStats,
        long remainingGameDrafts,
        long remainingMatches,
        boolean playerStatsCleared,
        String playerStatsRetainedReason
) {
}
