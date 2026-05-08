package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftActionRequest;
import com.example.demo.dto.esports.EsportsDraftActionResponse;
import com.example.demo.dto.esports.EsportsMatchGameRequest;
import com.example.demo.dto.esports.EsportsMatchGameResponse;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsMatchDraftAction;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import com.example.demo.repository.EsportsMatchGameRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.HeroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class EsportsDraftService {

    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsTeamRepository esportsTeamRepository;
    private final HeroRepository heroRepository;
    private final EsportsMatchGameRepository esportsMatchGameRepository;
    private final EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;

    public EsportsDraftService(EsportsMatchRepository esportsMatchRepository,
                               EsportsTeamRepository esportsTeamRepository,
                               HeroRepository heroRepository,
                               EsportsMatchGameRepository esportsMatchGameRepository,
                               EsportsMatchDraftActionRepository esportsMatchDraftActionRepository) {
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsTeamRepository = esportsTeamRepository;
        this.heroRepository = heroRepository;
        this.esportsMatchGameRepository = esportsMatchGameRepository;
        this.esportsMatchDraftActionRepository = esportsMatchDraftActionRepository;
    }

    @Transactional(readOnly = true)
    public List<EsportsMatchGameResponse> getGamesByMatchId(Long matchId) {
        requireMatch(matchId);
        return esportsMatchGameRepository.findByMatchIdOrderByGameNumberAsc(matchId).stream()
                .map(this::toGameResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EsportsMatchGameResponse getGame(Long gameId) {
        return toGameResponse(requireGame(gameId));
    }

    @Transactional
    public EsportsMatchGameResponse createGame(Long matchId, EsportsMatchGameRequest request) {
        EsportsMatch match = requireMatch(matchId);
        int gameNumber = requirePositive(request.gameNumber(), "gameNumber");
        EsportsTeam blueTeam = requireTeam(requireId(request.blueTeamId(), "blueTeamId"));
        EsportsTeam redTeam = requireTeam(requireId(request.redTeamId(), "redTeamId"));
        EsportsTeam winnerTeam = resolveWinnerTeam(request.winnerTeamId(), blueTeam, redTeam);
        validateDurationSeconds(request.durationSeconds());
        validateGameTeams(match, blueTeam, redTeam);

        if (esportsMatchGameRepository.existsByMatchIdAndGameNumber(matchId, gameNumber)) {
            throw new IllegalArgumentException("Game number da ton tai trong match nay.");
        }

        EsportsMatchGame game = new EsportsMatchGame();
        game.setMatch(match);
        game.setGameNumber(gameNumber);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setWinnerTeam(winnerTeam);
        game.setDurationSeconds(request.durationSeconds());

        return toGameResponse(esportsMatchGameRepository.save(game));
    }

    @Transactional
    public EsportsMatchGameResponse updateGame(Long gameId, EsportsMatchGameRequest request) {
        EsportsMatchGame game = requireGame(gameId);
        int gameNumber = requirePositive(request.gameNumber(), "gameNumber");
        EsportsTeam blueTeam = requireTeam(requireId(request.blueTeamId(), "blueTeamId"));
        EsportsTeam redTeam = requireTeam(requireId(request.redTeamId(), "redTeamId"));
        EsportsTeam winnerTeam = resolveWinnerTeam(request.winnerTeamId(), blueTeam, redTeam);
        validateDurationSeconds(request.durationSeconds());
        validateGameTeams(game.getMatch(), blueTeam, redTeam);

        if (esportsMatchGameRepository.existsByMatchIdAndGameNumberAndIdNot(game.getMatch().getId(), gameNumber, gameId)) {
            throw new IllegalArgumentException("Game number da ton tai trong match nay.");
        }

        game.setGameNumber(gameNumber);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setWinnerTeam(winnerTeam);
        game.setDurationSeconds(request.durationSeconds());

        return toGameResponse(esportsMatchGameRepository.save(game));
    }

    @Transactional
    public void deleteGame(Long gameId) {
        EsportsMatchGame game = requireGame(gameId);
        esportsMatchGameRepository.delete(game);
    }

    @Transactional(readOnly = true)
    public List<EsportsDraftActionResponse> getDraftActionsByGameId(Long gameId) {
        requireGame(gameId);
        return esportsMatchDraftActionRepository.findByGameIdOrderByStepNumberAsc(gameId).stream()
                .map(this::toDraftActionResponse)
                .toList();
    }

    @Transactional
    public EsportsDraftActionResponse createDraftAction(Long gameId, EsportsDraftActionRequest request) {
        EsportsMatchGame game = requireGame(gameId);
        EsportsTeam team = requireTeam(requireId(request.teamId(), "teamId"));
        Hero hero = requireHero(requireId(request.heroId(), "heroId"));
        BanPickActionType actionType = requireActionType(request.actionType());
        int stepNumber = requirePositive(request.stepNumber(), "stepNumber");
        BanPickTeamSide teamSide = requireTeamSide(request.teamSide());

        validateDraftAction(game, team, hero, stepNumber, teamSide);

        if (esportsMatchDraftActionRepository.existsByGameIdAndStepNumber(gameId, stepNumber)) {
            throw new IllegalArgumentException("Step number da ton tai trong game nay.");
        }
        if (esportsMatchDraftActionRepository.existsByGameIdAndHeroId(gameId, hero.getId())) {
            throw new IllegalArgumentException("Hero da duoc ban/pick trong game nay.");
        }

        EsportsMatchDraftAction action = new EsportsMatchDraftAction();
        action.setGame(game);
        action.setTeam(team);
        action.setHero(hero);
        action.setActionType(actionType);
        action.setStepNumber(stepNumber);
        action.setTeamSide(teamSide);

        return toDraftActionResponse(esportsMatchDraftActionRepository.save(action));
    }

    @Transactional
    public EsportsDraftActionResponse updateDraftAction(Long actionId, EsportsDraftActionRequest request) {
        EsportsMatchDraftAction action = requireDraftAction(actionId);
        EsportsMatchGame game = action.getGame();
        EsportsTeam team = requireTeam(requireId(request.teamId(), "teamId"));
        Hero hero = requireHero(requireId(request.heroId(), "heroId"));
        BanPickActionType actionType = requireActionType(request.actionType());
        int stepNumber = requirePositive(request.stepNumber(), "stepNumber");
        BanPickTeamSide teamSide = requireTeamSide(request.teamSide());

        validateDraftAction(game, team, hero, stepNumber, teamSide);

        if (esportsMatchDraftActionRepository.existsByGameIdAndStepNumberAndIdNot(game.getId(), stepNumber, actionId)) {
            throw new IllegalArgumentException("Step number da ton tai trong game nay.");
        }
        if (esportsMatchDraftActionRepository.existsByGameIdAndHeroIdAndIdNot(game.getId(), hero.getId(), actionId)) {
            throw new IllegalArgumentException("Hero da duoc ban/pick trong game nay.");
        }

        action.setTeam(team);
        action.setHero(hero);
        action.setActionType(actionType);
        action.setStepNumber(stepNumber);
        action.setTeamSide(teamSide);

        return toDraftActionResponse(esportsMatchDraftActionRepository.save(action));
    }

    @Transactional
    public void deleteDraftAction(Long actionId) {
        EsportsMatchDraftAction action = requireDraftAction(actionId);
        esportsMatchDraftActionRepository.delete(action);
    }

    private EsportsMatch requireMatch(Long matchId) {
        return esportsMatchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay esports match voi ID: " + matchId));
    }

    private EsportsMatchGame requireGame(Long gameId) {
        return esportsMatchGameRepository.findById(gameId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay esports game voi ID: " + gameId));
    }

    private EsportsMatchDraftAction requireDraftAction(Long actionId) {
        return esportsMatchDraftActionRepository.findById(actionId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay draft action voi ID: " + actionId));
    }

    private EsportsTeam requireTeam(Long teamId) {
        return esportsTeamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay esports team voi ID: " + teamId));
    }

    private Hero requireHero(Long heroId) {
        return heroRepository.findById(heroId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay hero voi ID: " + heroId));
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " la bat buoc.");
        }
        return value;
    }

    private int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " la bat buoc.");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " phai lon hon 0.");
        }
        return value;
    }

    private BanPickActionType requireActionType(BanPickActionType actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("actionType la bat buoc.");
        }
        return actionType;
    }

    private BanPickTeamSide requireTeamSide(BanPickTeamSide teamSide) {
        if (teamSide == null) {
            throw new IllegalArgumentException("teamSide la bat buoc.");
        }
        return teamSide;
    }

    private void validateDurationSeconds(Integer durationSeconds) {
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds khong duoc am.");
        }
    }

    private void validateGameTeams(EsportsMatch match, EsportsTeam blueTeam, EsportsTeam redTeam) {
        if (blueTeam.getId().equals(redTeam.getId())) {
            throw new IllegalArgumentException("Blue team va red team khong duoc trung nhau.");
        }

        Set<String> matchTeamCodes = new LinkedHashSet<>();
        matchTeamCodes.add(normalizeCode(match.getTeam1Code()));
        matchTeamCodes.add(normalizeCode(match.getTeam2Code()));

        if (!matchTeamCodes.contains(normalizeCode(blueTeam.getTeamCode()))
                || !matchTeamCodes.contains(normalizeCode(redTeam.getTeamCode()))) {
            throw new IllegalArgumentException("Blue/Red team phai thuoc dung 2 doi cua esports match.");
        }
    }

    private EsportsTeam resolveWinnerTeam(Long winnerTeamId, EsportsTeam blueTeam, EsportsTeam redTeam) {
        if (winnerTeamId == null) {
            return null;
        }

        EsportsTeam winnerTeam = requireTeam(winnerTeamId);
        boolean winnerMatchesBlue = winnerTeam.getId().equals(blueTeam.getId());
        boolean winnerMatchesRed = winnerTeam.getId().equals(redTeam.getId());
        if (!winnerMatchesBlue && !winnerMatchesRed) {
            throw new IllegalArgumentException("winnerTeamId phai la blue team hoac red team cua game.");
        }
        return winnerTeam;
    }

    private void validateDraftAction(EsportsMatchGame game,
                                     EsportsTeam team,
                                     Hero hero,
                                     int stepNumber,
                                     BanPickTeamSide teamSide) {
        if (hero.getId() == null) {
            throw new IllegalArgumentException("Hero khong hop le.");
        }
        if (stepNumber <= 0) {
            throw new IllegalArgumentException("stepNumber phai lon hon 0.");
        }

        Long blueTeamId = game.getBlueTeam().getId();
        Long redTeamId = game.getRedTeam().getId();
        if (!team.getId().equals(blueTeamId) && !team.getId().equals(redTeamId)) {
            throw new IllegalArgumentException("teamId phai la doi xanh hoac doi do cua game.");
        }

        if (teamSide == BanPickTeamSide.BLUE && !team.getId().equals(blueTeamId)) {
            throw new IllegalArgumentException("teamSide BLUE phai khop voi blue team cua game.");
        }
        if (teamSide == BanPickTeamSide.RED && !team.getId().equals(redTeamId)) {
            throw new IllegalArgumentException("teamSide RED phai khop voi red team cua game.");
        }
    }

    private EsportsMatchGameResponse toGameResponse(EsportsMatchGame game) {
        EsportsTeam blueTeam = game.getBlueTeam();
        EsportsTeam redTeam = game.getRedTeam();
        EsportsTeam winnerTeam = game.getWinnerTeam();
        return new EsportsMatchGameResponse(
                game.getId(),
                game.getMatch().getId(),
                game.getGameNumber(),
                blueTeam.getId(),
                blueTeam.getTeamCode(),
                displayTeamName(blueTeam),
                blueTeam.getLogoUrl(),
                redTeam.getId(),
                redTeam.getTeamCode(),
                displayTeamName(redTeam),
                redTeam.getLogoUrl(),
                winnerTeam != null ? winnerTeam.getId() : null,
                winnerTeam != null ? winnerTeam.getTeamCode() : null,
                winnerTeam != null ? displayTeamName(winnerTeam) : null,
                winnerTeam != null ? winnerTeam.getLogoUrl() : null,
                game.getDurationSeconds(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }

    private EsportsDraftActionResponse toDraftActionResponse(EsportsMatchDraftAction action) {
        EsportsTeam team = action.getTeam();
        Hero hero = action.getHero();
        return new EsportsDraftActionResponse(
                action.getId(),
                action.getGame().getId(),
                team.getId(),
                team.getTeamCode(),
                displayTeamName(team),
                team.getLogoUrl(),
                hero.getId(),
                hero.getName(),
                hero.getSlug(),
                hero.getAvatarUrl(),
                action.getActionType(),
                action.getStepNumber(),
                action.getTeamSide(),
                action.getCreatedAt()
        );
    }

    private String displayTeamName(EsportsTeam team) {
        if (team.getTeamName() != null && !team.getTeamName().isBlank()) {
            return team.getTeamName();
        }
        return team.getTeamCode();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
