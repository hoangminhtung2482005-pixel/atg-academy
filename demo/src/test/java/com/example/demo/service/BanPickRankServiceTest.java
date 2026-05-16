package com.example.demo.service;

import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BanPickRankServiceTest {

    private final BanPickRankService service = new BanPickRankService();

    @Test
    void resolveRanksMapsFiveDistinctRatingsAcrossAllBuckets() {
        PlayerStats rankD = statsRow(1L, 10, 1000);
        PlayerStats rankC = statsRow(2L, 10, 1100);
        PlayerStats rankB = statsRow(3L, 10, 1200);
        PlayerStats rankA = statsRow(4L, 10, 1300);
        PlayerStats rankS = statsRow(5L, 10, 1400);

        Map<Long, BanPickRankService.RankProfile> ranks = service.resolveRanks(List.of(rankD, rankC, rankB, rankA, rankS));

        assertThat(ranks.get(1L).code()).isEqualTo("D");
        assertThat(ranks.get(2L).code()).isEqualTo("C");
        assertThat(ranks.get(3L).code()).isEqualTo("B");
        assertThat(ranks.get(4L).code()).isEqualTo("A");
        assertThat(ranks.get(5L).code()).isEqualTo("S");
        assertThat(ranks.get(5L).label()).isEqualTo("Rank S");
    }

    @Test
    void resolveRanksKeepsTieRatingsOnSameStableBucket() {
        PlayerStats bottomTieOne = statsRow(1L, 10, 1000);
        PlayerStats bottomTieTwo = statsRow(2L, 10, 1000);
        PlayerStats middle = statsRow(3L, 10, 1200);
        PlayerStats high = statsRow(4L, 10, 1300);
        PlayerStats top = statsRow(5L, 10, 1400);

        Map<Long, BanPickRankService.RankProfile> ranks = service.resolveRanks(
                List.of(bottomTieOne, bottomTieTwo, middle, high, top)
        );

        assertThat(ranks.get(1L).code()).isEqualTo("D");
        assertThat(ranks.get(2L).code()).isEqualTo("D");
        assertThat(ranks.get(1L).percentile()).isEqualTo(ranks.get(2L).percentile());
        assertThat(ranks.get(5L).code()).isEqualTo("S");
    }

    @Test
    void resolveRankFallsBackSafelyForSingleRankedPlayerAndUnrankedPlayer() {
        PlayerStats onlyRankedPlayer = statsRow(1L, 8, 1150);
        PlayerStats newPlayer = statsRow(2L, 0, 1000);

        BanPickRankService.RankProfile onlyRankedProfile = service.resolveRank(onlyRankedPlayer, List.of(onlyRankedPlayer));
        BanPickRankService.RankProfile unrankedProfile = service.resolveRank(newPlayer, List.of(onlyRankedPlayer));

        assertThat(onlyRankedProfile.code()).isEqualTo("D");
        assertThat(onlyRankedProfile.label()).isEqualTo("Rank D");
        assertThat(unrankedProfile.code()).isEqualTo("UNRANKED");
        assertThat(unrankedProfile.label()).isEqualTo("Unranked");
    }

    private static PlayerStats statsRow(Long userId, int totalMatches, int rating) {
        User user = new User();
        user.setId(userId);
        user.setEmail("rank-" + userId + "@example.com");

        PlayerStats stats = new PlayerStats();
        stats.setUser(user);
        stats.setTotalMatches(totalMatches);
        stats.setWins(Math.max(0, totalMatches / 2));
        stats.setLosses(Math.max(0, totalMatches - stats.getWins()));
        stats.setRating(rating);
        return stats;
    }
}
