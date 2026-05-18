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
    private final BanPickRatingSettingsAccessor settingsAccessor;

    @Autowired
    public BanPickMacroEconomyService(DraftHistoryRepository draftHistoryRepository,
                                      PlayerStatsRepository playerStatsRepository,
                                      BanPickRatingSettingsAccessor settingsAccessor) {
        this(draftHistoryRepository, playerStatsRepository, Clock.systemDefaultZone(), settingsAccessor);
    }

    BanPickMacroEconomyService(DraftHistoryRepository draftHistoryRepository,
                               PlayerStatsRepository playerStatsRepository,
                               Clock clock) {
        this(draftHistoryRepository, playerStatsRepository, clock, BanPickRatingSettingsSnapshot::defaults);
    }

    BanPickMacroEconomyService(DraftHistoryRepository draftHistoryRepository,
                               PlayerStatsRepository playerStatsRepository,
                               Clock clock,
                               BanPickRatingSettingsAccessor settingsAccessor) {
        this.draftHistoryRepository = draftHistoryRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.clock = clock;
        this.settingsAccessor = settingsAccessor;
    }

    @Transactional(readOnly = true)
    public MacroEconomySnapshot getCurrentSnapshot() {
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        LocalDate snapshotDate = LocalDate.now(clock);
        LocalDateTime windowEnd = snapshotDate.atStartOfDay();
        LocalDateTime windowStart = windowEnd.minusDays(settings.macroActiveWindowDays());

        List<DraftHistory> completedHistories = draftHistoryRepository.findCompletedBetween(windowStart, windowEnd);
        Map<Long, Integer> completedMatchesByUserId = countCompletedMatchesByUser(completedHistories);
        if (completedMatchesByUserId.size() < settings.macroMinimumActivePlayers()) {
            return MacroEconomySnapshot.fallback(snapshotDate, completedMatchesByUserId.size(), settings.baseWinDelta());
        }

        List<Map.Entry<Long, Integer>> sortedPlayers = completedMatchesByUserId.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();

        int basePoolSize = Math.max(1, (int) Math.ceil(sortedPlayers.size() * (settings.macroActiveTopPercent() / 100.0)));
        int cutoffMatchCount = sortedPlayers.get(basePoolSize - 1).getValue();
        List<Long> activePoolUserIds = sortedPlayers.stream()
                .filter(entry -> entry.getValue() >= cutoffMatchCount)
                .map(Map.Entry::getKey)
                .toList();

        if (activePoolUserIds.isEmpty()) {
            return MacroEconomySnapshot.fallback(snapshotDate, completedMatchesByUserId.size(), settings.baseWinDelta());
        }

        Map<Long, Integer> ratingsByUserId = currentRatingsByUserId(settings);
        double averageRating = activePoolUserIds.stream()
                .mapToInt(userId -> ratingsByUserId.getOrDefault(userId, settings.initialRating()))
                .average()
                .orElse(settings.macroBalanceRating());

        return new MacroEconomySnapshot(
                snapshotDate,
                completedMatchesByUserId.size(),
                activePoolUserIds.size(),
                averageRating,
                resolveWinDelta(settings, averageRating)
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

    private Map<Long, Integer> currentRatingsByUserId(BanPickRatingSettingsSnapshot settings) {
        Map<Long, Integer> ratingsByUserId = new LinkedHashMap<>();
        for (PlayerStats stats : playerStatsRepository.findAll()) {
            if (stats == null || stats.getUser() == null || stats.getUser().getId() == null) {
                continue;
            }
            ratingsByUserId.put(stats.getUser().getId(), safeRating(stats.getRating(), settings.initialRating()));
        }
        return ratingsByUserId;
    }

    private int resolveWinDelta(BanPickRatingSettingsSnapshot settings, double averageRating) {
        if (!settings.macroEnabled()) {
            return settings.baseWinDelta();
        }

        double difference = averageRating - settings.macroBalanceRating();
        double adjustmentRatio = (difference / settings.macroRatingStep())
                * settings.macroWinAdjustmentPerStep();
        double rawWinDelta = settings.baseWinDelta() * (1.0 - adjustmentRatio);

        if (!Double.isFinite(rawWinDelta)) {
            return settings.baseWinDelta();
        }

        int roundedWinDelta = (int) Math.round(rawWinDelta);
        return Math.max(settings.macroMinWinDelta(), roundedWinDelta);
    }

    private int safeRating(Integer rating, int fallbackRating) {
        return rating != null ? Math.max(0, rating) : fallbackRating;
    }

    public record MacroEconomySnapshot(
            LocalDate snapshotDate,
            int activePlayerCount,
            int activePoolSize,
            Double averageRating,
            int winDelta
    ) {
        private static MacroEconomySnapshot fallback(LocalDate snapshotDate, int activePlayerCount, int baseWinDelta) {
            return new MacroEconomySnapshot(
                    snapshotDate,
                    activePlayerCount,
                    0,
                    null,
                    baseWinDelta
            );
        }
    }
}
