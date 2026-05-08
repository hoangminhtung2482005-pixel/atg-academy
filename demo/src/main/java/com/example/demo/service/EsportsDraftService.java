package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftActionRequest;
import com.example.demo.dto.esports.EsportsDraftActionResponse;
import com.example.demo.dto.esports.EsportsDraftActionUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameLineupRequest;
import com.example.demo.dto.esports.EsportsMatchGameLineupResponse;
import com.example.demo.dto.esports.EsportsMatchGameLineupUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameRequest;
import com.example.demo.dto.esports.EsportsMatchGameResponse;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsDraftFormat;
import com.example.demo.entity.EsportsDraftPhaseRule;
import com.example.demo.entity.EsportsLineupLaneRole;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsMatchDraftAction;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsMatchGameLineup;
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
import com.example.demo.util.EsportsDraftDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class EsportsDraftService {

    private static final int LINEUP_MIN_POSITION = 1;
    private static final int LINEUP_MAX_POSITION = 5;
    private static final int MAX_LINEUPS_PER_GAME = 10;
    private static final int MAX_LINEUPS_PER_SIDE = 5;

    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsTeamRepository esportsTeamRepository;
    private final HeroRepository heroRepository;
    private final EsportsMatchGameRepository esportsMatchGameRepository;
    private final EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;
    private final EsportsDraftFormatRepository esportsDraftFormatRepository;
    private final EsportsDraftPhaseRuleRepository esportsDraftPhaseRuleRepository;
    private final EsportsMatchGameLineupRepository esportsMatchGameLineupRepository;

    public EsportsDraftService(EsportsMatchRepository esportsMatchRepository,
                               EsportsTeamRepository esportsTeamRepository,
                               HeroRepository heroRepository,
                               EsportsMatchGameRepository esportsMatchGameRepository,
                               EsportsMatchDraftActionRepository esportsMatchDraftActionRepository,
                               EsportsDraftFormatRepository esportsDraftFormatRepository,
                               EsportsDraftPhaseRuleRepository esportsDraftPhaseRuleRepository,
                               EsportsMatchGameLineupRepository esportsMatchGameLineupRepository) {
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsTeamRepository = esportsTeamRepository;
        this.heroRepository = heroRepository;
        this.esportsMatchGameRepository = esportsMatchGameRepository;
        this.esportsMatchDraftActionRepository = esportsMatchDraftActionRepository;
        this.esportsDraftFormatRepository = esportsDraftFormatRepository;
        this.esportsDraftPhaseRuleRepository = esportsDraftPhaseRuleRepository;
        this.esportsMatchGameLineupRepository = esportsMatchGameLineupRepository;
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
        EsportsDraftFormat draftFormat = resolveDraftFormatForWrite(request.draftFormatId());
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
        game.setDraftFormat(draftFormat);
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
        EsportsDraftFormat draftFormat = resolveDraftFormatForWrite(request.draftFormatId());
        validateDurationSeconds(request.durationSeconds());
        validateGameTeams(game.getMatch(), blueTeam, redTeam);

        if (esportsMatchGameRepository.existsByMatchIdAndGameNumberAndIdNot(game.getMatch().getId(), gameNumber, gameId)) {
            throw new IllegalArgumentException("Game number da ton tai trong match nay.");
        }

        game.setGameNumber(gameNumber);
        game.setBlueTeam(blueTeam);
        game.setRedTeam(redTeam);
        game.setWinnerTeam(winnerTeam);
        game.setDraftFormat(draftFormat);
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
    public List<EsportsDraftActionResponse> replaceDraftActions(Long gameId,
                                                                EsportsDraftActionUpsertRequest request) {
        EsportsMatchGame game = requireGame(gameId);
        List<EsportsDraftActionRequest> requestedActions = requireDraftActionPayload(request);
        EsportsDraftFormat draftFormat = resolveDraftFormat(game);
        int totalSteps = draftFormat.getTotalSteps() != null && draftFormat.getTotalSteps() > 0
                ? draftFormat.getTotalSteps()
                : EsportsDraftDefaults.DEFAULT_TOTAL_STEPS;

        if (requestedActions.size() > totalSteps) {
            throw new IllegalArgumentException("Moi game chi duoc luu toi da " + totalSteps + " draft actions.");
        }

        Map<Integer, EsportsDraftPhaseRule> phaseRulesByStep = esportsDraftPhaseRuleRepository
                .findByFormatIdOrderByStepNumberAsc(draftFormat.getId())
                .stream()
                .collect(LinkedHashMap::new, (map, rule) -> map.put(rule.getStepNumber(), rule), LinkedHashMap::putAll);

        Set<Integer> usedSteps = new LinkedHashSet<>();
        Set<Long> usedHeroIds = new LinkedHashSet<>();
        List<EsportsMatchDraftAction> rowsToCreate = new ArrayList<>();

        for (EsportsDraftActionRequest requestedAction : requestedActions) {
            if (requestedAction == null) {
                throw new IllegalArgumentException("actions khong duoc chua item null.");
            }

            EsportsTeam team = requireTeam(requireId(requestedAction.teamId(), "teamId"));
            Hero hero = requireHero(requireId(requestedAction.heroId(), "heroId"));
            BanPickActionType actionType = requireActionType(requestedAction.actionType());
            int stepNumber = requirePositive(requestedAction.stepNumber(), "stepNumber");
            BanPickTeamSide teamSide = requireTeamSide(requestedAction.teamSide());
            EsportsDraftPhaseRule phaseRule = phaseRulesByStep.get(stepNumber);
            if (phaseRule == null) {
                throw new IllegalArgumentException("Step number khong ton tai trong draft format cua game.");
            }

            validateDraftAction(game, team, hero, actionType, stepNumber, teamSide, phaseRule);

            if (!usedSteps.add(stepNumber)) {
                throw new IllegalArgumentException("Khong duoc trung stepNumber trong cung game.");
            }
            if (!usedHeroIds.add(hero.getId())) {
                throw new IllegalArgumentException("Khong duoc trung hero trong cung game.");
            }

            EsportsMatchDraftAction action = new EsportsMatchDraftAction();
            action.setGame(game);
            action.setTeam(team);
            action.setHero(hero);
            action.setActionType(actionType);
            action.setStepNumber(stepNumber);
            action.setTeamSide(teamSide);
            rowsToCreate.add(action);
        }

        List<EsportsMatchDraftAction> existingActions = esportsMatchDraftActionRepository
                .findByGameIdOrderByStepNumberAsc(gameId);
        if (!existingActions.isEmpty()) {
            esportsMatchDraftActionRepository.deleteAllInBatch(existingActions);
            esportsMatchDraftActionRepository.flush();
        }

        if (!rowsToCreate.isEmpty()) {
            esportsMatchDraftActionRepository.saveAll(rowsToCreate);
        }

        purgeInvalidLineups(gameId);
        return getDraftActionsByGameId(gameId);
    }

    @Transactional
    public EsportsDraftActionResponse createDraftAction(Long gameId, EsportsDraftActionRequest request) {
        EsportsMatchGame game = requireGame(gameId);
        EsportsTeam team = requireTeam(requireId(request.teamId(), "teamId"));
        Hero hero = requireHero(requireId(request.heroId(), "heroId"));
        BanPickActionType actionType = requireActionType(request.actionType());
        int stepNumber = requirePositive(request.stepNumber(), "stepNumber");
        BanPickTeamSide teamSide = requireTeamSide(request.teamSide());
        EsportsDraftPhaseRule phaseRule = requirePhaseRule(game, stepNumber);

        validateDraftAction(game, team, hero, actionType, stepNumber, teamSide, phaseRule);

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
        EsportsDraftPhaseRule phaseRule = requirePhaseRule(game, stepNumber);

        validateDraftAction(game, team, hero, actionType, stepNumber, teamSide, phaseRule);

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

        EsportsDraftActionResponse response = toDraftActionResponse(esportsMatchDraftActionRepository.save(action));
        purgeInvalidLineups(game.getId());
        return response;
    }

    @Transactional
    public void deleteDraftAction(Long actionId) {
        EsportsMatchDraftAction action = requireDraftAction(actionId);
        Long gameId = action.getGame().getId();
        esportsMatchDraftActionRepository.delete(action);
        purgeInvalidLineups(gameId);
    }

    @Transactional(readOnly = true)
    public List<EsportsMatchGameLineupResponse> getLineupsByGameId(Long gameId) {
        requireGame(gameId);
        return esportsMatchGameLineupRepository.findByGameIdOrderByTeamSideAscPositionNumberAsc(gameId).stream()
                .map(this::toLineupResponse)
                .toList();
    }

    @Transactional
    public List<EsportsMatchGameLineupResponse> upsertLineups(Long gameId, EsportsMatchGameLineupUpsertRequest request) {
        EsportsMatchGame game = requireGame(gameId);
        List<EsportsMatchGameLineupRequest> requestedLineups = requireLineupPayload(request);
        Map<Long, EsportsMatchDraftAction> pickedActionsByHeroId = mapPickedActionsByHeroId(gameId);
        List<NormalizedLineupRow> normalizedRows = normalizeLineups(game, requestedLineups, pickedActionsByHeroId);

        List<EsportsMatchGameLineup> existingLineups = esportsMatchGameLineupRepository
                .findByGameIdOrderByTeamSideAscPositionNumberAsc(gameId);
        Map<LineupKey, NormalizedLineupRow> requestedByKey = new LinkedHashMap<>();
        normalizedRows.forEach(row -> requestedByKey.put(row.key(), row));

        List<EsportsMatchGameLineup> rowsToDelete = new ArrayList<>();
        for (EsportsMatchGameLineup existingLineup : existingLineups) {
            LineupKey key = new LineupKey(existingLineup.getTeamSide(), existingLineup.getPositionNumber());
            NormalizedLineupRow requestedRow = requestedByKey.get(key);
            if (requestedRow == null || !lineupMatches(existingLineup, requestedRow)) {
                rowsToDelete.add(existingLineup);
            }
        }

        if (!rowsToDelete.isEmpty()) {
            esportsMatchGameLineupRepository.deleteAllInBatch(rowsToDelete);
            esportsMatchGameLineupRepository.flush();
        }

        List<EsportsMatchGameLineup> rowsToCreate = new ArrayList<>();
        for (NormalizedLineupRow row : normalizedRows) {
            EsportsMatchGameLineup existingLineup = existingLineups.stream()
                    .filter(candidate -> candidate.getTeamSide() == row.teamSide()
                            && candidate.getPositionNumber().equals(row.positionNumber()))
                    .findFirst()
                    .orElse(null);
            if (existingLineup != null && lineupMatches(existingLineup, row)) {
                continue;
            }

            EsportsMatchGameLineup lineup = new EsportsMatchGameLineup();
            lineup.setGame(game);
            lineup.setTeam(row.team());
            lineup.setTeamSide(row.teamSide());
            lineup.setPositionNumber(row.positionNumber());
            lineup.setLaneRole(row.laneRole());
            lineup.setHero(row.hero());
            rowsToCreate.add(lineup);
        }

        if (!rowsToCreate.isEmpty()) {
            esportsMatchGameLineupRepository.saveAll(rowsToCreate);
        }

        return getLineupsByGameId(gameId);
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

    private EsportsDraftFormat requireDraftFormat(Long draftFormatId) {
        return esportsDraftFormatRepository.findById(draftFormatId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay esports draft format voi ID: " + draftFormatId));
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

    private EsportsLineupLaneRole requireLaneRole(String laneRole) {
        if (laneRole == null || laneRole.isBlank()) {
            throw new IllegalArgumentException("laneRole la bat buoc.");
        }

        String normalized = laneRole.trim().toUpperCase();
        if ("TOP".equals(normalized)) {
            normalized = EsportsLineupLaneRole.DSL.name();
        }

        try {
            return EsportsLineupLaneRole.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("laneRole phai la DSL, JGL, MID, ADL hoac SUP.");
        }
    }

    private int requireLineupPosition(Integer positionNumber) {
        if (positionNumber == null) {
            throw new IllegalArgumentException("positionNumber la bat buoc.");
        }
        if (positionNumber < LINEUP_MIN_POSITION || positionNumber > LINEUP_MAX_POSITION) {
            throw new IllegalArgumentException("positionNumber phai nam trong khoang 1..5.");
        }
        return positionNumber;
    }

    private List<EsportsMatchGameLineupRequest> requireLineupPayload(EsportsMatchGameLineupUpsertRequest request) {
        if (request == null || request.lineups() == null) {
            throw new IllegalArgumentException("lineups la bat buoc.");
        }
        return request.lineups();
    }

    private List<EsportsDraftActionRequest> requireDraftActionPayload(EsportsDraftActionUpsertRequest request) {
        if (request == null || request.actions() == null) {
            throw new IllegalArgumentException("actions la bat buoc.");
        }
        return request.actions();
    }

    private EsportsDraftFormat resolveDraftFormatForWrite(Long draftFormatId) {
        if (draftFormatId != null) {
            return requireDraftFormat(draftFormatId);
        }
        return requireDefaultDraftFormat();
    }

    private EsportsDraftFormat resolveDraftFormat(EsportsMatchGame game) {
        if (game.getDraftFormat() != null) {
            return game.getDraftFormat();
        }
        return requireDefaultDraftFormat();
    }

    private EsportsDraftFormat requireDefaultDraftFormat() {
        return esportsDraftFormatRepository.findByCode(EsportsDraftDefaults.DEFAULT_FORMAT_CODE)
                .or(() -> esportsDraftFormatRepository.findFirstByDefaultFormatTrueAndActiveTrueOrderByIdAsc())
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay draft format mac dinh."));
    }

    private EsportsDraftPhaseRule requirePhaseRule(EsportsMatchGame game, int stepNumber) {
        EsportsDraftFormat draftFormat = resolveDraftFormat(game);
        return esportsDraftPhaseRuleRepository.findByFormatIdAndStepNumber(draftFormat.getId(), stepNumber)
                .orElseThrow(() -> new IllegalArgumentException("Step number khong ton tai trong draft format cua game."));
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
                                     BanPickActionType actionType,
                                     int stepNumber,
                                     BanPickTeamSide teamSide,
                                     EsportsDraftPhaseRule phaseRule) {
        if (hero.getId() == null) {
            throw new IllegalArgumentException("Hero khong hop le.");
        }
        if (stepNumber <= 0) {
            throw new IllegalArgumentException("stepNumber phai lon hon 0.");
        }
        if (phaseRule.getActionType() != actionType || phaseRule.getTeamSide() != teamSide) {
            throw new IllegalArgumentException("Step " + stepNumber + " chi cho phep "
                    + phaseRule.getTeamSide() + " " + phaseRule.getActionType() + ".");
        }

        validateTeamSideForGame(game, team, teamSide);
    }

    private List<NormalizedLineupRow> normalizeLineups(EsportsMatchGame game,
                                                       List<EsportsMatchGameLineupRequest> requestedLineups,
                                                       Map<Long, EsportsMatchDraftAction> pickedActionsByHeroId) {
        if (requestedLineups.size() > MAX_LINEUPS_PER_GAME) {
            throw new IllegalArgumentException("Moi game chi duoc luu toi da 10 lineup rows.");
        }

        List<NormalizedLineupRow> normalizedRows = new ArrayList<>();
        Set<LineupKey> usedPositionKeys = new LinkedHashSet<>();
        Set<String> usedLaneKeys = new LinkedHashSet<>();
        Set<Long> usedHeroIds = new LinkedHashSet<>();
        int blueCount = 0;
        int redCount = 0;

        for (EsportsMatchGameLineupRequest request : requestedLineups) {
            if (request == null) {
                throw new IllegalArgumentException("lineups khong duoc chua item null.");
            }

            EsportsTeam team = requireTeam(requireId(request.teamId(), "teamId"));
            BanPickTeamSide teamSide = requireTeamSide(request.teamSide());
            int positionNumber = requireLineupPosition(request.positionNumber());
            EsportsLineupLaneRole laneRole = requireLaneRole(request.laneRole());
            Hero hero = requireHero(requireId(request.heroId(), "heroId"));

            validateTeamSideForGame(game, team, teamSide);
            validateLaneRolePosition(positionNumber, laneRole);
            validatePickedHeroForLineup(game, team, hero, teamSide, pickedActionsByHeroId);

            LineupKey positionKey = new LineupKey(teamSide, positionNumber);
            if (!usedPositionKeys.add(positionKey)) {
                throw new IllegalArgumentException("Khong duoc trung position_number trong cung team/game.");
            }

            String laneKey = teamSide.name() + ":" + laneRole.name();
            if (!usedLaneKeys.add(laneKey)) {
                throw new IllegalArgumentException("Khong duoc trung lane_role trong cung team/game.");
            }

            if (!usedHeroIds.add(hero.getId())) {
                throw new IllegalArgumentException("Khong duoc trung hero trong cung game.");
            }

            if (teamSide == BanPickTeamSide.BLUE) {
                blueCount++;
                if (blueCount > MAX_LINEUPS_PER_SIDE) {
                    throw new IllegalArgumentException("Moi team/side chi duoc luu toi da 5 lineup rows.");
                }
            } else {
                redCount++;
                if (redCount > MAX_LINEUPS_PER_SIDE) {
                    throw new IllegalArgumentException("Moi team/side chi duoc luu toi da 5 lineup rows.");
                }
            }

            normalizedRows.add(new NormalizedLineupRow(positionKey, team, teamSide, positionNumber, laneRole, hero));
        }

        return normalizedRows;
    }

    private void validateLaneRolePosition(int positionNumber, EsportsLineupLaneRole laneRole) {
        if (!laneRole.matchesPosition(positionNumber)) {
            EsportsLineupLaneRole expectedRole = EsportsLineupLaneRole.fromPosition(positionNumber)
                    .orElseThrow(() -> new IllegalArgumentException("positionNumber phai nam trong khoang 1..5."));
            throw new IllegalArgumentException("laneRole phai khop voi positionNumber. Vi tri "
                    + positionNumber + " bat buoc la " + expectedRole.name() + ".");
        }
    }

    private void validatePickedHeroForLineup(EsportsMatchGame game,
                                             EsportsTeam team,
                                             Hero hero,
                                             BanPickTeamSide teamSide,
                                             Map<Long, EsportsMatchDraftAction> pickedActionsByHeroId) {
        EsportsMatchDraftAction pickedAction = pickedActionsByHeroId.get(hero.getId());
        if (pickedAction == null) {
            if (esportsMatchDraftActionRepository.existsByGameIdAndHeroId(game.getId(), hero.getId())) {
                throw new IllegalArgumentException("Hero chi bi BAN thi khong duoc luu vao final lineup.");
            }
            throw new IllegalArgumentException("Hero phai la hero da PICK trong game.");
        }

        if (pickedAction.getTeamSide() != teamSide) {
            throw new IllegalArgumentException("teamSide phai khop voi side da PICK hero trong game.");
        }
        if (!pickedAction.getTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("teamId phai khop voi team da PICK hero trong game.");
        }
    }

    private void validateTeamSideForGame(EsportsMatchGame game,
                                         EsportsTeam team,
                                         BanPickTeamSide teamSide) {
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

    private Map<Long, EsportsMatchDraftAction> mapPickedActionsByHeroId(Long gameId) {
        Map<Long, EsportsMatchDraftAction> pickedActionsByHeroId = new LinkedHashMap<>();
        esportsMatchDraftActionRepository.findByGameIdAndActionTypeOrderByStepNumberAsc(gameId, BanPickActionType.PICK)
                .forEach(action -> pickedActionsByHeroId.put(action.getHero().getId(), action));
        return pickedActionsByHeroId;
    }

    private boolean lineupMatches(EsportsMatchGameLineup existingLineup, NormalizedLineupRow requestedRow) {
        return existingLineup.getTeam().getId().equals(requestedRow.team().getId())
                && existingLineup.getTeamSide() == requestedRow.teamSide()
                && existingLineup.getPositionNumber().equals(requestedRow.positionNumber())
                && existingLineup.getLaneRole() == requestedRow.laneRole()
                && existingLineup.getHero().getId().equals(requestedRow.hero().getId());
    }

    private void purgeInvalidLineups(Long gameId) {
        List<EsportsMatchGameLineup> existingLineups = esportsMatchGameLineupRepository
                .findByGameIdOrderByTeamSideAscPositionNumberAsc(gameId);
        if (existingLineups.isEmpty()) {
            return;
        }

        Map<Long, EsportsMatchDraftAction> pickedActionsByHeroId = mapPickedActionsByHeroId(gameId);
        List<EsportsMatchGameLineup> invalidLineups = existingLineups.stream()
                .filter(lineup -> {
                    EsportsMatchDraftAction pickedAction = pickedActionsByHeroId.get(lineup.getHero().getId());
                    return pickedAction == null
                            || pickedAction.getTeamSide() != lineup.getTeamSide()
                            || !pickedAction.getTeam().getId().equals(lineup.getTeam().getId());
                })
                .toList();

        if (!invalidLineups.isEmpty()) {
            esportsMatchGameLineupRepository.deleteAllInBatch(invalidLineups);
        }
    }

    private EsportsMatchGameResponse toGameResponse(EsportsMatchGame game) {
        EsportsTeam blueTeam = game.getBlueTeam();
        EsportsTeam redTeam = game.getRedTeam();
        EsportsTeam winnerTeam = game.getWinnerTeam();
        EsportsDraftFormat draftFormat = resolveDraftFormat(game);
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
                draftFormat.getId(),
                draftFormat.getCode(),
                draftFormat.getName(),
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

    private EsportsMatchGameLineupResponse toLineupResponse(EsportsMatchGameLineup lineup) {
        EsportsTeam team = lineup.getTeam();
        Hero hero = lineup.getHero();
        return new EsportsMatchGameLineupResponse(
                lineup.getId(),
                lineup.getGame().getId(),
                team.getId(),
                team.getTeamCode(),
                displayTeamName(team),
                team.getLogoUrl(),
                lineup.getTeamSide(),
                lineup.getPositionNumber(),
                lineup.getLaneRole().name(),
                hero.getId(),
                hero.getName(),
                hero.getSlug(),
                hero.getAvatarUrl(),
                lineup.getCreatedAt(),
                lineup.getUpdatedAt()
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

    private record LineupKey(BanPickTeamSide teamSide, Integer positionNumber) {
    }

    private record NormalizedLineupRow(
            LineupKey key,
            EsportsTeam team,
            BanPickTeamSide teamSide,
            Integer positionNumber,
            EsportsLineupLaneRole laneRole,
            Hero hero
    ) {
    }
}
