package com.example.demo.service;

import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BanPickMacroEconomyService {

    private final DraftHistoryRepository draftHistoryRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final Clock clock;

    @Autowired
    public BanPickMacroEconomyService(DraftHistoryRepository draftHistoryRepository,
                                      PlayerStatsRepository playerStatsRepository) {
        this(draftHistoryRepository, playerStatsRepository, Clock.systemDefaultZone());
    }

    BanPickMacroEconomyService(DraftHistoryRepository draftHistoryRepository,
                               PlayerStatsRepository playerStatsRepository,
                               Clock clock) {
        this.draftHistoryRepository = draftHistoryRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MacroEconomySnapshot getCurrentSnapshot() {
        LocalDate snapshotDate = LocalDate.now(clock);
        LocalDateTime windowEnd = snapshotDate.atStartOfDay();
        LocalDateTime windowStart = windowEnd.minusDays(BanPickRatingRules.ACTIVE_WINDOW_DAYS);

        List<DraftHistory> completedHistories = draftHistoryRepository.findCompletedBetween(windowStart, windowEnd);
        Map<Long, Integer> completedMatchesByUserId = countCompletedMatchesByUser(completedHistories);
        if (completedMatchesByUserId.size() < BanPickRatingRules.MIN_ACTIVE_PLAYERS) {
            return MacroEconomySnapshot.fallback(snapshotDate, completedMatchesByUserId.size());
        }

        List<Map.Entry<Long, Integer>> sortedPlayers = completedMatchesByUserId.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();

        int basePoolSize = Math.max(1, (sortedPlayers.size() + 1) / 2);
        int cutoffMatchCount = sortedPlayers.get(basePoolSize - 1).getValue();
        List<Long> activePoolUserIds = sortedPlayers.stream()
                .filter(entry -> entry.getValue() >= cutoffMatchCount)
                .map(Map.Entry::getKey)
                .toList();

        if (activePoolUserIds.isEmpty()) {
            return MacroEconomySnapshot.fallback(snapshotDate, completedMatchesByUserId.size());
        }

        Map<Long, Integer> ratingsByUserId = currentRatingsByUserId();
        double averageRating = activePoolUserIds.stream()
                .mapToInt(userId -> ratingsByUserId.getOrDefault(userId, BanPickRatingRules.INITIAL_RATING))
                .average()
                .orElse(BanPickRatingRules.BALANCE_RATING);

        return new MacroEconomySnapshot(
                snapshotDate,
                completedMatchesByUserId.size(),
                activePoolUserIds.size(),
                averageRating,
                resolveWinDelta(averageRating)
        );
    }

    public int getCurrentWinDelta() {
        return getCurrentSnapshot().winDelta();
    }

    private Map<Long, Integer> countCompletedMatchesByUser(List<DraftHistory> completedHistories) {
        Map<Long, Integer> completedMatchesByUserId = new LinkedHashMap<>();
        if (completedHistories == null) {
            return completedMatchesByUserId;
        }
        for (DraftHistory history : completedHistories) {
            increment(completedMatchesByUserId, history != null ? history.getBlueUser() : null);
            increment(completedMatchesByUserId, history != null ? history.getRedUser() : null);
        }
        return completedMatchesByUserId;
    }

    private void increment(Map<Long, Integer> countsByUserId, User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        countsByUserId.merge(user.getId(), 1, Integer::sum);
    }

    private Map<Long, Integer> currentRatingsByUserId() {
        Map<Long, Integer> ratingsByUserId = new LinkedHashMap<>();
        for (PlayerStats stats : playerStatsRepository.findAll()) {
            if (stats == null || stats.getUser() == null || stats.getUser().getId() == null) {
                continue;
            }
            ratingsByUserId.put(stats.getUser().getId(), safeRating(stats.getRating()));
        }
        return ratingsByUserId;
    }

    private int resolveWinDelta(double averageRating) {
        double difference = averageRating - BanPickRatingRules.BALANCE_RATING;
        double adjustmentRatio = (difference / BanPickRatingRules.RATING_STEP)
                * BanPickRatingRules.WIN_ADJUSTMENT_PER_STEP;
        double rawWinDelta = BanPickRatingRules.BASE_WIN_DELTA * (1.0 - adjustmentRatio);

        if (!Double.isFinite(rawWinDelta)) {
            return BanPickRatingRules.BASE_WIN_DELTA;
        }

        int roundedWinDelta = (int) Math.round(rawWinDelta);
        return Math.max(BanPickRatingRules.MIN_WIN_DELTA, roundedWinDelta);
    }

    private int safeRating(Integer rating) {
        return rating != null ? Math.max(BanPickRatingRules.MIN_RATING, rating) : BanPickRatingRules.INITIAL_RATING;
    }

    public record MacroEconomySnapshot(
            LocalDate snapshotDate,
            int activePlayerCount,
            int activePoolSize,
            Double averageRating,
            int winDelta
    ) {
        private static MacroEconomySnapshot fallback(LocalDate snapshotDate, int activePlayerCount) {
            return new MacroEconomySnapshot(
                    snapshotDate,
                    activePlayerCount,
                    0,
                    null,
                    BanPickRatingRules.BASE_WIN_DELTA
            );
        }
    }
}
