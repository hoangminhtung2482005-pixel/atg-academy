package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.EsportsGameDraft;
import com.example.demo.entity.EsportsFranchise;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.entity.Hero;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

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
        EsportsTeam fs = persistTeam("FS", "Flash Wolves");
        EsportsTeam sgp = persistTeam("SGP", "Saigon Phantom");

        Hero heroA = persistHero("Hero A");
        Hero heroB = persistHero("Hero B");
        Hero heroC = persistHero("Hero C");
        Hero heroD = persistHero("Hero D");
        Hero heroE = persistHero("Hero E");
        Hero heroF = persistHero("Hero F");
        Hero heroG = persistHero("Hero G");
        Hero heroH = persistHero("Hero H");
        Hero heroI = persistHero("Hero I");
        Hero heroJ = persistHero("Hero J");
        Hero heroK = persistHero("Hero K");
        Hero heroL = persistHero("Hero L");
        Hero heroM = persistHero("Hero M");
        Hero heroN = persistHero("Hero N");
        Hero heroO = persistHero("Hero O");
        Hero heroP = persistHero("Hero P");
        Hero heroQ = persistHero("Hero Q");
        Hero heroR = persistHero("Hero R");
        Hero heroS = persistHero("Hero S");
        Hero heroT = persistHero("Hero T");
        Hero heroU = persistHero("Hero U");
        Hero heroV = persistHero("Hero V");
        Hero heroW = persistHero("Hero W");
        Hero heroX = persistHero("Hero X");

        EsportsMatch proLeagueMatch = persistMatch("1", LocalDateTime.of(2026, 5, 1, 10, 0), fs, sgp);
        persistDraft(
                proLeagueMatch,
                1,
                fs,
                sgp,
                fs,
                List.of(heroA, heroB, heroC, heroD, heroE),
                List.of(heroF, heroG, heroH, heroI, heroJ),
                List.of(heroK, heroL, heroM, heroN, heroO),
                List.of(heroP, heroQ, heroR, heroS, heroT)
        );
        persistDraft(
                proLeagueMatch,
                2,
                fs,
                sgp,
                sgp,
                List.of(heroA, heroC, heroE, heroG, heroI),
                List.of(heroF, heroH, heroJ, heroL, heroN),
                List.of(heroK, heroL, heroM, heroN, heroO),
                List.of(heroP, heroU, heroV, heroW, heroX)
        );

        EsportsMatch challengerMatch = persistMatch("2", LocalDateTime.of(2026, 5, 2, 10, 0), fs, sgp);
        persistDraft(
                challengerMatch,
                1,
                fs,
                sgp,
                fs,
                List.of(heroB, heroD, heroF, heroH, heroJ),
                List.of(heroA, heroC, heroE, heroG, heroI),
                List.of(heroK, heroL, heroM, heroN, heroO),
                List.of(heroP, heroQ, heroR, heroS, heroT)
        );

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getTopBannedHeroesReadsUnionedBanColumnsFromFlatTable() {
        List<EsportsHeroBanStatResponse> result = esportsDataService.getTopBannedHeroes("AER Pro League", 10);

        assertThat(result)
                .extracting(EsportsHeroBanStatResponse::heroName, EsportsHeroBanStatResponse::banCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Hero A", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero C", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero E", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero F", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero G", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero H", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero I", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero J", 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero B", 1L),
                        org.assertj.core.groups.Tuple.tuple("Hero D", 1L)
                );
    }

    @Test
    void getHeroStatsUsesLineupSlotsForPickWinsAndPresence() {
        List<EsportsHeroStatResponse> result = esportsDataService.getHeroStats("AER Pro League");

        assertThat(result)
                .extracting(
                        EsportsHeroStatResponse::heroName,
                        EsportsHeroStatResponse::pickCount,
                        EsportsHeroStatResponse::pickWins,
                        EsportsHeroStatResponse::pickLosses,
                        EsportsHeroStatResponse::banCount,
                        EsportsHeroStatResponse::presenceCount
                )
                .contains(
                        org.assertj.core.groups.Tuple.tuple("Hero K", 2L, 1L, 1L, 0L, 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero P", 2L, 1L, 1L, 0L, 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero A", 0L, 0L, 0L, 2L, 2L),
                        org.assertj.core.groups.Tuple.tuple("Hero U", 1L, 1L, 0L, 0L, 1L)
                );
    }

    @Test
    void getDashboardBuildsSummaryAndBlueSideBanLeaderFromGameDrafts() {
        EsportsDashboardResponse dashboard = esportsDataService.getDashboard("AER Pro League", null, null, null);

        assertThat(dashboard.summary().totalMatches()).isEqualTo(1L);
        assertThat(dashboard.summary().totalGames()).isEqualTo(2L);
        assertThat(dashboard.summary().completedGames()).isEqualTo(2L);
        assertThat(dashboard.summary().blueSideWinRate()).isEqualTo(50.0D);
        assertThat(dashboard.summary().draftAccuracy()).isNull();
        assertThat(dashboard.sideAdvantage().blueWins()).isEqualTo(1L);
        assertThat(dashboard.sideAdvantage().redWins()).isEqualTo(1L);
        assertThat(dashboard.topBannedHeroes().get(0).heroName()).isEqualTo("Hero A");
        assertThat(dashboard.topBlueBannedHero().heroName()).isEqualTo("Hero A");
        assertThat(dashboard.teamOptions())
                .extracting(EsportsDashboardResponse.TeamOption::teamCode)
                .containsExactly("FS", "SGP");
    }

    @Test
    void getAvailableTournamentsIncludesOfficialTournamentAerTier() {
        EsportsTeam fs = persistTeam("ONE", "One Team");
        EsportsTeam sgp = persistTeam("BRO", "Bro Team");
        Hero heroA = persistHero("Hero Official A");
        Hero heroB = persistHero("Hero Official B");
        Hero heroC = persistHero("Hero Official C");
        Hero heroD = persistHero("Hero Official D");
        Hero heroE = persistHero("Hero Official E");
        Hero heroF = persistHero("Hero Official F");
        Hero heroG = persistHero("Hero Official G");
        Hero heroH = persistHero("Hero Official H");
        Hero heroI = persistHero("Hero Official I");
        Hero heroJ = persistHero("Hero Official J");
        Hero heroK = persistHero("Hero Official K");
        Hero heroL = persistHero("Hero Official L");
        Hero heroM = persistHero("Hero Official M");
        Hero heroN = persistHero("Hero Official N");
        Hero heroO = persistHero("Hero Official O");
        Hero heroP = persistHero("Hero Official P");
        Hero heroQ = persistHero("Hero Official Q");
        Hero heroR = persistHero("Hero Official R");
        Hero heroS = persistHero("Hero Official S");
        Hero heroT = persistHero("Hero Official T");

        EsportsFranchise franchise = persistFranchise("AOG", "Arena Of Glory", "T1", 20);
        EsportsTournament tournament = persistTournament(franchise, "AOG Spring 2026", "aog-spring-2026", "T1", 2);
        EsportsMatch officialMatch = persistMatch("1", LocalDateTime.of(2026, 5, 3, 10, 0), fs, sgp, tournament);
        persistDraft(
                officialMatch,
                1,
                fs,
                sgp,
                fs,
                List.of(heroA, heroB, heroC, heroD, heroE),
                List.of(heroF, heroG, heroH, heroI, heroJ),
                List.of(heroK, heroL, heroM, heroN, heroO),
                List.of(heroP, heroQ, heroR, heroS, heroT)
        );

        entityManager.flush();
        entityManager.clear();

        List<EsportsTournamentOptionResponse> options = esportsDataService.getAvailableTournaments();

        assertThat(options)
                .anySatisfy(option -> {
                    assertThat(option.tournamentId()).isEqualTo(tournament.getId());
                    assertThat(option.tournamentName()).isEqualTo("AOG Spring 2026");
                    assertThat(option.tournamentTier()).isEqualTo("2");
                    assertThat(option.franchiseCode()).isEqualTo("AOG");
                    assertThat(option.legacyScope()).isFalse();
                });
    }

    private void persistDraft(EsportsMatch match,
                              int gameNumber,
                              EsportsTeam blueTeam,
                              EsportsTeam redTeam,
                              EsportsTeam winnerTeam,
                              List<Hero> blueBans,
                              List<Hero> redBans,
                              List<Hero> blueLineup,
                              List<Hero> redLineup) {
        EsportsGameDraft draft = new EsportsGameDraft();
        draft.setMatch(match);
        draft.setGameNumber(gameNumber);
        draft.setBlueTeam(blueTeam);
        draft.setRedTeam(redTeam);
        draft.setWinnerTeam(winnerTeam);
        draft.setDurationSeconds(1200 + gameNumber);
        draft.setDraftFormatCode("AOV_STANDARD_18");
        draft.setSource("integration-test");

        draft.setBlueBan1HeroId(blueBans.get(0).getId());
        draft.setBlueBan2HeroId(blueBans.get(1).getId());
        draft.setBlueBan3HeroId(blueBans.get(2).getId());
        draft.setBlueBan4HeroId(blueBans.get(3).getId());
        draft.setBlueBan5HeroId(blueBans.get(4).getId());
        draft.setRedBan1HeroId(redBans.get(0).getId());
        draft.setRedBan2HeroId(redBans.get(1).getId());
        draft.setRedBan3HeroId(redBans.get(2).getId());
        draft.setRedBan4HeroId(redBans.get(3).getId());
        draft.setRedBan5HeroId(redBans.get(4).getId());

        draft.setBlueDslHeroId(blueLineup.get(0).getId());
        draft.setBlueJglHeroId(blueLineup.get(1).getId());
        draft.setBlueMidHeroId(blueLineup.get(2).getId());
        draft.setBlueAdlHeroId(blueLineup.get(3).getId());
        draft.setBlueSupHeroId(blueLineup.get(4).getId());
        draft.setRedDslHeroId(redLineup.get(0).getId());
        draft.setRedJglHeroId(redLineup.get(1).getId());
        draft.setRedMidHeroId(redLineup.get(2).getId());
        draft.setRedAdlHeroId(redLineup.get(3).getId());
        draft.setRedSupHeroId(redLineup.get(4).getId());

        entityManager.persist(draft);
    }

    private EsportsMatch persistMatch(String tier,
                                      LocalDateTime matchDate,
                                      EsportsTeam team1,
                                      EsportsTeam team2) {
        return persistMatch(tier, matchDate, team1, team2, null);
    }

    private EsportsMatch persistMatch(String tier,
                                      LocalDateTime matchDate,
                                      EsportsTeam team1,
                                      EsportsTeam team2,
                                      EsportsTournament tournament) {
        EsportsMatch match = new EsportsMatch();
        match.setMatchDate(matchDate);
        match.setTeam1Code(team1.getTeamCode());
        match.setTeam2Code(team2.getTeamCode());
        match.setScore1(3);
        match.setScore2(2);
        match.setTier(tier);
        match.setStage("bang");
        match.setTournament(tournament);
        entityManager.persist(match);
        return match;
    }

    private EsportsFranchise persistFranchise(String code, String name, String tierLevel, int displayOrder) {
        EsportsFranchise franchise = new EsportsFranchise();
        franchise.setCode(code);
        franchise.setName(name);
        franchise.setTierLevel(tierLevel);
        franchise.setDisplayOrder(displayOrder);
        franchise.setActive(Boolean.TRUE);
        entityManager.persist(franchise);
        return franchise;
    }

    private EsportsTournament persistTournament(EsportsFranchise franchise,
                                                String name,
                                                String slug,
                                                String tierLevel,
                                                int aerTier) {
        EsportsTournament tournament = new EsportsTournament();
        tournament.setFranchise(franchise);
        tournament.setName(name);
        tournament.setSlug(slug);
        tournament.setTierLevel(tierLevel);
        tournament.setAerTier(aerTier);
        tournament.setSeasonYear(2026);
        tournament.setSplitName("Spring");
        tournament.setStatus("ONGOING");
        entityManager.persist(tournament);
        return tournament;
    }

    private EsportsTeam persistTeam(String teamCode, String teamName) {
        EsportsTeam team = new EsportsTeam();
        team.setTeamCode(teamCode);
        team.setTeamName(teamName);
        team.setLogoUrl("/images/teams/" + teamCode + ".png");
        team.setRegion("AOV");
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
}
