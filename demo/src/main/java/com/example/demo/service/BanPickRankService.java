package com.example.demo.service;

import com.example.demo.entity.PlayerStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BanPickRankService {

    private static final RankProfile UNRANKED = new RankProfile("UNRANKED", "Unranked", 0.0);
    private static final RankProfile RANK_S = new RankProfile("S", "Rank S", 80.0);
    private static final RankProfile RANK_A = new RankProfile("A", "Rank A", 60.0);
    private static final RankProfile RANK_B = new RankProfile("B", "Rank B", 40.0);
    private static final RankProfile RANK_C = new RankProfile("C", "Rank C", 20.0);
    private static final RankProfile RANK_D = new RankProfile("D", "Rank D", 0.0);
    private final BanPickRatingSettingsAccessor settingsAccessor;

    public BanPickRankService() {
        this(BanPickRatingSettingsSnapshot::defaults);
    }

    @Autowired
    public BanPickRankService(BanPickRatingSettingsAccessor settingsAccessor) {
        this.settingsAccessor = settingsAccessor;
    }

    public RankProfile resolveRank(PlayerStats targetStats, List<PlayerStats> allStats) {
        if (!isRankEligible(targetStats)) {
            return UNRANKED;
        }

        Long targetUserId = userIdOf(targetStats);
        if (targetUserId == null) {
            return RANK_D;
        }

        Map<Long, PlayerStats> comparableStatsByUserId = normalizeComparableStats(allStats);
        comparableStatsByUserId.put(targetUserId, targetStats);

        return resolveRanks(new ArrayList<>(comparableStatsByUserId.values()))
                .getOrDefault(targetUserId, RANK_D);
    }

    public Map<Long, RankProfile> resolveRanks(List<PlayerStats> allStats) {
        Map<Long, PlayerStats> rankedStatsByUserId = normalizeComparableStats(allStats);
        if (rankedStatsByUserId.isEmpty()) {
            return Map.of();
        }

        List<RankedEntry> rankedEntries = rankedStatsByUserId.values().stream()
                .map(stats -> new RankedEntry(userIdOf(stats), safeRating(stats.getRating())))
                .sorted((left, right) -> Integer.compare(left.rating(), right.rating()))
                .toList();

        Map<Integer, RankProfile> rankProfilesByRating = new LinkedHashMap<>();
        int totalPlayers = rankedEntries.size();
        int index = 0;
        while (index < rankedEntries.size()) {
            int startIndex = index;
            int rating = rankedEntries.get(index).rating();
            while (index + 1 < rankedEntries.size() && rankedEntries.get(index + 1).rating() == rating) {
                index += 1;
            }
            int endIndex = index;
            double percentile = calculatePercentile(startIndex, endIndex, totalPlayers);
            rankProfilesByRating.put(rating, toRankProfile(percentile));
            index += 1;
        }

        Map<Long, RankProfile> ranksByUserId = new LinkedHashMap<>();
        for (RankedEntry entry : rankedEntries) {
            ranksByUserId.put(entry.userId(), rankProfilesByRating.getOrDefault(entry.rating(), RANK_D));
        }
        return ranksByUserId;
    }

    public RankProfile unranked() {
        return UNRANKED;
    }

    private Map<Long, PlayerStats> normalizeComparableStats(List<PlayerStats> allStats) {
        Map<Long, PlayerStats> comparableStatsByUserId = new LinkedHashMap<>();
        if (allStats == null) {
            return comparableStatsByUserId;
        }

        for (PlayerStats stats : allStats) {
            if (!isRankEligible(stats)) {
                continue;
            }
            Long userId = userIdOf(stats);
            if (userId != null) {
                comparableStatsByUserId.put(userId, stats);
            }
        }
        return comparableStatsByUserId;
    }

    private boolean isRankEligible(PlayerStats stats) {
        return userIdOf(stats) != null && safeInt(stats.getTotalMatches()) > 0;
    }

    private Long userIdOf(PlayerStats stats) {
        if (stats == null || stats.getUser() == null) {
            return null;
        }
        return stats.getUser().getId();
    }

    private double calculatePercentile(int startIndex, int endIndex, int totalPlayers) {
        if (totalPlayers <= 1) {
            return 0.0;
        }
        double groupMidpoint = (startIndex + endIndex) / 2.0;
        return (groupMidpoint * 100.0) / (totalPlayers - 1);
    }

    private RankProfile toRankProfile(double percentile) {
        if (percentile >= 80.0) {
            return new RankProfile(RANK_S.code(), RANK_S.label(), percentile);
        }
        if (percentile >= 60.0) {
            return new RankProfile(RANK_A.code(), RANK_A.label(), percentile);
        }
        if (percentile >= 40.0) {
            return new RankProfile(RANK_B.code(), RANK_B.label(), percentile);
        }
        if (percentile >= 20.0) {
            return new RankProfile(RANK_C.code(), RANK_C.label(), percentile);
        }
        return new RankProfile(RANK_D.code(), RANK_D.label(), percentile);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int safeRating(Integer value) {
        return value != null ? Math.max(0, value) : settingsAccessor.getCurrentSettings().initialRating();
    }

    private record RankedEntry(
            Long userId,
            int rating
    ) {
    }

    public record RankProfile(
            String code,
            String label,
            double percentile
    ) {
    }
}
