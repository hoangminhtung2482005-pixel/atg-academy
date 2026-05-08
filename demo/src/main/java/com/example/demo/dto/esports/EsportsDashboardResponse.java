package com.example.demo.dto.esports;

import java.time.LocalDate;
import java.util.List;

public record EsportsDashboardResponse(
        Summary summary,
        List<ActivityPoint> matchActivity,
        SideAdvantage sideAdvantage,
        List<HeroInsight> powerPicks,
        List<HeroInsight> trapPicks,
        List<TeamInsight> topTeams,
        List<EsportsHeroStatResponse> heroStats,
        List<EsportsHeroBanStatResponse> topBannedHeroes,
        EsportsHeroBanStatResponse topBlueBannedHero,
        List<TeamOption> teamOptions
) {

    public record Summary(
            Long totalMatches,
            Long totalGames,
            Long metaChampions,
            Double blueSideWinRate,
            Double draftAccuracy,
            Long completedGames,
            Long draftAccuracySampleSize
    ) {
    }

    public record ActivityPoint(
            LocalDate activityDate,
            Long matchCount,
            Long gameCount
    ) {
    }

    public record SideAdvantage(
            Long blueWins,
            Long redWins,
            Long completedGames,
            Double blueWinRate,
            Double redWinRate
    ) {
    }

    public record HeroInsight(
            Long heroId,
            String heroName,
            String heroAvatarUrl,
            Long pickCount,
            Long pickWins,
            Long pickLosses,
            Double pickWinRate
    ) {
    }

    public record TeamInsight(
            Long teamId,
            String teamCode,
            String teamName,
            String logoUrl,
            Long wins,
            Long losses,
            Long gamesPlayed,
            Double winRate
    ) {
    }

    public record TeamOption(
            String teamCode,
            String teamName,
            String logoUrl
    ) {
    }
}
