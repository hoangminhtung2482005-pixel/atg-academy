package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftActionRequest;
import com.example.demo.dto.esports.EsportsMatchGameRequest;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import com.example.demo.repository.EsportsMatchGameRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createGame(10L, new EsportsMatchGameRequest(1, 1L, 2L, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blue/Red team phai thuoc dung 2 doi cua esports match.");

        verify(esportsMatchGameRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDraftActionRejectsDuplicateStepNumber() {
        EsportsMatchGame game = game(50L, team(1L, "FS"), team(2L, "SGP"));
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(game.getBlueTeam()));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
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
        EsportsMatchGame game = game(50L, team(1L, "FS"), team(2L, "SGP"));
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(1L)).thenReturn(Optional.of(game.getBlueTeam()));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));
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
    void createDraftActionRejectsTeamSideMismatch() {
        EsportsTeam blueTeam = team(1L, "FS");
        EsportsTeam redTeam = team(2L, "SGP");
        EsportsMatchGame game = game(50L, blueTeam, redTeam);
        Hero hero = hero(100L, "Hayate");

        when(esportsMatchGameRepository.findById(50L)).thenReturn(Optional.of(game));
        when(esportsTeamRepository.findById(2L)).thenReturn(Optional.of(redTeam));
        when(heroRepository.findById(100L)).thenReturn(Optional.of(hero));

        EsportsDraftService service = service();

        assertThatThrownBy(() -> service.createDraftAction(50L, new EsportsDraftActionRequest(
                2L,
                100L,
                BanPickActionType.PICK,
                3,
                BanPickTeamSide.BLUE
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teamSide BLUE phai khop voi blue team cua game.");

        verify(esportsMatchDraftActionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private EsportsDraftService service() {
        return new EsportsDraftService(
                esportsMatchRepository,
                esportsTeamRepository,
                heroRepository,
                esportsMatchGameRepository,
                esportsMatchDraftActionRepository
        );
    }

    private EsportsTeam team(Long id, String code) {
        EsportsTeam team = new EsportsTeam();
        team.setId(id);
        team.setTeamCode(code);
        team.setTeamName(code + " Team");
        return team;
    }

    private EsportsMatchGame game(Long id, EsportsTeam blueTeam, EsportsTeam redTeam) {
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
        return game;
    }

    private Hero hero(Long id, String name) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(name.toLowerCase());
        return hero;
    }
}
