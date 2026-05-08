package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsMatchDraftAction;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(EsportsDataService.class)
class EsportsDataServiceIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EsportsDataService esportsDataService;

    @BeforeEach
    void setUp() {
        EsportsTeam blueTeam = persistTeam("FS", "RPL");
        EsportsTeam redTeam = persistTeam("SGP", "AOG");

        Hero heroA = persistHero("Hero A");
        Hero heroB = persistHero("Hero B");
        Hero heroC = persistHero("Hero C");
        Hero heroD = persistHero("Hero D");
        Hero heroE = persistHero("Hero E");
        Hero heroF = persistHero("Hero F");
        Hero heroG = persistHero("Hero G");
        Hero heroPickOnly = persistHero("Hero Pick Only");
        Hero heroStatA = persistHero("Hero Stat A");
        Hero heroStatB = persistHero("Hero Stat B");
        Hero heroStatBanOnly = persistHero("Hero Stat Ban Only");
        Hero heroStatBanSupport = persistHero("Hero Stat Ban Support");

        EsportsMatch proLeagueMatch = persistMatch("1", LocalDateTime.of(2026, 5, 1, 10, 0), blueTeam, redTeam);
        seedGame(proLeagueMatch, 1, blueTeam, redTeam, heroB, heroA, heroPickOnly, heroD);
        seedGame(proLeagueMatch, 2, blueTeam, redTeam, heroB, heroA, heroPickOnly, heroC);
        seedGame(proLeagueMatch, 3, blueTeam, redTeam, heroB, heroA, heroPickOnly, heroF);
        seedGame(proLeagueMatch, 4, blueTeam, redTeam, heroD, heroA, heroPickOnly, heroF);
        seedGame(proLeagueMatch, 5, blueTeam, redTeam, heroE, heroC, heroPickOnly, heroD);
        seedGame(proLeagueMatch, 6, blueTeam, redTeam, heroF, heroC, heroPickOnly, heroD);

        EsportsMatch internationalMatch = persistMatch("0", LocalDateTime.of(2026, 5, 2, 10, 0), blueTeam, redTeam);
        seedGame(internationalMatch, 1, blueTeam, redTeam, heroF, heroG, heroA, heroB);

        EsportsMatch challengerMatch = persistMatch("2", LocalDateTime.of(2026, 5, 3, 10, 0), blueTeam, redTeam);
        seedGame(challengerMatch, 1, blueTeam, redTeam, blueTeam, heroStatBanOnly, heroStatBanSupport, heroStatA, heroStatB);
        seedGame(challengerMatch, 2, blueTeam, redTeam, redTeam, heroStatBanOnly, heroG, heroStatA, heroStatB);
        seedGame(challengerMatch, 3, blueTeam, redTeam, blueTeam, heroStatBanOnly, heroF, heroStatB, heroStatA);

        EsportsMatch dashboardMatch = persistMatch("DASHBOARD_FULL_2026", LocalDateTime.of(2026, 5, 4, 12, 0), blueTeam, redTeam);
        seedFullDraftGame(
                dashboardMatch,
                1,
                blueTeam,
                redTeam,
                blueTeam,
                persistHero("Dashboard Ban Blue"),
                persistHero("Dashboard Ban Red"),
                List.of(
                        persistHero("Dashboard Blue 1", 9.0),
                        persistHero("Dashboard Blue 2", 8.5),
                        persistHero("Dashboard Blue 3", 8.0),
                        persistHero("Dashboard Blue 4", 7.5),
                        persistHero("Dashboard Blue 5", 7.0)
                ),
                List.of(
                        persistHero("Dashboard Red 1", 6.0),
                        persistHero("Dashboard Red 2", 5.5),
                        persistHero("Dashboard Red 3", 5.0),
                        persistHero("Dashboard Red 4", 4.5),
                        persistHero("Dashboard Red 5", 4.0)
                )
        );

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getTopBannedHeroesMatchesDeterministicDraftCounts() {
        List<EsportsHeroBanStatResponse> result = esportsDataService.getTopBannedHeroes("AER Pro League", 10);

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName, EsportsHeroBanStatResponse::banCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Hero A", 4L),
                        org.assertj.core.groups.Tuple.tuple("Hero B", 3L),
                        org.assertj.core.groups.Tuple.tuple("Hero C", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero D", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero E", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero F", 1L)
                );

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName)
                .doesNotContain("Hero Pick Only");
        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::tournamentName)
                .containsOnly("AER Pro League");
    }

    @Test
    void getTopBlueBannedHeroesCountsOnlyBlueBanActions() {
        List<EsportsHeroBanStatResponse> result = esportsDataService.getTopBlueBannedHeroes("AER Pro League", 10);

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName, EsportsHeroBanStatResponse::banCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Hero B", 3L),
                        org.assertj.core.groups.Tuple.tuple("Hero D", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero E", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero F", 1L)
                );

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName)
                .doesNotContain("Hero A", "Hero C", "Hero G", "Hero Pick Only");
    }

    @Test
    void getTopBannedHeroesHonorsLimitFive() {
        List<EsportsHeroBanStatResponse> result = esportsDataService.getTopBannedHeroes("AER Pro League", 5);

        assertThat(result).hasSize(5);
        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName)
                .containsExactly("Hero A", "Hero B", "Hero C", "Hero D", "Hero E");
    }

    @Test
    void getTopBannedHeroesFiltersByTournamentName() {
        List<EsportsHeroBanStatResponse> result = esportsDataService.getTopBannedHeroes("AER International", 10);

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName, EsportsHeroBanStatResponse::banCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Hero F", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero G", 1L)
                );
        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::tournamentName)
                .containsOnly("AER International");
    }

    @Test
    void getHeroStatsUsesPickAndBanAggregatesWithoutDoubleCounting() {
        List<EsportsHeroStatResponse> result = esportsDataService.getHeroStats("AER Challenger");

        assertThat(result)
                .extracting(
                        EsportsHeroStatResponse::heroName,
                        EsportsHeroStatResponse::pickCount,
                        EsportsHeroStatResponse::pickWins,
                        EsportsHeroStatResponse::pickLosses,
                        EsportsHeroStatResponse::bluePickCount,
                        EsportsHeroStatResponse::blueWins,
                        EsportsHeroStatResponse::blueLosses,
                        EsportsHeroStatResponse::redPickCount,
                        EsportsHeroStatResponse::redWins,
                        EsportsHeroStatResponse::redLosses,
                        EsportsHeroStatResponse::banCount,
                        EsportsHeroStatResponse::blueBanCount,
                        EsportsHeroStatResponse::redBanCount,
                        EsportsHeroStatResponse::presenceCount
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Hero Stat A", 3L, 1L, 2L, 2L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 3L),
                        org.assertj.core.groups.Tuple.tuple("Hero Stat B", 3L, 2L, 1L, 1L, 1L, 0L, 2L, 1L, 1L, 0L, 0L, 0L, 3L),
                        org.assertj.core.groups.Tuple.tuple("Hero Stat Ban Only", 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 3L, 3L, 0L, 3L),
                        org.assertj.core.groups.Tuple.tuple("Hero F", 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero G", 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero Stat Ban Support", 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 1L)
                );

        assertThat(result)
                .extracting(EsportsHeroStatResponse::pickWinRate)
                .containsExactly(33.333333333333336D, 66.66666666666667D, 0D, 0D, 0D, 0D);
        assertThat(result)
                .extracting(EsportsHeroStatResponse::blueWinRate)
                .containsExactly(50.0D, 100.0D, 0D, 0D, 0D, 0D);
        assertThat(result)
                .extracting(EsportsHeroStatResponse::redWinRate)
                .containsExactly(0D, 50.0D, 0D, 0D, 0D, 0D);

        assertThat(result).allSatisfy(item ->
                assertThat(item.banCount()).isEqualTo(item.blueBanCount() + item.redBanCount()));
    }

    @Test
    void getDashboardBuildsOverviewAndInsightsFromFilteredGames() {
        EsportsDashboardResponse dashboard = esportsDataService.getDashboard("AER Challenger", null, null, null);

        assertThat(dashboard.summary().totalMatches()).isEqualTo(1L);
        assertThat(dashboard.summary().totalGames()).isEqualTo(3L);
        assertThat(dashboard.summary().metaChampions()).isEqualTo(6L);
        assertThat(dashboard.summary().completedGames()).isEqualTo(3L);
        assertThat(dashboard.summary().blueSideWinRate()).isEqualTo(66.66666666666667D);
        assertThat(dashboard.summary().draftAccuracy()).isNull();
        assertThat(dashboard.summary().draftAccuracySampleSize()).isEqualTo(0L);

        assertThat(dashboard.matchActivity()).containsExactly(
                new EsportsDashboardResponse.ActivityPoint(
                        java.time.LocalDate.of(2026, 5, 3),
                        1L,
                        3L
                )
        );

        assertThat(dashboard.sideAdvantage()).isEqualTo(
                new EsportsDashboardResponse.SideAdvantage(
                        2L,
                        1L,
                        3L,
                        66.66666666666667D,
                        33.333333333333336D
                )
        );

        assertThat(dashboard.powerPicks())
                .extracting(EsportsDashboardResponse.HeroInsight::heroName)
                .containsExactly("Hero Stat B", "Hero Stat A");
        assertThat(dashboard.trapPicks())
                .extracting(EsportsDashboardResponse.HeroInsight::heroName)
                .containsExactly("Hero Stat A", "Hero Stat B");
        assertThat(dashboard.topTeams())
                .extracting(EsportsDashboardResponse.TeamInsight::teamCode, EsportsDashboardResponse.TeamInsight::winRate)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("FS", 66.66666666666667D),
                        org.assertj.core.groups.Tuple.tuple("SGP", 33.333333333333336D)
                );
        assertThat(dashboard.topBannedHeroes().get(0).heroName()).isEqualTo("Hero Stat Ban Only");
        assertThat(dashboard.topBlueBannedHero().heroName()).isEqualTo("Hero Stat Ban Only");
        assertThat(dashboard.teamOptions())
                .extracting(EsportsDashboardResponse.TeamOption::teamCode)
                .containsExactly("FS", "SGP");
    }

    @Test
    void getDashboardCalculatesDraftAccuracyWhenFullPickScoresExist() {
        EsportsDashboardResponse dashboard = esportsDataService.getDashboard("DASHBOARD_FULL_2026", null, null, null);

        assertThat(dashboard.summary().totalMatches()).isEqualTo(1L);
        assertThat(dashboard.summary().totalGames()).isEqualTo(1L);
        assertThat(dashboard.summary().draftAccuracy()).isEqualTo(100.0D);
        assertThat(dashboard.summary().draftAccuracySampleSize()).isEqualTo(1L);
        assertThat(dashboard.sideAdvantage().blueWins()).isEqualTo(1L);
        assertThat(dashboard.sideAdvantage().redWins()).isEqualTo(0L);
    }

    private void seedGame(EsportsMatch match,
                          int gameNumber,
                          EsportsTeam blueTeam,
                          EsportsTeam redTeam,
                          Hero blueBanHero,
                          Hero redBanHero,
                          Hero bluePickHero,
                          Hero redPickHero) {
        seedGame(match, gameNumber, blueTeam, redTeam, null, blueBanHero, redBanHero, bluePickHero, redPickHero);
    }

    private void seedGame(EsportsMatch match,
                          int gameNumber,
                          EsportsTeam blueTeam,
                          EsportsTeam redTeam,
                          EsportsTeam winnerTeam,
                          Hero blueBanHero,
                          Hero redBanHero,
                          Hero bluePickHero,
                          Hero redPickHero) {
        EsportsMatchGame game = new EsportsMatchGame();
        game.setMatch(match);
        game.setGameNumber(gameNumber);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setWinnerTeam(winnerTeam);
        entityManager.persist(game);

        persistDraftAction(game, blueTeam, blueBanHero, BanPickActionType.BAN, 1, BanPickTeamSide.BLUE);
        persistDraftAction(game, redTeam, redBanHero, BanPickActionType.BAN, 2, BanPickTeamSide.RED);
        persistDraftAction(game, blueTeam, bluePickHero, BanPickActionType.PICK, 3, BanPickTeamSide.BLUE);
        persistDraftAction(game, redTeam, redPickHero, BanPickActionType.PICK, 4, BanPickTeamSide.RED);
    }

    private void persistDraftAction(EsportsMatchGame game,
                                    EsportsTeam team,
                                    Hero hero,
                                    BanPickActionType actionType,
                                    int stepNumber,
                                    BanPickTeamSide teamSide) {
        EsportsMatchDraftAction action = new EsportsMatchDraftAction();
        action.setGame(game);
        action.setTeam(team);
        action.setHero(hero);
        action.setActionType(actionType);
        action.setStepNumber(stepNumber);
        action.setTeamSide(teamSide);
        entityManager.persist(action);
    }

    private void seedFullDraftGame(EsportsMatch match,
                                   int gameNumber,
                                   EsportsTeam blueTeam,
                                   EsportsTeam redTeam,
                                   EsportsTeam winnerTeam,
                                   Hero blueBanHero,
                                   Hero redBanHero,
                                   List<Hero> bluePickHeroes,
                                   List<Hero> redPickHeroes) {
        EsportsMatchGame game = new EsportsMatchGame();
        game.setMatch(match);
        game.setGameNumber(gameNumber);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setWinnerTeam(winnerTeam);
        entityManager.persist(game);

        int stepNumber = 1;
        persistDraftAction(game, blueTeam, blueBanHero, BanPickActionType.BAN, stepNumber++, BanPickTeamSide.BLUE);
        persistDraftAction(game, redTeam, redBanHero, BanPickActionType.BAN, stepNumber++, BanPickTeamSide.RED);

        for (Hero hero : bluePickHeroes) {
            persistDraftAction(game, blueTeam, hero, BanPickActionType.PICK, stepNumber++, BanPickTeamSide.BLUE);
        }
        for (Hero hero : redPickHeroes) {
            persistDraftAction(game, redTeam, hero, BanPickActionType.PICK, stepNumber++, BanPickTeamSide.RED);
        }
    }

    private EsportsMatch persistMatch(String tier,
                                      LocalDateTime matchDate,
                                      EsportsTeam team1,
                                      EsportsTeam team2) {
        EsportsMatch match = new EsportsMatch();
        match.setMatchDate(matchDate);
        match.setTeam1Code(team1.getTeamCode());
        match.setTeam2Code(team2.getTeamCode());
        match.setScore1(3);
        match.setScore2(2);
        match.setTier(tier);
        match.setStage("bang");
        entityManager.persist(match);
        return match;
    }

    private EsportsTeam persistTeam(String teamCode, String region) {
        EsportsTeam team = new EsportsTeam();
        team.setTeamCode(teamCode);
        team.setTeamName(teamCode + " Team");
        team.setLogoUrl("/images/teams/" + teamCode + ".png");
        team.setRegion(region);
        team.setScore(1200.0);
        team.setGameWins(0);
        team.setGameLosses(0);
        team.setMatchWins(0);
        team.setMatchLosses(0);
        entityManager.persist(team);
        return team;
    }

    private Hero persistHero(String name) {
        Hero hero = new Hero();
        hero.setName(name);
        hero.setAvatarUrl("/images/heroes/" + name.replace(' ', '-') + ".jpg");
        entityManager.persist(hero);
        return hero;
    }

    private Hero persistHero(String name, double banPickScore) {
        Hero hero = persistHero(name);
        hero.setBanPickScore(BigDecimal.valueOf(banPickScore));
        return hero;
    }
}
