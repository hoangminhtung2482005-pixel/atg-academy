package com.example.demo.service;

import com.example.demo.dto.esports.EsportsGameDraftRequest;
import com.example.demo.dto.esports.EsportsGameDraftResponse;
import com.example.demo.entity.EsportsGameDraft;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsDraftServiceTest {

    @Mock
    private EsportsMatchRepository esportsMatchRepository;

    @Mock
    private EsportsTeamRepository esportsTeamRepository;

    @Mock
    private HeroRepository heroRepository;

    @Mock
    private EsportsTournamentRepository esportsTournamentRepository;

    @Mock
    private EsportsGameDraftRepository esportsGameDraftRepository;

    @Mock
    private EloCalculationService eloCalculationService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void createGameDraftRejectsTeamsOutsideMatchParticipants() {
        EsportsMatch match = match(10L, "FS", "SGP");
        EsportsTeam fs = team(1L, "FS");
        EsportsTeam kog = team(2L, "KOG");

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(kog));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createGameDraft(10L, new EsportsGameDraftRequest(
                1,
                1L,
                2L,
                null,
                900,
                "AOV_STANDARD_18",
                "manual",
                List.of(),
                List.of(),
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải thuộc đúng 2 đội của esports match");

        verify(esportsGameDraftRepository, never()).save(any());
    }

    @Test
    void createGameDraftRejectsDuplicateGameNumberWithinMatch() {
        EsportsMatch match = match(10L, "FS", "SGP");
        EsportsTeam fs = team(1L, "FS");
        EsportsTeam sgp = team(2L, "SGP");

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(true);

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createGameDraft(10L, requestWith(
                1,
                1L,
                2L,
                1L,
                101,
                102,
                201,
                202
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gameNumber đã tồn tại");
    }

    @Test
    void createGameDraftRejectsDuplicateHeroAcrossBanAndLineup() {
        EsportsMatch match = match(10L, "FS", "SGP");
        EsportsTeam fs = team(1L, "FS");
        EsportsTeam sgp = team(2L, "SGP");
        Hero duplicateHero = hero(101L, "Hayate");

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(false);
        when(heroRepository.findAllById(any())).thenReturn(List.of(duplicateHero));

        EsportsDraftService service = service();

        EsportsGameDraftRequest request = new EsportsGameDraftRequest(
                1,
                1L,
                2L,
                1L,
                1200,
                "AOV_STANDARD_18",
                "manual",
                List.of(101L),
                List.of(),
                new EsportsGameDraftRequest.LineupRequest(101L, null, null, null, null),
                null
        );

        assertThatThrownBy(() -> service.createGameDraft(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không được trùng hero");

        verify(esportsGameDraftRepository, never()).save(any());
    }

    @Test
    void createGameDraftMapsPayloadToFlatRecordAndCompleteness() {
        EsportsMatch match = match(10L, "FS", "SGP");
        EsportsTeam fs = team(1L, "FS");
        EsportsTeam sgp = team(2L, "SGP");
        List<Hero> heroes = heroes(101L, 120L);

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(false);
        when(heroRepository.findAllById(any())).thenReturn(heroes);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"test\"}");
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> {
            EsportsGameDraft saved = invocation.getArgument(0);
            saved.setId(77L);
            saved.setCreatedAt(LocalDateTime.of(2026, 5, 12, 22, 0));
            saved.setUpdatedAt(LocalDateTime.of(2026, 5, 12, 22, 5));
            return saved;
        });

        EsportsDraftService service = service();
        EsportsGameDraftRequest request = new EsportsGameDraftRequest(
                1,
                1L,
                2L,
                1L,
                1191,
                "AOV_STANDARD_18",
                "manual",
                List.of(101L, 102L, 103L, 104L, 105L),
                List.of(106L, 107L, 108L, 109L, 110L),
                new EsportsGameDraftRequest.LineupRequest(111L, 112L, 113L, 114L, 115L),
                new EsportsGameDraftRequest.LineupRequest(116L, 117L, 118L, 119L, 120L)
        );

        EsportsGameDraftResponse response = service.createGameDraft(10L, request);
        ArgumentCaptor<EsportsGameDraft> captor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsGameDraftRepository).save(captor.capture());

        EsportsGameDraft savedDraft = captor.getValue();
        assertThat(savedDraft.getMatch()).isSameAs(match);
        assertThat(savedDraft.getBlueTeam()).isSameAs(fs);
        assertThat(savedDraft.getRedTeam()).isSameAs(sgp);
        assertThat(savedDraft.getWinnerTeam()).isSameAs(fs);
        assertThat(savedDraft.getBlueBan1HeroId()).isEqualTo(101L);
        assertThat(savedDraft.getRedBan5HeroId()).isEqualTo(110L);
        assertThat(savedDraft.getBlueDslHeroId()).isEqualTo(111L);
        assertThat(savedDraft.getRedSupHeroId()).isEqualTo(120L);
        assertThat(savedDraft.getRawDraftJson()).contains("test");

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.durationText()).isEqualTo("19:51");
        assertThat(response.draftCompleteness().complete()).isTrue();
        assertThat(response.draftCompleteness().status()).isEqualTo("Complete");
        assertThat(response.draftCompleteness().banCount()).isEqualTo(10);
        assertThat(response.draftCompleteness().pickCount()).isEqualTo(10);
        assertThat(response.blueLineup().get("DSL").name()).isEqualTo("Hero 111");
        assertThat(response.redLineup().get("SUP").name()).isEqualTo("Hero 120");
    }

    @Test
    void updateGameDraftRejectsWinnerOutsideSides() {
        EsportsMatch match = match(10L, "FS", "SGP");
        EsportsTeam fs = team(1L, "FS");
        EsportsTeam sgp = team(2L, "SGP");
        EsportsTeam kog = team(3L, "KOG");

        EsportsGameDraft existingDraft = new EsportsGameDraft();
        existingDraft.setId(5L);
        existingDraft.setMatch(match);
        existingDraft.setGameNumber(1);
        existingDraft.setBlueTeam(fs);
        existingDraft.setRedTeam(sgp);

        when(esportsGameDraftRepository.findById(5L)).thenReturn(Optional.of(existingDraft));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(esportsTeamRepository.findById(3L)).thenReturn(Optional.of(kog));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.updateGameDraft(5L, new EsportsGameDraftRequest(
                1,
                1L,
                2L,
                3L,
                1000,
                "AOV_STANDARD_18",
                "manual",
                List.of(),
                List.of(),
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("winnerTeamId phải là blue team hoặc red team");
    }

    @Test
    void exportGameDraftsCsvBuildsExpectedHeaderAndRowMapping() {
        EsportsMatch match = match(10L, "FS", "SGP");
        match.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));
        match.setTier("0");

        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");

        EsportsGameDraft draft = exportDraft(match, 2, fs, sgp, fs);
        List<Hero> heroes = List.of(
                hero(101L, "Florentino"),
                hero(102L, "Ryoma"),
                hero(103L, "Yena"),
                hero(104L, "Richter"),
                hero(105L, "Maloch"),
                hero(106L, "Aya"),
                hero(107L, "Zip"),
                hero(108L, "Helen"),
                hero(109L, "Cresht"),
                hero(110L, "Toro"),
                hero(111L, "Omen"),
                hero(112L, "Nakroth"),
                hero(113L, "Liliana"),
                hero(114L, "Hayate"),
                hero(115L, "Alice"),
                hero(116L, "Veres"),
                hero(117L, "Aoi"),
                hero(118L, "Krixi"),
                hero(119L, "Violet"),
                hero(120L, "Rouie")
        );

        when(esportsGameDraftRepository.findAllForExportScope(null, null, null, null, null)).thenReturn(List.of(draft));
        when(heroRepository.findAllById(any())).thenReturn(heroes);

        EsportsDraftService service = service();

        String csv = new String(service.exportGameDraftsCsv(null, null, null, null), StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r\\n");

        assertThat(lines[0]).isEqualTo("\uFEFFDate,Tournament,Match,Team_1,T1_Side,T1_DSL,T1_JGL,T1_MID,T1_ADL,T1_SUP,T1_Ban_1,T1_Ban_2,T1_Ban_3,T1_Ban_4,T1_Ban_5,Team_2,T2_Side,T2_DSL,T2_JGL,T2_MID,T2_ADL,T2_SUP,T2_Ban_1,T2_Ban_2,T2_Ban_3,T2_Ban_4,T2_Ban_5,Winner,Length");
        assertThat(lines[1]).isEqualTo("2026-05-11,AER International,2,Flash Wolves,Blue,Omen,Nakroth,Liliana,Hayate,Alice,Florentino,Ryoma,Yena,Richter,Maloch,Saigon Phantom,Red,Veres,Aoi,Krixi,Violet,Rouie,Aya,Zip,Helen,Cresht,Toro,Flash Wolves,19:15");
    }

    @Test
    void exportGameDraftsCsvEscapesQuotesAndCommas() {
        EsportsMatch match = match(10L, "FS", "SGP");
        match.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));
        match.setTier("1");

        EsportsTeam blueTeam = team(1L, "FS");
        blueTeam.setTeamName("Team \"A\", Alpha");
        EsportsTeam redTeam = team(2L, "SGP");
        redTeam.setTeamName("Saigon Phantom");

        EsportsGameDraft draft = exportDraft(match, 1, blueTeam, redTeam, redTeam);
        draft.setBlueDslHeroId(111L);
        draft.setBlueJglHeroId(null);
        draft.setBlueMidHeroId(null);
        draft.setBlueAdlHeroId(null);
        draft.setBlueSupHeroId(null);
        draft.setBlueBan1HeroId(101L);
        draft.setBlueBan2HeroId(null);
        draft.setBlueBan3HeroId(null);
        draft.setBlueBan4HeroId(null);
        draft.setBlueBan5HeroId(null);
        draft.setRedDslHeroId(null);
        draft.setRedJglHeroId(null);
        draft.setRedMidHeroId(null);
        draft.setRedAdlHeroId(null);
        draft.setRedSupHeroId(null);
        draft.setRedBan1HeroId(102L);
        draft.setRedBan2HeroId(null);
        draft.setRedBan3HeroId(null);
        draft.setRedBan4HeroId(null);
        draft.setRedBan5HeroId(null);

        when(esportsGameDraftRepository.findAllForExportScope(null, null, null, null, null)).thenReturn(List.of(draft));
        when(heroRepository.findAllById(any())).thenReturn(List.of(
                hero(101L, "The Flash, \"Prime\""),
                hero(102L, "Y'bneth"),
                hero(111L, "Florentino")
        ));

        EsportsDraftService service = service();

        String csv = new String(service.exportGameDraftsCsv(null, null, null, null), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Team \"\"A\"\", Alpha\"");
        assertThat(csv).contains("\"The Flash, \"\"Prime\"\"\"");
    }

    @Test
    void exportGameDraftsCsvAllowsMissingWinnerAndDuration() {
        EsportsMatch match = match(10L, "FS", "SGP");
        match.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));
        match.setTier("1");

        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");

        EsportsGameDraft draft = exportDraft(match, 1, fs, sgp, null);
        draft.setDurationSeconds(null);

        when(esportsGameDraftRepository.findAllForExportScope(null, null, null, null, null)).thenReturn(List.of(draft));
        when(heroRepository.findAllById(any())).thenReturn(heroes(101L, 120L));

        EsportsDraftService service = service();

        String csv = new String(service.exportGameDraftsCsv(null, null, null, null), StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r\\n");

        assertThat(lines[1]).contains("Flash Wolves");
        assertThat(lines[1]).endsWith(",,");
    }

    @Test
    void exportGameDraftsCsvUsesMatchIdFilterWhenProvided() {
        EsportsMatch match = match(10L, "FS", "SGP");
        match.setMatchDate(LocalDateTime.of(2026, 5, 12, 20, 0));

        EsportsTeam fs = team(1L, "FS");
        EsportsTeam sgp = team(2L, "SGP");
        EsportsGameDraft draft = exportDraft(match, 1, fs, sgp, null);

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsGameDraftRepository.findAllForExportScope(null, null, 10L, null, null)).thenReturn(List.of(draft));
        when(heroRepository.findAllById(any())).thenReturn(heroes(101L, 120L));

        EsportsDraftService service = service();

        String csv = new String(service.exportGameDraftsCsv(null, 10L, null, null), StandardCharsets.UTF_8);

        verify(esportsGameDraftRepository).findAllForExportScope(null, null, 10L, null, null);
        assertThat(csv).contains("2026-05-12");
        assertThat(csv).contains(",1,");
    }

    @Test
    void previewGameDraftImportFlagsUnknownTournament() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of());
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of());

        EsportsDraftService service = service();
        var response = service.previewGameDraftImport(csvFile(csvBody("Missing Tournament", "Flash Wolves")), false);

        assertThat(response.readyToImport()).isFalse();
        assertThat(response.summary().errorRows()).isEqualTo(1);
        assertThat(response.rows()).singleElement().satisfies(row -> {
            assertThat(row.errors()).anyMatch(message -> message.contains("Tournament"));
        });
    }

    @Test
    void previewGameDraftImportRejectsDuplicateWhenOverwriteDisabled() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");
        EsportsMatch existingMatch = match(10L, "FS", "SGP");
        existingMatch.setTournament(tournament);
        existingMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of(existingMatch));
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(true);

        EsportsDraftService service = service();
        var response = service.previewGameDraftImport(csvFile(csvBody("AOG Spring 2026", "Flash Wolves")), false);

        assertThat(response.readyToImport()).isFalse();
        assertThat(response.summary().draftsToOverwrite()).isZero();
        assertThat(response.rows()).singleElement().satisfies(row -> {
            assertThat(row.matchId()).isEqualTo(10L);
            assertThat(row.errors()).anyMatch(message -> message.contains("Bật overwrite"));
        });
    }

    @Test
    void previewAndConfirmImportGroupsThreeGamesIntoSingleSeriesMatch() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of());
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> {
            EsportsMatch saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(
                csvFile(seriesCsvBody("AOG Spring 2026", "playoff", List.of("Flash Wolves", "Flash Wolves", "Flash Wolves"))),
                false
        );

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().matchesToCreate()).isEqualTo(1);
        assertThat(preview.summary().draftsToCreate()).isEqualTo(3);
        assertThat(preview.rows()).hasSize(3).allSatisfy(row -> {
            assertThat(row.matchId()).isNull();
            assertThat(row.matchAction()).isEqualTo("Tao match moi");
        });

        var confirm = service.confirmGameDraftImport(
                new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken())
        );

        ArgumentCaptor<EsportsMatch> matchCaptor = ArgumentCaptor.forClass(EsportsMatch.class);
        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsMatchRepository).save(matchCaptor.capture());
        verify(esportsGameDraftRepository, times(3)).save(draftCaptor.capture());
        verify(eloCalculationService).calculateAllRankings();

        EsportsMatch savedMatch = matchCaptor.getValue();
        assertThat(savedMatch.getTeam1Code()).isEqualTo("FS");
        assertThat(savedMatch.getTeam2Code()).isEqualTo("SGP");
        assertThat(savedMatch.getStage()).isEqualTo("playoff");
        assertThat(savedMatch.getScore1()).isEqualTo(3);
        assertThat(savedMatch.getScore2()).isZero();
        assertThat(savedMatch.getTournament()).isSameAs(tournament);

        assertThat(draftCaptor.getAllValues())
                .extracting(EsportsGameDraft::getGameNumber)
                .containsExactly(1, 2, 3);
        assertThat(draftCaptor.getAllValues()).allSatisfy(draft -> {
            assertThat(draft.getMatch()).isSameAs(savedMatch);
            assertThat(draft.getBlueTeam()).isSameAs(fs);
            assertThat(draft.getRedTeam()).isSameAs(sgp);
            assertThat(draft.getWinnerTeam()).isSameAs(fs);
        });

        assertThat(confirm.importedRows()).isEqualTo(3);
        assertThat(confirm.createdMatches()).isEqualTo(1);
        assertThat(confirm.updatedMatches()).isZero();
        assertThat(confirm.createdDrafts()).isEqualTo(3);
        assertThat(confirm.overwrittenDrafts()).isZero();
        assertThat(confirm.affectedMatchIds()).containsExactly(99L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isTrue();
    }

    @Test
    void previewAndConfirmImportAttachesThreeGamesToExistingSeriesParent() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");
        EsportsMatch existingMatch = match(10L, "FS", "SGP");
        existingMatch.setTournament(tournament);
        existingMatch.setStage("playoff");
        existingMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));
        existingMatch.setScore1(0);
        existingMatch.setScore2(0);

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of(existingMatch));
        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(existingMatch));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(
                csvFile(seriesCsvBody("AOG Spring 2026", "playoff", List.of("Flash Wolves", "Saigon Phantom", "Flash Wolves"))),
                false
        );

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().matchesToCreate()).isZero();
        assertThat(preview.summary().matchesToUpdate()).isEqualTo(1);
        assertThat(preview.summary().draftsToCreate()).isEqualTo(3);
        assertThat(preview.rows()).hasSize(3).allSatisfy(row -> {
            assertThat(row.matchId()).isEqualTo(10L);
            assertThat(row.matchAction().toLowerCase()).contains("match #10");
        });

        var confirm = service.confirmGameDraftImport(
                new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken())
        );

        ArgumentCaptor<EsportsMatch> matchCaptor = ArgumentCaptor.forClass(EsportsMatch.class);
        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsMatchRepository).save(matchCaptor.capture());
        verify(esportsGameDraftRepository, times(3)).save(draftCaptor.capture());
        verify(eloCalculationService).calculateAllRankings();

        EsportsMatch savedMatch = matchCaptor.getValue();
        assertThat(savedMatch.getId()).isEqualTo(10L);
        assertThat(savedMatch.getScore1()).isEqualTo(2);
        assertThat(savedMatch.getScore2()).isEqualTo(1);
        assertThat(savedMatch.getStage()).isEqualTo("playoff");
        assertThat(draftCaptor.getAllValues())
                .extracting(draft -> draft.getMatch().getId(), EsportsGameDraft::getGameNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 1),
                        org.assertj.core.groups.Tuple.tuple(10L, 2),
                        org.assertj.core.groups.Tuple.tuple(10L, 3)
                );

        assertThat(confirm.importedRows()).isEqualTo(3);
        assertThat(confirm.createdMatches()).isZero();
        assertThat(confirm.updatedMatches()).isEqualTo(1);
        assertThat(confirm.createdDrafts()).isEqualTo(3);
        assertThat(confirm.overwrittenDrafts()).isZero();
        assertThat(confirm.affectedMatchIds()).containsExactly(10L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isTrue();
    }

    @Test
    void previewAndConfirmImportReusesCanonicalExactSeriesParent() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");

        EsportsMatch canonicalMatch = match(10L, "FS", "SGP");
        canonicalMatch.setTournament(tournament);
        canonicalMatch.setStage("playoff");
        canonicalMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 8, 45));
        canonicalMatch.setScore1(2);
        canonicalMatch.setScore2(1);

        EsportsMatch duplicateExactMatch = match(11L, "FS", "SGP");
        duplicateExactMatch.setTournament(tournament);
        duplicateExactMatch.setStage("playoff");
        duplicateExactMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 21, 0));
        duplicateExactMatch.setScore1(2);
        duplicateExactMatch.setScore2(1);

        EsportsGameDraft existingDraft = new EsportsGameDraft();
        existingDraft.setId(501L);
        existingDraft.setMatch(canonicalMatch);
        existingDraft.setGameNumber(1);
        existingDraft.setBlueTeam(fs);
        existingDraft.setRedTeam(sgp);
        existingDraft.setWinnerTeam(fs);

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of(canonicalMatch, duplicateExactMatch));
        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(canonicalMatch));
        when(esportsGameDraftRepository.findById(501L)).thenReturn(Optional.of(existingDraft));
        when(esportsGameDraftRepository.findByMatchId(10L)).thenReturn(List.of(existingDraft));
        when(esportsGameDraftRepository.findByMatchId(11L)).thenReturn(List.of());
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(true);
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(
                csvFile(seriesCsvBody("AOG Spring 2026", "playoff", List.of("Flash Wolves", "Saigon Phantom", "Flash Wolves"))),
                true
        );

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().matchesToCreate()).isZero();
        assertThat(preview.summary().matchesToUpdate()).isZero();
        assertThat(preview.summary().draftsToCreate()).isEqualTo(2);
        assertThat(preview.summary().draftsToOverwrite()).isEqualTo(1);
        assertThat(preview.rows()).hasSize(3).allSatisfy(row -> {
            assertThat(row.matchId()).isEqualTo(10L);
            assertThat(row.matchAction()).isEqualTo("Dùng match #10");
        });
        assertThat(preview.rows()).anySatisfy(row ->
                assertThat(row.warnings()).anyMatch(message -> message.contains("nhiều esports_matches exact"))
        );
        assertThat(preview.rows().get(0).draftAction()).contains("Overwrite game draft #501");

        var confirm = service.confirmGameDraftImport(
                new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken())
        );

        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsMatchRepository, never()).save(any());
        verify(esportsGameDraftRepository, times(3)).save(draftCaptor.capture());
        verify(eloCalculationService, never()).calculateAllRankings();

        assertThat(draftCaptor.getAllValues())
                .extracting(draft -> draft.getMatch().getId(), EsportsGameDraft::getGameNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 1),
                        org.assertj.core.groups.Tuple.tuple(10L, 2),
                        org.assertj.core.groups.Tuple.tuple(10L, 3)
                );
        assertThat(draftCaptor.getAllValues().get(0).getId()).isEqualTo(501L);

        assertThat(confirm.importedRows()).isEqualTo(3);
        assertThat(confirm.createdMatches()).isZero();
        assertThat(confirm.updatedMatches()).isZero();
        assertThat(confirm.createdDrafts()).isEqualTo(2);
        assertThat(confirm.overwrittenDrafts()).isEqualTo(1);
        assertThat(confirm.affectedMatchIds()).containsExactly(10L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isFalse();
    }

    @Test
    void previewAndConfirmImportReusesExistingSeriesParentWhenTeamOrderIsReversed() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");

        EsportsMatch existingMatch = match(10L, "SGP", "FS");
        existingMatch.setTournament(tournament);
        existingMatch.setStage("playoff");
        existingMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 22, 15));
        existingMatch.setScore1(1);
        existingMatch.setScore2(2);

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of(existingMatch));
        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(existingMatch));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(
                csvFile(seriesCsvBody("AOG Spring 2026", "playoff", List.of("Flash Wolves", "Saigon Phantom", "Flash Wolves"))),
                false
        );

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().matchesToCreate()).isZero();
        assertThat(preview.summary().matchesToUpdate()).isZero();
        assertThat(preview.summary().draftsToCreate()).isEqualTo(3);
        assertThat(preview.summary().draftsToOverwrite()).isZero();
        assertThat(preview.rows()).hasSize(3).allSatisfy(row -> {
            assertThat(row.matchId()).isEqualTo(10L);
            assertThat(row.matchAction()).isEqualTo("Dùng match #10");
        });

        var confirm = service.confirmGameDraftImport(
                new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken())
        );

        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsMatchRepository, never()).save(any());
        verify(esportsGameDraftRepository, times(3)).save(draftCaptor.capture());
        verify(eloCalculationService, never()).calculateAllRankings();

        assertThat(draftCaptor.getAllValues())
                .extracting(draft -> draft.getMatch().getId(), EsportsGameDraft::getGameNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 1),
                        org.assertj.core.groups.Tuple.tuple(10L, 2),
                        org.assertj.core.groups.Tuple.tuple(10L, 3)
                );

        assertThat(confirm.importedRows()).isEqualTo(3);
        assertThat(confirm.createdMatches()).isZero();
        assertThat(confirm.updatedMatches()).isZero();
        assertThat(confirm.createdDrafts()).isEqualTo(3);
        assertThat(confirm.overwrittenDrafts()).isZero();
        assertThat(confirm.affectedMatchIds()).containsExactly(10L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isFalse();
    }

    @Test
    void previewAndConfirmImportOverwritesExistingDraftWhenEnabled() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");
        EsportsMatch existingMatch = match(10L, "FS", "SGP");
        existingMatch.setTournament(tournament);
        existingMatch.setStage("bang");
        existingMatch.setMatchDate(LocalDateTime.of(2026, 5, 11, 18, 30));
        existingMatch.setScore1(1);
        existingMatch.setScore2(0);

        EsportsGameDraft existingDraft = new EsportsGameDraft();
        existingDraft.setId(501L);
        existingDraft.setMatch(existingMatch);
        existingDraft.setGameNumber(1);
        existingDraft.setBlueTeam(fs);
        existingDraft.setRedTeam(sgp);
        existingDraft.setWinnerTeam(fs);

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of(existingMatch));
        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(existingMatch));
        when(esportsGameDraftRepository.findById(501L)).thenReturn(Optional.of(existingDraft));
        when(esportsGameDraftRepository.findByMatchId(10L)).thenReturn(List.of(existingDraft));
        when(esportsGameDraftRepository.existsByMatchIdAndGameNumber(10L, 1)).thenReturn(true);
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(
                csvFile(seriesCsvBody("AOG Spring 2026", "bang", List.of("Flash Wolves")).replace(",19:15\r\n", ",18:00\r\n")),
                true
        );

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().draftsToOverwrite()).isEqualTo(1);
        assertThat(preview.rows()).singleElement().satisfies(row -> {
            assertThat(row.matchId()).isEqualTo(10L);
            assertThat(row.draftAction()).contains("Overwrite game draft #501");
        });

        var confirm = service.confirmGameDraftImport(
                new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken())
        );

        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsGameDraftRepository).save(draftCaptor.capture());
        verify(eloCalculationService, never()).calculateAllRankings();

        EsportsGameDraft savedDraft = draftCaptor.getValue();
        assertThat(savedDraft.getId()).isEqualTo(501L);
        assertThat(savedDraft.getGameNumber()).isEqualTo(1);
        assertThat(savedDraft.getDurationSeconds()).isEqualTo(1080);
        assertThat(savedDraft.getWinnerTeam()).isSameAs(fs);

        assertThat(confirm.importedRows()).isEqualTo(1);
        assertThat(confirm.createdMatches()).isZero();
        assertThat(confirm.updatedMatches()).isZero();
        assertThat(confirm.createdDrafts()).isZero();
        assertThat(confirm.overwrittenDrafts()).isEqualTo(1);
        assertThat(confirm.affectedMatchIds()).containsExactly(10L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isFalse();
    }

    @Test
    void previewAndConfirmImportCreatesMatchAndDraft() {
        EsportsTeam fs = team(1L, "FS");
        fs.setTeamName("Flash Wolves");
        EsportsTeam sgp = team(2L, "SGP");
        sgp.setTeamName("Saigon Phantom");
        EsportsTournament tournament = tournament(7L, "AOG Spring 2026", "aog-spring-2026", "T1");
        tournament.setAerTier(2);

        when(esportsTeamRepository.findAll()).thenReturn(List.of(fs, sgp));
        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(importHeroes());
        when(heroRepository.findAllById(any())).thenReturn(importHeroes());
        when(esportsTournamentRepository.findAll()).thenReturn(List.of(tournament));
        when(esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(List.of());
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(sgp));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"source\":\"import\"}");
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> {
            EsportsMatch saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(esportsGameDraftRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsDraftService service = service();
        var preview = service.previewGameDraftImport(csvFile(csvBody("AOG Spring 2026", "Flash Wolves")), false);

        assertThat(preview.readyToImport()).isTrue();
        assertThat(preview.summary().matchesToCreate()).isEqualTo(1);
        assertThat(preview.summary().draftsToCreate()).isEqualTo(1);

        var confirm = service.confirmGameDraftImport(new com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest(preview.previewToken()));

        ArgumentCaptor<EsportsMatch> matchCaptor = ArgumentCaptor.forClass(EsportsMatch.class);
        ArgumentCaptor<EsportsGameDraft> draftCaptor = ArgumentCaptor.forClass(EsportsGameDraft.class);
        verify(esportsMatchRepository).save(matchCaptor.capture());
        verify(esportsGameDraftRepository).save(draftCaptor.capture());
        verify(eloCalculationService).calculateAllRankings();

        EsportsMatch savedMatch = matchCaptor.getValue();
        EsportsGameDraft savedDraft = draftCaptor.getValue();

        assertThat(savedMatch.getTeam1Code()).isEqualTo("FS");
        assertThat(savedMatch.getTeam2Code()).isEqualTo("SGP");
        assertThat(savedMatch.getScore1()).isEqualTo(1);
        assertThat(savedMatch.getScore2()).isZero();
        assertThat(savedMatch.getTournament()).isSameAs(tournament);
        assertThat(savedMatch.getTier()).isEqualTo("2");

        assertThat(savedDraft.getGameNumber()).isEqualTo(1);
        assertThat(savedDraft.getBlueTeam()).isSameAs(fs);
        assertThat(savedDraft.getRedTeam()).isSameAs(sgp);
        assertThat(savedDraft.getWinnerTeam()).isSameAs(fs);
        assertThat(savedDraft.getDurationSeconds()).isEqualTo(1155);

        assertThat(confirm.importedRows()).isEqualTo(1);
        assertThat(confirm.createdMatches()).isEqualTo(1);
        assertThat(confirm.createdDrafts()).isEqualTo(1);
        assertThat(confirm.affectedMatchIds()).containsExactly(99L);
        assertThat(confirm.affectedSeriesCount()).isEqualTo(1);
        assertThat(confirm.rankingsRecalculated()).isTrue();
    }

    private EsportsDraftService service() {
        return new EsportsDraftService(
                esportsMatchRepository,
                esportsTeamRepository,
                esportsTournamentRepository,
                heroRepository,
                esportsGameDraftRepository,
                eloCalculationService,
                objectMapper
        );
    }

    private EsportsGameDraftRequest requestWith(int gameNumber,
                                                Long blueTeamId,
                                                Long redTeamId,
                                                Long winnerTeamId,
                                                long blueBanHeroId,
                                                long redBanHeroId,
                                                long blueLineupHeroId,
                                                long redLineupHeroId) {
        return new EsportsGameDraftRequest(
                gameNumber,
                blueTeamId,
                redTeamId,
                winnerTeamId,
                1000,
                "AOV_STANDARD_18",
                "manual",
                List.of(blueBanHeroId),
                List.of(redBanHeroId),
                new EsportsGameDraftRequest.LineupRequest(blueLineupHeroId, null, null, null, null),
                new EsportsGameDraftRequest.LineupRequest(redLineupHeroId, null, null, null, null)
        );
    }

    private EsportsMatch match(Long id, String team1Code, String team2Code) {
        EsportsMatch match = new EsportsMatch();
        match.setId(id);
        match.setTeam1Code(team1Code);
        match.setTeam2Code(team2Code);
        match.setMatchDate(LocalDateTime.of(2026, 5, 10, 19, 0));
        match.setTier("1");
        match.setStage("bang");
        return match;
    }

    private EsportsGameDraft exportDraft(EsportsMatch match,
                                         int gameNumber,
                                         EsportsTeam blueTeam,
                                         EsportsTeam redTeam,
                                         EsportsTeam winnerTeam) {
        EsportsGameDraft draft = new EsportsGameDraft();
        draft.setId(90L + gameNumber);
        draft.setMatch(match);
        draft.setGameNumber(gameNumber);
        draft.setBlueTeam(blueTeam);
        draft.setRedTeam(redTeam);
        draft.setWinnerTeam(winnerTeam);
        draft.setDurationSeconds(1155);
        draft.setDraftFormatCode("AOV_STANDARD_18");
        draft.setSource("manual");
        draft.setBlueBan1HeroId(101L);
        draft.setBlueBan2HeroId(102L);
        draft.setBlueBan3HeroId(103L);
        draft.setBlueBan4HeroId(104L);
        draft.setBlueBan5HeroId(105L);
        draft.setRedBan1HeroId(106L);
        draft.setRedBan2HeroId(107L);
        draft.setRedBan3HeroId(108L);
        draft.setRedBan4HeroId(109L);
        draft.setRedBan5HeroId(110L);
        draft.setBlueDslHeroId(111L);
        draft.setBlueJglHeroId(112L);
        draft.setBlueMidHeroId(113L);
        draft.setBlueAdlHeroId(114L);
        draft.setBlueSupHeroId(115L);
        draft.setRedDslHeroId(116L);
        draft.setRedJglHeroId(117L);
        draft.setRedMidHeroId(118L);
        draft.setRedAdlHeroId(119L);
        draft.setRedSupHeroId(120L);
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

    private List<Hero> heroes(long fromInclusive, long toInclusive) {
        List<Hero> heroes = new ArrayList<>();
        for (long heroId = fromInclusive; heroId <= toInclusive; heroId++) {
            heroes.add(hero(heroId, "Hero " + heroId));
        }
        return heroes;
    }

    private Hero hero(Long id, String name) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(name.toLowerCase().replace(' ', '-'));
        hero.setAvatarUrl("/images/heroes/" + id + ".jpg");
        return hero;
    }

    private MockMultipartFile csvFile(String body) {
        return new MockMultipartFile("file", "drafts.csv", "text/csv", body.getBytes(StandardCharsets.UTF_8));
    }

    private String csvBody(String tournamentName, String winnerName) {
        return "\uFEFFDate,Tournament,Match,Team_1,T1_Side,T1_DSL,T1_JGL,T1_MID,T1_ADL,T1_SUP,T1_Ban_1,T1_Ban_2,T1_Ban_3,T1_Ban_4,T1_Ban_5,Team_2,T2_Side,T2_DSL,T2_JGL,T2_MID,T2_ADL,T2_SUP,T2_Ban_1,T2_Ban_2,T2_Ban_3,T2_Ban_4,T2_Ban_5,Winner,Length\r\n"
                + "2026-05-11," + tournamentName + ",1,Flash Wolves,Blue,Omen,Nakroth,Liliana,Hayate,Alice,Florentino,Ryoma,Yena,Richter,Maloch,Saigon Phantom,Red,Veres,Aoi,Krixi,Violet,Rouie,Aya,Zip,Helen,Cresht,Toro," + winnerName + ",19:15\r\n";
    }

    private String seriesCsvBody(String tournamentName, String stage, List<String> winners) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("Date,Tournament,Stage,Match,Team_1,T1_Side,T1_DSL,T1_JGL,T1_MID,T1_ADL,T1_SUP,T1_Ban_1,T1_Ban_2,T1_Ban_3,T1_Ban_4,T1_Ban_5,Team_2,T2_Side,T2_DSL,T2_JGL,T2_MID,T2_ADL,T2_SUP,T2_Ban_1,T2_Ban_2,T2_Ban_3,T2_Ban_4,T2_Ban_5,Winner,Length\r\n");
        for (int index = 0; index < winners.size(); index++) {
            builder.append("2026-05-11,")
                    .append(tournamentName)
                    .append(',')
                    .append(stage)
                    .append(',')
                    .append(index + 1)
                    .append(",Flash Wolves,Blue,Omen,Nakroth,Liliana,Hayate,Alice,Florentino,Ryoma,Yena,Richter,Maloch,Saigon Phantom,Red,Veres,Aoi,Krixi,Violet,Rouie,Aya,Zip,Helen,Cresht,Toro,")
                    .append(winners.get(index))
                    .append(",19:15\r\n");
        }
        return builder.toString();
    }

    private List<Hero> importHeroes() {
        return List.of(
                hero(101L, "Florentino"),
                hero(102L, "Ryoma"),
                hero(103L, "Yena"),
                hero(104L, "Richter"),
                hero(105L, "Maloch"),
                hero(106L, "Aya"),
                hero(107L, "Zip"),
                hero(108L, "Helen"),
                hero(109L, "Cresht"),
                hero(110L, "Toro"),
                hero(111L, "Omen"),
                hero(112L, "Nakroth"),
                hero(113L, "Liliana"),
                hero(114L, "Hayate"),
                hero(115L, "Alice"),
                hero(116L, "Veres"),
                hero(117L, "Aoi"),
                hero(118L, "Krixi"),
                hero(119L, "Violet"),
                hero(120L, "Rouie")
        );
    }

    private EsportsTournament tournament(Long id, String name, String slug, String tierLevel) {
        EsportsTournament tournament = new EsportsTournament();
        tournament.setId(id);
        tournament.setName(name);
        tournament.setSlug(slug);
        tournament.setTierLevel(tierLevel);
        tournament.setAerTier(1);
        return tournament;
    }
}
