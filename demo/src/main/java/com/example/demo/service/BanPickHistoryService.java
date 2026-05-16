package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickProfileResponse;
import com.example.demo.dto.banpick.BanPickPlayerCardResponse;
import com.example.demo.dto.banpick.BanPickUserSummary;
import com.example.demo.dto.banpick.DraftHistoryResponse;
import com.example.demo.dto.banpick.HeroPickStatResponse;
import com.example.demo.dto.banpick.PlayerStatsResponse;
import com.example.demo.dto.banpick.RecordDraftWinnerRequest;
import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.DraftHistoryEndReason;
import com.example.demo.entity.Hero;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.PlayerStatsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.support.PlayerCardDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class BanPickHistoryService {

    private static final int RECENT_HISTORY_LIMIT = 50;
    private static final int LEADERBOARD_LIMIT = 50;

    private final DraftHistoryRepository draftHistoryRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final UserRepository userRepository;
    private final HeroRepository heroRepository;
    private final BanPickRankService banPickRankService;
    private final BanPickMacroEconomyService banPickMacroEconomyService;

    public BanPickHistoryService(DraftHistoryRepository draftHistoryRepository,
                                 PlayerStatsRepository playerStatsRepository,
                                 UserRepository userRepository,
                                 HeroRepository heroRepository,
                                 BanPickRankService banPickRankService,
                                 BanPickMacroEconomyService banPickMacroEconomyService) {
        this.draftHistoryRepository = draftHistoryRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.userRepository = userRepository;
        this.heroRepository = heroRepository;
        this.banPickRankService = banPickRankService;
        this.banPickMacroEconomyService = banPickMacroEconomyService;
    }

    @Transactional
    public DraftHistory recordFinishedDraft(BanPickRoom room, List<BanPickAction> actions) {
        return recordFinishedDraft(room, actions, null, DraftHistoryEndReason.NORMAL, null);
    }

    @Transactional
    public DraftHistory recordFinishedDraft(BanPickRoom room,
                                           List<BanPickAction> actions,
                                           User forcedWinner,
                                           DraftHistoryEndReason endReason,
                                           User dodgedUser) {
        if (room.getBlueUser() == null || room.getRedUser() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot save draft history without assigned sides");
        }

        Optional<DraftHistory> existingHistory = findRecordedHistoryForCurrentFinishedState(room);
        if (existingHistory.isPresent()) {
            return existingHistory.get();
        }

        List<Long> bluePickIds = finalPickHeroIds(room, actions, BanPickTeamSide.BLUE);
        List<Long> redPickIds = finalPickHeroIds(room, actions, BanPickTeamSide.RED);
        Map<Long, Hero> heroLookup = loadHeroesById(actions, bluePickIds, redPickIds);
        List<String> bluePicks = heroNames(bluePickIds, heroLookup);
        List<String> redPicks = heroNames(redPickIds, heroLookup);
        List<String> blueBans = heroNames(actions, BanPickTeamSide.BLUE, BanPickActionType.BAN, heroLookup);
        List<String> redBans = heroNames(actions, BanPickTeamSide.RED, BanPickActionType.BAN, heroLookup);
        LocalDateTime resultRecordedAt = LocalDateTime.now();
        DraftResultEvaluation evaluation = evaluateDraftResult(bluePickIds, redPickIds, heroLookup);
        BanPickTeamSide winnerSide = resolveWinnerSide(room, forcedWinner, evaluation);
        User winnerUser = winnerForSide(room, winnerSide);
        BanPickRatingRules.RatingDeltaSnapshot ratingDeltaSnapshot = resolveCurrentRatingDeltas(room, winnerSide);
        if (winnerSide != null && shouldBlockPairRating(room.getBlueUser(), room.getRedUser(), resultRecordedAt)) {
            ratingDeltaSnapshot = BanPickRatingRules.noRatingChange();
        }

        DraftHistory history = new DraftHistory();
        history.setRoomCode(room.getRoomCode());
        history.setBlueUser(room.getBlueUser());
        history.setRedUser(room.getRedUser());
        history.setWinnerUser(winnerUser);
        history.setDodgedUser(resolveDodgedUser(room, dodgedUser));
        history.setEndReason(endReason != null ? endReason : DraftHistoryEndReason.NORMAL);
        history.setBluePicks(serializeList(bluePicks));
        history.setRedPicks(serializeList(redPicks));
        history.setBlueBans(serializeList(blueBans));
        history.setRedBans(serializeList(redBans));
        history.setResultRecordedAt(resultRecordedAt);
        history.setWinRatingDelta(ratingDeltaSnapshot.winDelta());
        history.setLossRatingDelta(ratingDeltaSnapshot.lossDelta());

        DraftHistory savedHistory = draftHistoryRepository.save(history);
        synchronizeRecentHistoryStats(room.getBlueUser(), room.getRedUser());
        return savedHistory;
    }

    @Transactional(readOnly = true)
    public List<DraftHistoryResponse> getCurrentUserHistory(GoogleUserPrincipal principal) {
        User user = findUser(principal);
        return findRecentHistories(user).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DraftHistoryResponse getHistory(Long id) {
        return toHistoryResponse(findHistory(id));
    }

    @Transactional
    public DraftHistoryResponse recordWinner(Long historyId,
                                             RecordDraftWinnerRequest request,
                                             GoogleUserPrincipal principal) {
        throw new ResponseStatusException(HttpStatus.GONE, "Winner is calculated automatically from Ban/Pick score.");
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> getLeaderboard() {
        List<PlayerStats> rankedStats = playerStatsRepository.findAll().stream()
                .filter(this::isRankedLeaderboardPlayer)
                .sorted(this::compareLeaderboardPlayers)
                .toList();
        Map<Long, BanPickRankService.RankProfile> rankProfilesByUserId = banPickRankService.resolveRanks(rankedStats);
        return rankedStats.stream()
                .limit(LEADERBOARD_LIMIT)
                .map(stats -> toStatsResponse(stats, rankProfilesByUserId.get(userIdOf(stats))))
                .toList();
    }

    @Transactional(readOnly = true)
    public BanPickProfileResponse getProfile(GoogleUserPrincipal principal) {
        User user = findUser(principal);
        List<DraftHistory> recentHistories = findRecentHistories(user);
        PlayerStats statsSnapshot = buildStatsSnapshot(user, recentHistories);
        BanPickRankService.RankProfile rankProfile = banPickRankService.resolveRank(
                statsSnapshot,
                playerStatsRepository.findAll()
        );
        return new BanPickProfileResponse(
                toUserSummary(user),
                toStatsResponse(statsSnapshot, rankProfile),
                toPlayerCardResponse(user, statsSnapshot, rankProfile),
                recentHistories.stream().map(this::toHistoryResponse).toList()
        );
    }

    private boolean isRankedLeaderboardPlayer(PlayerStats stats) {
        return stats != null && userIdOf(stats) != null && safeInt(stats.getTotalMatches()) > 0;
    }

    private int compareLeaderboardPlayers(PlayerStats left, PlayerStats right) {
        int ratingCompare = Integer.compare(
                safeRating(right != null ? right.getRating() : null),
                safeRating(left != null ? left.getRating() : null)
        );
        if (ratingCompare != 0) {
            return ratingCompare;
        }

        int totalMatchesCompare = Integer.compare(
                safeInt(right != null ? right.getTotalMatches() : null),
                safeInt(left != null ? left.getTotalMatches() : null)
        );
        if (totalMatchesCompare != 0) {
            return totalMatchesCompare;
        }

        return Long.compare(userIdOrMax(left), userIdOrMax(right));
    }

    private long userIdOrMax(PlayerStats stats) {
        Long userId = userIdOf(stats);
        return userId != null ? userId : Long.MAX_VALUE;
    }

    private Long userIdOf(PlayerStats stats) {
        if (stats == null || stats.getUser() == null) {
            return null;
        }
        return stats.getUser().getId();
    }

    @Transactional(readOnly = true)
    public Optional<Long> findLatestHistoryIdByRoomCode(String roomCode) {
        return draftHistoryRepository.findFirstByRoomCodeOrderByCreatedAtDesc(roomCode)
                .map(DraftHistory::getId);
    }

    private Optional<DraftHistory> findRecordedHistoryForCurrentFinishedState(BanPickRoom room) {
        if (room == null || room.getRoomCode() == null || room.getRoomCode().isBlank()) {
            return Optional.empty();
        }
        Optional<DraftHistory> latestHistory = draftHistoryRepository.findFirstByRoomCodeOrderByCreatedAtDesc(room.getRoomCode());
        if (latestHistory.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime roomFinishedAt = room.getUpdatedAt();
        LocalDateTime historyCreatedAt = latestHistory.get().getCreatedAt();
        if (roomFinishedAt == null || historyCreatedAt == null) {
            return Optional.empty();
        }
        return historyCreatedAt.isBefore(roomFinishedAt) ? Optional.empty() : latestHistory;
    }

    private void synchronizeRecentHistoryStats(User... users) {
        Map<Long, User> usersById = uniqueUsers(users);
        if (usersById.isEmpty()) {
            return;
        }

        Set<Long> deletableHistoryIds = findDeletableOverflowHistoryIds(usersById.values());
        if (!deletableHistoryIds.isEmpty()) {
            draftHistoryRepository.deleteAllByIdInBatch(deletableHistoryIds);
        }

        usersById.values().forEach(this::rebuildStatsForUser);
    }

    private Map<Long, User> uniqueUsers(User... users) {
        Map<Long, User> usersById = new LinkedHashMap<>();
        if (users == null) {
            return usersById;
        }
        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            usersById.putIfAbsent(user.getId(), user);
        }
        return usersById;
    }

    private Set<Long> findDeletableOverflowHistoryIds(Iterable<User> users) {
        Map<Long, List<DraftHistory>> historyCache = new LinkedHashMap<>();
        Map<Long, DraftHistory> overflowCandidates = new LinkedHashMap<>();

        for (User user : users) {
            List<DraftHistory> orderedHistories = loadOrderedHistories(user, historyCache);
            if (orderedHistories.size() <= RECENT_HISTORY_LIMIT) {
                continue;
            }
            for (DraftHistory history : orderedHistories.subList(RECENT_HISTORY_LIMIT, orderedHistories.size())) {
                if (history.getId() != null) {
                    overflowCandidates.putIfAbsent(history.getId(), history);
                }
            }
        }

        Set<Long> deletableHistoryIds = new LinkedHashSet<>();
        for (DraftHistory history : overflowCandidates.values()) {
            if (history == null || history.getId() == null) {
                continue;
            }
            if (isOutsideRecentHistoryLimit(history, history.getBlueUser(), historyCache)
                    && isOutsideRecentHistoryLimit(history, history.getRedUser(), historyCache)) {
                deletableHistoryIds.add(history.getId());
            }
        }
        return deletableHistoryIds;
    }

    private boolean isOutsideRecentHistoryLimit(DraftHistory history,
                                                User user,
                                                Map<Long, List<DraftHistory>> historyCache) {
        if (history == null || history.getId() == null || user == null || user.getId() == null) {
            return false;
        }

        List<DraftHistory> orderedHistories = loadOrderedHistories(user, historyCache);
        for (int index = 0; index < orderedHistories.size(); index++) {
            if (Objects.equals(orderedHistories.get(index).getId(), history.getId())) {
                return index >= RECENT_HISTORY_LIMIT;
            }
        }
        return true;
    }

    private List<DraftHistory> loadOrderedHistories(User user, Map<Long, List<DraftHistory>> historyCache) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return historyCache.computeIfAbsent(
                user.getId(),
                ignored -> draftHistoryRepository.findByParticipantOrderByRecentDesc(user)
        );
    }

    private List<DraftHistory> findRecentHistories(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return draftHistoryRepository.findRecentByParticipantOrderByRecentDesc(
                user,
                PageRequest.of(0, RECENT_HISTORY_LIMIT)
        );
    }

    private void rebuildStatsForUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        List<DraftHistory> recentHistories = findRecentHistories(user);
        Optional<PlayerStats> existingStats = playerStatsRepository.findByUser(user);
        if (recentHistories.isEmpty()) {
            existingStats.ifPresent(playerStatsRepository::delete);
            return;
        }

        PlayerStats snapshot = buildStatsSnapshot(user, recentHistories);
        PlayerStats stats = existingStats.orElseGet(() -> {
            PlayerStats newStats = new PlayerStats();
            newStats.setUser(user);
            return newStats;
        });
        stats.setTotalMatches(snapshot.getTotalMatches());
        stats.setWins(snapshot.getWins());
        stats.setLosses(snapshot.getLosses());
        stats.setRating(snapshot.getRating());
        stats.setPickedHeroCounts(snapshot.getPickedHeroCounts());
        playerStatsRepository.save(stats);
    }

    private PlayerStats buildStatsSnapshot(User user, List<DraftHistory> recentHistories) {
        PlayerStats stats = new PlayerStats();
        stats.setUser(user);

        int totalMatches = 0;
        int wins = 0;
        int losses = 0;
        Map<String, Integer> pickCounts = new LinkedHashMap<>();

        for (DraftHistory history : recentHistories) {
            totalMatches += 1;
            if (isSameUser(history.getWinnerUser(), user)) {
                wins += 1;
            } else if (history.getWinnerUser() != null) {
                losses += 1;
            }

            for (String pick : picksForUser(history, user)) {
                if (pick == null || pick.isBlank()) {
                    continue;
                }
                pickCounts.merge(pick, 1, Integer::sum);
            }
        }

        stats.setTotalMatches(totalMatches);
        stats.setWins(wins);
        stats.setLosses(losses);
        stats.setRating(rebuildRatingFromRecentHistories(user, recentHistories));
        stats.setPickedHeroCounts(serializePickCounts(pickCounts));
        return stats;
    }

    private int rebuildRatingFromRecentHistories(User user, List<DraftHistory> recentHistories) {
        int rating = BanPickRatingRules.INITIAL_RATING;
        if (user == null || recentHistories == null || recentHistories.isEmpty()) {
            return rating;
        }

        for (int index = recentHistories.size() - 1; index >= 0; index -= 1) {
            DraftHistory history = recentHistories.get(index);
            if (history == null || history.getWinnerUser() == null) {
                continue;
            }
            if (isSameUser(history.getWinnerUser(), user)) {
                rating += resolveWinDelta(history);
                continue;
            }
            rating = Math.max(BanPickRatingRules.MIN_RATING, rating + resolveLossDelta(history));
        }
        return Math.max(BanPickRatingRules.MIN_RATING, rating);
    }

    private BanPickRatingRules.RatingDeltaSnapshot resolveCurrentRatingDeltas(BanPickRoom room,
                                                                              BanPickTeamSide winnerSide) {
        if (room == null || winnerSide == null) {
            return BanPickRatingRules.noRatingChange();
        }

        int currentWinDelta = banPickMacroEconomyService.getCurrentWinDelta();
        User winner = winnerForSide(room, winnerSide);
        User loser = winnerSide == BanPickTeamSide.BLUE ? room.getRedUser() : room.getBlueUser();
        return BanPickRatingRules.applyGapModifier(
                currentRatingBeforeMatch(winner),
                currentRatingBeforeMatch(loser),
                currentWinDelta,
                BanPickRatingRules.BASE_LOSS_DELTA
        );
    }

    private BanPickTeamSide resolveWinnerSide(BanPickRoom room,
                                              User forcedWinner,
                                              DraftResultEvaluation evaluation) {
        if (forcedWinner == null) {
            return evaluation != null ? evaluation.winnerSide() : null;
        }
        if (isSameUser(forcedWinner, room.getBlueUser())) {
            return BanPickTeamSide.BLUE;
        }
        if (isSameUser(forcedWinner, room.getRedUser())) {
            return BanPickTeamSide.RED;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Forced winner must belong to the room");
    }

    private User resolveDodgedUser(BanPickRoom room, User dodgedUser) {
        if (dodgedUser == null) {
            return null;
        }
        if (isSameUser(dodgedUser, room.getBlueUser())) {
            return room.getBlueUser();
        }
        if (isSameUser(dodgedUser, room.getRedUser())) {
            return room.getRedUser();
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Dodged user must belong to the room");
    }

    private User winnerForSide(BanPickRoom room, BanPickTeamSide winnerSide) {
        if (room == null || winnerSide == null) {
            return null;
        }
        return winnerSide == BanPickTeamSide.BLUE ? room.getBlueUser() : room.getRedUser();
    }

    private int currentRatingBeforeMatch(User user) {
        if (user == null || user.getId() == null) {
            return BanPickRatingRules.INITIAL_RATING;
        }

        Optional<PlayerStats> existingStats = playerStatsRepository.findByUser(user);
        if (existingStats.isPresent()) {
            return safeRating(existingStats.get().getRating());
        }

        List<DraftHistory> recentHistories = findRecentHistories(user);
        if (recentHistories.isEmpty()) {
            return BanPickRatingRules.INITIAL_RATING;
        }
        return rebuildRatingFromRecentHistories(user, recentHistories);
    }

    private boolean shouldBlockPairRating(User firstUser, User secondUser, LocalDateTime resultRecordedAt) {
        if (firstUser == null || secondUser == null || firstUser.getId() == null || secondUser.getId() == null
                || Objects.equals(firstUser.getId(), secondUser.getId()) || resultRecordedAt == null) {
            return false;
        }

        long lowerUserId = Math.min(firstUser.getId(), secondUser.getId());
        long higherUserId = Math.max(firstUser.getId(), secondUser.getId());
        LocalDateTime windowStart = resultRecordedAt.minusHours(BanPickRatingRules.ANTI_WIN_TRADING_RESET_HOURS);
        long recentPairMatches = draftHistoryRepository.countCompletedPairMatchesWithinWindow(
                lowerUserId,
                higherUserId,
                windowStart,
                resultRecordedAt
        );
        return recentPairMatches >= BanPickRatingRules.MAX_RATED_PAIR_MATCHES_PER_48H;
    }

    private int resolveWinDelta(DraftHistory history) {
        if (history == null || history.getWinRatingDelta() == null) {
            return BanPickRatingRules.BASE_WIN_DELTA;
        }
        return Math.max(0, history.getWinRatingDelta());
    }

    private int resolveLossDelta(DraftHistory history) {
        if (history == null || history.getLossRatingDelta() == null) {
            return BanPickRatingRules.BASE_LOSS_DELTA;
        }
        return Math.min(0, history.getLossRatingDelta());
    }

    private List<String> picksForUser(DraftHistory history, User user) {
        if (history == null || user == null) {
            return List.of();
        }
        if (isSameUser(history.getBlueUser(), user)) {
            return deserializeList(history.getBluePicks());
        }
        if (isSameUser(history.getRedUser(), user)) {
            return deserializeList(history.getRedPicks());
        }
        return List.of();
    }

    private DraftHistory findHistory(Long id) {
        return draftHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft history not found"));
    }

    private User findUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
    }

    private List<Long> finalPickHeroIds(BanPickRoom room,
                                        List<BanPickAction> actions,
                                        BanPickTeamSide teamSide) {
        List<Long> orderedHeroIds = parseHeroIdList(teamSide == BanPickTeamSide.BLUE
                ? room.getBluePickOrder()
                : room.getRedPickOrder());
        if (!orderedHeroIds.isEmpty()) {
            return orderedHeroIds;
        }
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        return actions.stream()
                .filter(action -> action.getTeamSide() == teamSide && action.getActionType() == BanPickActionType.PICK)
                .sorted(Comparator.comparing(BanPickAction::getConfirmedAt))
                .map(BanPickAction::getHeroId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, Hero> loadHeroesById(List<BanPickAction> actions,
                                           List<Long> bluePickIds,
                                           List<Long> redPickIds) {
        LinkedHashSet<Long> heroIds = new LinkedHashSet<>();
        heroIds.addAll(bluePickIds);
        heroIds.addAll(redPickIds);
        if (actions != null) {
            actions.stream()
                    .map(BanPickAction::getHeroId)
                    .filter(Objects::nonNull)
                    .forEach(heroIds::add);
        }

        Map<Long, Hero> heroLookup = new LinkedHashMap<>();
        heroRepository.findAllById(heroIds).forEach(hero -> heroLookup.put(hero.getId(), hero));
        return heroLookup;
    }

    private List<String> heroNames(List<Long> heroIds, Map<Long, Hero> heroLookup) {
        if (heroIds == null || heroIds.isEmpty()) {
            return List.of();
        }
        return heroIds.stream()
                .map(heroId -> heroName(heroId, heroLookup))
                .toList();
    }

    private List<String> heroNames(List<BanPickAction> actions,
                                   BanPickTeamSide teamSide,
                                   BanPickActionType actionType,
                                   Map<Long, Hero> heroLookup) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        return actions.stream()
                .filter(action -> action.getTeamSide() == teamSide && action.getActionType() == actionType)
                .sorted(Comparator.comparing(BanPickAction::getConfirmedAt))
                .map(action -> heroName(action.getHeroId(), heroLookup))
                .toList();
    }

    private String heroName(Long heroId, Map<Long, Hero> heroLookup) {
        Hero hero = heroLookup.get(heroId);
        return hero != null ? hero.getName() : "Hero #" + heroId;
    }

    private DraftResultEvaluation evaluateDraftResult(List<Long> bluePickIds,
                                                      List<Long> redPickIds,
                                                      Map<Long, Hero> heroLookup) {
        BigDecimal blueScore = calculateTeamBanPickScore(bluePickIds, heroLookup);
        BigDecimal redScore = calculateTeamBanPickScore(redPickIds, heroLookup);
        if (blueScore.compareTo(redScore) > 0) {
            return new DraftResultEvaluation(blueScore, redScore, BanPickTeamSide.BLUE);
        }
        if (redScore.compareTo(blueScore) > 0) {
            return new DraftResultEvaluation(blueScore, redScore, BanPickTeamSide.RED);
        }
        return new DraftResultEvaluation(blueScore, redScore, null);
    }

    private BigDecimal calculateTeamBanPickScore(List<Long> heroIds, Map<Long, Hero> heroLookup) {
        BigDecimal totalScore = BigDecimal.ZERO;
        if (heroIds == null || heroIds.isEmpty()) {
            return totalScore.setScale(2, RoundingMode.HALF_UP);
        }
        for (Long heroId : heroIds) {
            Hero hero = heroLookup.get(heroId);
            totalScore = totalScore.add(sanitizeBanPickScore(hero != null ? hero.getBanPickScore() : null));
        }
        return sanitizeBanPickScore(totalScore);
    }

    private BigDecimal sanitizeBanPickScore(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private DraftHistoryResponse toHistoryResponse(DraftHistory history) {
        return new DraftHistoryResponse(
                history.getId(),
                history.getRoomCode(),
                toUserSummary(history.getBlueUser()),
                toUserSummary(history.getRedUser()),
                toUserSummary(history.getWinnerUser()),
                winnerSide(history),
                toUserSummary(history.getDodgedUser()),
                history.getEndReason(),
                deserializeList(history.getBluePicks()),
                deserializeList(history.getRedPicks()),
                deserializeList(history.getBlueBans()),
                deserializeList(history.getRedBans()),
                history.getCreatedAt(),
                history.getResultRecordedAt()
        );
    }

    private PlayerStatsResponse toStatsResponse(PlayerStats stats, BanPickRankService.RankProfile rankProfile) {
        int totalMatches = safeInt(stats.getTotalMatches());
        int wins = safeInt(stats.getWins());
        int losses = safeInt(stats.getLosses());
        int decidedMatches = wins + losses;
        double winRate = decidedMatches == 0 ? 0.0 : Math.round((wins * 10000.0 / decidedMatches)) / 100.0;
        BanPickRankService.RankProfile safeRankProfile = rankProfile != null ? rankProfile : banPickRankService.unranked();
        return new PlayerStatsResponse(
                toUserSummary(stats.getUser()),
                totalMatches,
                wins,
                losses,
                winRate,
                safeRating(stats.getRating()),
                safeRankProfile.code(),
                safeRankProfile.label(),
                mostPickedHeroes(stats.getPickedHeroCounts())
        );
    }

    private BanPickPlayerCardResponse toPlayerCardResponse(User user,
                                                           PlayerStats stats,
                                                           BanPickRankService.RankProfile rankProfile) {
        BanPickRankService.RankProfile safeRankProfile = rankProfile != null ? rankProfile : banPickRankService.unranked();
        return new BanPickPlayerCardResponse(
                user != null ? user.getAvatarUrl() : null,
                user != null ? user.resolveDisplayName() : "User",
                safeRating(stats != null ? stats.getRating() : null),
                safeRankProfile.code(),
                safeRankProfile.label(),
                user != null ? user.resolvePlayerBadgeCode() : PlayerCardDefaults.DEFAULT_BADGE_CODE,
                user != null ? user.resolvePlayerBadgeName() : PlayerCardDefaults.DEFAULT_BADGE_NAME,
                user != null ? user.resolvePlayerBadgeIconUrl() : null,
                user != null ? user.resolvePlayerTitle() : PlayerCardDefaults.DEFAULT_TITLE
        );
    }

    private BanPickTeamSide winnerSide(DraftHistory history) {
        if (history.getWinnerUser() == null) {
            return null;
        }
        if (isSameUser(history.getWinnerUser(), history.getBlueUser())) {
            return BanPickTeamSide.BLUE;
        }
        if (isSameUser(history.getWinnerUser(), history.getRedUser())) {
            return BanPickTeamSide.RED;
        }
        return null;
    }

    private BanPickUserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return new BanPickUserSummary(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                user.getAvatarUrl()
        );
    }

    private List<HeroPickStatResponse> mostPickedHeroes(String value) {
        return parsePickCounts(value).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new HeroPickStatResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Integer> parsePickCounts(String value) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : deserializeList(value)) {
            int separator = line.lastIndexOf('\t');
            if (separator <= 0 || separator >= line.length() - 1) {
                continue;
            }
            String heroName = line.substring(0, separator);
            int count;
            try {
                count = Integer.parseInt(line.substring(separator + 1));
            } catch (NumberFormatException ex) {
                continue;
            }
            counts.put(heroName, count);
        }
        return counts;
    }

    private String serializePickCounts(Map<String, Integer> counts) {
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "\t" + entry.getValue())
                .reduce((first, second) -> first + "\n" + second)
                .orElse("");
    }

    private List<Long> parseHeroIdList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> heroIds = new ArrayList<>();
        for (String token : value.split(",")) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                heroIds.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // Keep valid ids and ignore malformed entries.
            }
        }
        return heroIds;
    }

    private String serializeList(List<String> values) {
        return String.join("\n", values);
    }

    private List<String> deserializeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : value.split("\\R")) {
            if (!item.isBlank()) {
                values.add(item.trim());
            }
        }
        return values;
    }

    private boolean isSameUser(User first, User second) {
        return first != null && second != null && Objects.equals(first.getId(), second.getId());
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private int safeRating(Integer value) {
        return value != null
                ? Math.max(BanPickRatingRules.MIN_RATING, value)
                : BanPickRatingRules.INITIAL_RATING;
    }

    private record DraftResultEvaluation(
            BigDecimal blueScore,
            BigDecimal redScore,
            BanPickTeamSide winnerSide
    ) {
    }
}
