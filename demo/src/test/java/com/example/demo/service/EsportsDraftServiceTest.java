package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftActionRequest;
import com.example.demo.dto.esports.EsportsDraftActionUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameLineupRequest;
import com.example.demo.dto.esports.EsportsMatchGameLineupUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameRequest;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsDraftFormat;
import com.example.demo.entity.EsportsDraftPhaseRule;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsMatchDraftAction;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsDraftFormatRepository;
import com.example.demo.repository.EsportsDraftPhaseRuleRepository;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import com.example.demo.repository.EsportsMatchGameLineupRepository;
import com.example.demo.repository.EsportsMatchGameRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
    private EsportsMatchGameRepository esportsMatchGameRepository;

    @Mock
    private EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;

    @Mock
    private EsportsDraftFormatRepository esportsDraftFormatRepository;

    @Mock
    private EsportsDraftPhaseRuleRepository esportsDraftPhaseRuleRepository;

    @Mock
    private EsportsMatchGameLineupRepository esportsMatchGameLineupRepository;

    @Test
    void createGameRejectsTeamsOutsideMatchParticipants() {
        EsportsMatch match = new EsportsMatch();
        match.setId(10L);
        match.setTeam1Code("FS");
        match.setTeam2Code("SGP");

        EsportsTeam fs = team(1L, "FS");
        EsportsTeam kog = team(2L, "KOG");

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(match));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(fs));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(kog));
        when(esportsDraftFormatRepository.findByCode("AOV_STANDARD_18"))
                .thenReturn(Optional.of(draftFormat(99L)));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createGame(10L, new EsportsMatchGameRequest(1, 1L, 2L, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blue/Red team phai thuoc dung 2 doi cua esports match.");

        verify(esportsMatchGameRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDraftActionRejectsDuplicateStepNumber() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsMatchGame game = game(50L, team(1L, "FS"), team(2L, "SGP"), format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(game.getBlueTeam()));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsDraftPhaseRuleRepository.findByFormatIdAndStepNumber(900L, 1))
                .thenReturn(Optional.of(phaseRule(format, 1, BanPickTeamSide.BLUE, BanPickActionType.BAN)));
        when(esportsMatchDraftActionRepository.existsByGameIdAndStepNumber(50L, 1)).thenReturn(true);

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createDraftAction(50L, new EsportsDraftActionRequest(
                1L,
                100L,
                BanPickActionType.BAN,
                1,
                BanPickTeamSide.BLUE
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step number da ton tai trong game nay.");

        verify(esportsMatchDraftActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDraftActionRejectsDuplicateHeroInSameGame() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsMatchGame game = game(50L, team(1L, "FS"), team(2L, "SGP"), format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(game.getBlueTeam()));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsDraftPhaseRuleRepository.findByFormatIdAndStepNumber(900L, 1))
                .thenReturn(Optional.of(phaseRule(format, 1, BanPickTeamSide.BLUE, BanPickActionType.BAN)));
        when(esportsMatchDraftActionRepository.existsByGameIdAndStepNumber(50L, 1)).thenReturn(false);
        when(esportsMatchDraftActionRepository.existsByGameIdAndHeroId(50L, 100L)).thenReturn(true);

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createDraftAction(50L, new EsportsDraftActionRequest(
                1L,
                100L,
                BanPickActionType.BAN,
                1,
                BanPickTeamSide.BLUE
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hero da duoc ban/pick trong game nay.");

        verify(esportsMatchDraftActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDraftActionRejectsPhaseActionMismatch() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsMatchGame game = game(50L, team(1L, "FS"), team(2L, "SGP"), format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(game.getBlueTeam()));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsDraftPhaseRuleRepository.findByFormatIdAndStepNumber(900L, 5))
                .thenReturn(Optional.of(phaseRule(format, 5, BanPickTeamSide.BLUE, BanPickActionType.PICK)));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createDraftAction(50L, new EsportsDraftActionRequest(
                1L,
                100L,
                BanPickActionType.BAN,
                5,
                BanPickTeamSide.BLUE
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Step 5 chi cho phep BLUE PICK.");

        verify(esportsMatchDraftActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDraftActionRejectsTeamIdMismatchWithPhaseSide() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam, format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(blueTeam));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsDraftPhaseRuleRepository.findByFormatIdAndStepNumber(900L, 6))
                .thenReturn(Optional.of(phaseRule(format, 6, BanPickTeamSide.RED, BanPickActionType.PICK)));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createDraftAction(50L, new EsportsDraftActionRequest(
                1L,
                100L,
                BanPickActionType.PICK,
                6,
                BanPickTeamSide.RED
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teamSide RED phai khop voi red team cua game.");

        verify(esportsMatchDraftActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upsertLineupsRejectsHeroThatWasOnlyBanned() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam, format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(blueTeam));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsMatchDraftActionRepository.findByGameIdAndActionTypeOrderByStepNumberAsc(50L, BanPickActionType.PICK))
                .thenReturn(List.of());
        when(esportsMatchDraftActionRepository.existsByGameIdAndHeroId(50L, 100L)).thenReturn(true);

        EsportsDraftService service = service();

        EsportsMatchGameLineupUpsertRequest request = new EsportsMatchGameLineupUpsertRequest(List.of(
                new EsportsMatchGameLineupRequest(1L, BanPickTeamSide.BLUE, 1, "DSL", 100L)
        ));

        assertThatThrownBy(() -> service.upsertLineups(50L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hero chi bi BAN thi khong duoc luu vao final lineup.");

        verify(esportsMatchGameLineupRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void upsertLineupsRejectsInvalidPositionNumber() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam, format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(blueTeam));
        when(esportsMatchDraftActionRepository.findByGameIdAndActionTypeOrderByStepNumberAsc(50L, BanPickActionType.PICK))
                .thenReturn(List.of());

        EsportsDraftService service = service();

        EsportsMatchGameLineupUpsertRequest request = new EsportsMatchGameLineupUpsertRequest(List.of(
                new EsportsMatchGameLineupRequest(1L, BanPickTeamSide.BLUE, 6, "SUP", 100L)
        ));

        assertThatThrownBy(() -> service.upsertLineups(50L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positionNumber phai nam trong khoang 1..5.");
    }

    @Test
    void replaceDraftActionsAllowsClearingExistingDraft() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam, format);
        EsportsMatchDraftAction existingAction = draftAction(game, blueTeam, hero(100L, "Hayate"),
                BanPickActionType.BAN, 1, BanPickTeamSide.BLUE);

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsMatchDraftActionRepository.findByGameIdOrderByStepNumberAsc(50L))
                .thenReturn(List.of(existingAction), List.of());
        when(esportsMatchGameLineupRepository.findByGameIdOrderByTeamSideAscPositionNumberAsc(50L))
                .thenReturn(List.of());

        EsportsDraftService service = service();

        assertThatCode(() -> service.replaceDraftActions(50L, new EsportsDraftActionUpsertRequest(List.of())))
                .doesNotThrowAnyException();

        verify(esportsMatchDraftActionRepository).deleteAllInBatch(List.of(existingAction));
        verify(esportsMatchDraftActionRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void replaceDraftActionsRejectsDuplicateHeroWithinPayload() {
        EsportsDraftFormat format = draftFormat(900L);
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam, format);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(blueTeam));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(redTeam));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
        when(esportsDraftPhaseRuleRepository.findByFormatIdOrderByStepNumberAsc(900L)).thenReturn(List.of(
                phaseRule(format, 1, BanPickTeamSide.BLUE, BanPickActionType.BAN),
                phaseRule(format, 2, BanPickTeamSide.RED, BanPickActionType.BAN)
        ));

        EsportsDraftService service = service();

        EsportsDraftActionUpsertRequest request = new EsportsDraftActionUpsertRequest(List.of(
                new EsportsDraftActionRequest(1L, 100L, BanPickActionType.BAN, 1, BanPickTeamSide.BLUE),
                new EsportsDraftActionRequest(2L, 100L, BanPickActionType.BAN, 2, BanPickTeamSide.RED)
        ));

        assertThatThrownBy(() -> service.replaceDraftActions(50L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khong duoc trung hero trong cung game.");

        verify(esportsMatchDraftActionRepository, never()).deleteAllInBatch(org.mockito.ArgumentMatchers.any());
        verify(esportsMatchDraftActionRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    private EsportsDraftService service() {
        return new EsportsDraftService(
                esportsMatchRepository,
                esportsTeamRepository,
                heroRepository,
                esportsMatchGameRepository,
                esportsMatchDraftActionRepository,
                esportsDraftFormatRepository,
                esportsDraftPhaseRuleRepository,
                esportsMatchGameLineupRepository
        );
    }

    private EsportsDraftFormat draftFormat(Long id) {
        EsportsDraftFormat format = new EsportsDraftFormat();
        format.setId(id);
        format.setCode("AOV_STANDARD_18");
        format.setName("AOV Standard 18 Phase");
        format.setTotalSteps(18);
        format.setDefaultFormat(true);
        format.setActive(true);
        return format;
    }

    private EsportsDraftPhaseRule phaseRule(EsportsDraftFormat format,
                                            int stepNumber,
                                            BanPickTeamSide teamSide,
                                            BanPickActionType actionType) {
        EsportsDraftPhaseRule rule = new EsportsDraftPhaseRule();
        rule.setId((long) stepNumber);
        rule.setFormat(format);
        rule.setStepNumber(stepNumber);
        rule.setTeamSide(teamSide);
        rule.setActionType(actionType);
        return rule;
    }

    private EsportsTeam team(Long id, String code) {
        EsportsTeam team = new EsportsTeam();
        team.setId(id);
        team.setTeamCode(code);
        team.setTeamName(code + " Team");
        return team;
    }

    private EsportsMatchGame game(Long id, EsportsTeam blueTeam, EsportsTeam redTeam, EsportsDraftFormat draftFormat) {
        EsportsMatch match = new EsportsMatch();
        match.setId(99L);
        match.setTeam1Code(blueTeam.getTeamCode());
        match.setTeam2Code(redTeam.getTeamCode());

        EsportsMatchGame game = new EsportsMatchGame();
        game.setId(id);
        game.setMatch(match);
        game.setGameNumber(1);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setDraftFormat(draftFormat);
        return game;
    }

    private Hero hero(Long id, String name) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(name.toLowerCase());
        return hero;
    }

    private EsportsMatchDraftAction draftAction(EsportsMatchGame game,
                                                EsportsTeam team,
                                                Hero hero,
                                                BanPickActionType actionType,
                                                int stepNumber,
                                                BanPickTeamSide teamSide) {
        EsportsMatchDraftAction action = new EsportsMatchDraftAction();
        action.setId((long) stepNumber);
        action.setGame(game);
        action.setTeam(team);
        action.setHero(hero);
        action.setActionType(actionType);
        action.setStepNumber(stepNumber);
        action.setTeamSide(teamSide);
        return action;
    }
}
