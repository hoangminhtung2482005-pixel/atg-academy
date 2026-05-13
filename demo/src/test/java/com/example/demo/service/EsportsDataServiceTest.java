package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsDraftTournamentScopeAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.EsportsGameDraft;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsDataServiceTest {

        @Mock
        private EsportsGameDraftRepository esportsGameDraftRepository;

        @Mock
        private HeroRepository heroRepository;

        @Mock
        private EsportsTournamentRepository esportsTournamentRepository;

        @Test
        void getAvailableTournamentsMapsUniqueLatestDraftScopes() {
                when(esportsGameDraftRepository.findDraftTournamentScopesOrderByLatestMatchDesc()).thenReturn(List.of(
                                new EsportsDraftTournamentScopeAggregate(null, null, "2", null, LocalDateTime.of(2026, 5, 7, 10, 0)),
                                new EsportsDraftTournamentScopeAggregate(null, null, "0", null, LocalDateTime.of(2026, 5, 6, 10, 0)),
                                new EsportsDraftTournamentScopeAggregate(null, null, "2", null, LocalDateTime.of(2026, 5, 5, 10, 0))));

                List<EsportsTournamentOptionResponse> result = service().getAvailableTournaments();

                assertThat(result).containsExactly(
                                new EsportsTournamentOptionResponse(null, "AER Challenger", "2", null, true),
                                new EsportsTournamentOptionResponse(null, "AER International", "0", null, true));
        }

        @Test
        void getTopBlueBannedHeroesUsesFlatGameDraftsAndSanitizesLimit() {
                EsportsGameDraft draftA = draft("0", 1L, 2L);
                draftA.setBlueBan1HeroId(7L);
                draftA.setBlueBan2HeroId(9L);
                draftA.setRedBan1HeroId(11L);

                EsportsGameDraft draftB = draft("0", 1L, 2L);
                draftB.setBlueBan1HeroId(7L);
                draftB.setBlueBan2HeroId(7L);
                draftB.setRedBan1HeroId(11L);

                when(esportsGameDraftRepository.findAllForAnalyticsScope(null, "0", null, null, null))
                                .thenReturn(List.of(draftA, draftB));
                when(heroRepository.findAllById(any())).thenReturn(List.of(
                                hero(7L, "Hayate"),
                                hero(9L, "Aya"),
                                hero(11L, "Toro")));

                List<EsportsHeroBanStatResponse> result = service().getTopBlueBannedHeroes("AER International", 99);

                verify(esportsGameDraftRepository).findAllForAnalyticsScope(null, "0", null, null, null);
                assertThat(result).containsExactly(
                                new EsportsHeroBanStatResponse(7L, "Hayate", "/images/heroes/Hayate.jpg", 3L,
                                                "AER International"),
                                new EsportsHeroBanStatResponse(9L, "Aya", "/images/heroes/Aya.jpg", 1L,
                                                "AER International"));
        }

        @Test
        void getHeroStatsAcceptsExistingRawTournamentTierFromDraftCatalog() {
                when(esportsTournamentRepository.findByNameIgnoreCase("TEST_OTHER_DRAFT_2026")).thenReturn(java.util.Optional.empty());
                when(esportsTournamentRepository.findBySlugIgnoreCase("TEST_OTHER_DRAFT_2026")).thenReturn(java.util.Optional.empty());
                when(esportsGameDraftRepository.findDraftTournamentScopesOrderByLatestMatchDesc()).thenReturn(List.of(
                                new EsportsDraftTournamentScopeAggregate(
                                                null,
                                                null,
                                                "TEST_OTHER_DRAFT_2026",
                                                null,
                                                LocalDateTime.of(2026, 5, 8, 10, 0))));
                when(esportsGameDraftRepository.findAllForAnalyticsScope(null, "TEST_OTHER_DRAFT_2026", null, null, null))
                                .thenReturn(List.of());

                List<EsportsHeroStatResponse> result = service().getHeroStats("TEST_OTHER_DRAFT_2026");

                assertThat(result).isEmpty();
                verify(esportsGameDraftRepository).findAllForAnalyticsScope(null, "TEST_OTHER_DRAFT_2026", null, null, null);
        }

        @Test
        void getTopBannedHeroesRejectsUnknownTournamentName() {
                when(esportsTournamentRepository.findByNameIgnoreCase("Unknown League")).thenReturn(java.util.Optional.empty());
                when(esportsTournamentRepository.findBySlugIgnoreCase("Unknown League")).thenReturn(java.util.Optional.empty());
                assertThatThrownBy(() -> service().getTopBannedHeroes("Unknown League", 5))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("tournamentName khong hop le");
        }

        @Test
        void getDashboardRejectsInvalidDateRange() {
                assertThatThrownBy(() -> service().getDashboard(
                                null,
                                null,
                                LocalDate.of(2026, 5, 9),
                                LocalDate.of(2026, 5, 1)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("date range khong hop le");
        }

        @Test
        void getDashboardBuildsTeamOptionsFromFilteredDrafts() {
                EsportsGameDraft draft = draft("1", 1L, 2L);
                draft.setWinnerTeam(draft.getBlueTeam());
                draft.setBlueBan1HeroId(1L);
                draft.setRedBan1HeroId(2L);
                draft.setBlueDslHeroId(3L);
                draft.setBlueJglHeroId(4L);
                draft.setBlueMidHeroId(5L);
                draft.setBlueAdlHeroId(6L);
                draft.setBlueSupHeroId(7L);
                draft.setRedDslHeroId(8L);
                draft.setRedJglHeroId(9L);
                draft.setRedMidHeroId(10L);
                draft.setRedAdlHeroId(11L);
                draft.setRedSupHeroId(12L);

                when(esportsGameDraftRepository.findAllForAnalyticsScope(null, "1", "FS", null, null))
                                .thenReturn(List.of(draft));
                when(esportsGameDraftRepository.findAllForAnalyticsScope(null, "1", null, null, null))
                                .thenReturn(List.of(draft));
                when(heroRepository.findAllById(any())).thenReturn(List.of(
                                hero(1L, "Hero 1"),
                                hero(2L, "Hero 2"),
                                hero(3L, "Hero 3"),
                                hero(4L, "Hero 4"),
                                hero(5L, "Hero 5"),
                                hero(6L, "Hero 6"),
                                hero(7L, "Hero 7"),
                                hero(8L, "Hero 8"),
                                hero(9L, "Hero 9"),
                                hero(10L, "Hero 10"),
                                hero(11L, "Hero 11"),
                                hero(12L, "Hero 12")));

                EsportsDashboardResponse result = service().getDashboard("AER Pro League", "FS", null, null);

                assertThat(result.summary().totalMatches()).isEqualTo(1L);
                assertThat(result.summary().totalGames()).isEqualTo(1L);
                assertThat(result.sideAdvantage().blueWins()).isEqualTo(1L);
                assertThat(result.topBlueBannedHero().heroName()).isEqualTo("Hero 1");
                assertThat(result.teamOptions())
                                .extracting(EsportsDashboardResponse.TeamOption::teamCode)
                                .containsExactly("FS", "SGP");
        }

        private EsportsDataService service() {
                return new EsportsDataService(esportsGameDraftRepository, esportsTournamentRepository, heroRepository);
        }

        private EsportsGameDraft draft(String tier, Long blueTeamId, Long redTeamId) {
                EsportsTeam blueTeam = team(blueTeamId, "FS");
                EsportsTeam redTeam = team(redTeamId, "SGP");

                EsportsMatch match = new EsportsMatch();
                match.setId(90L);
                match.setTier(tier);
                match.setStage("bang");
                match.setMatchDate(LocalDateTime.of(2026, 5, 9, 10, 0));
                match.setTeam1Code(blueTeam.getTeamCode());
                match.setTeam2Code(redTeam.getTeamCode());

                EsportsGameDraft draft = new EsportsGameDraft();
                draft.setId(100L + blueTeamId + redTeamId);
                draft.setMatch(match);
                draft.setGameNumber(1);
                draft.setBlueTeam(blueTeam);
                draft.setRedTeam(redTeam);
                draft.setDraftFormatCode("AOV_STANDARD_18");
                draft.setSource("manual");
                return draft;
        }

        private EsportsTeam team(Long id, String teamCode) {
                EsportsTeam team = new EsportsTeam();
                team.setId(id);
                team.setTeamCode(teamCode);
                team.setTeamName(teamCode + " Team");
                team.setLogoUrl("/images/teams/" + teamCode + ".png");
                return team;
        }

        private Hero hero(Long id, String name) {
                Hero hero = new Hero();
                hero.setId(id);
                hero.setName(name);
                hero.setAvatarUrl("/images/heroes/" + name + ".jpg");
                hero.setBanPickScore(BigDecimal.valueOf(5));
                return hero;
        }
}
