package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsDraftTournamentAggregate;
import com.example.demo.dto.esports.EsportsHeroBanBreakdownAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroPickStatAggregate;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.BanPickActionType;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsMatchDraftAction;
import com.example.demo.entity.EsportsMatchGame;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import com.example.demo.repository.EsportsMatchGameRepository;
import com.example.demo.util.EsportsTournamentCatalog;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class EsportsDataService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final int DASHBOARD_BAN_LIMIT = 5;
    private static final int DASHBOARD_TOP_TEAM_LIMIT = 5;
    private static final int DASHBOARD_HERO_INSIGHT_LIMIT = 5;
    private static final int MIN_HERO_PICK_SAMPLE = 2;
    private static final int MIN_TEAM_SAMPLE = 2;
    private static final int REQUIRED_PICKS_PER_SIDE = 5;

    private final EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;
    private final EsportsMatchGameRepository esportsMatchGameRepository;

    public EsportsDataService(EsportsMatchDraftActionRepository esportsMatchDraftActionRepository,
                              EsportsMatchGameRepository esportsMatchGameRepository) {
        this.esportsMatchDraftActionRepository = esportsMatchDraftActionRepository;
        this.esportsMatchGameRepository = esportsMatchGameRepository;
    }

    public List<EsportsTournamentOptionResponse> getAvailableTournaments() {
        Map<String, EsportsTournamentOptionResponse> uniqueTournaments = new LinkedHashMap<>();

        esportsMatchDraftActionRepository.findDraftTournamentsOrderByLatestMatchDesc()
                .forEach(tournament -> {
                    String tournamentTier = tournament.tournamentTier();
                    String tournamentName = EsportsTournamentCatalog.resolveTournamentName(tournamentTier);
                    uniqueTournaments.putIfAbsent(
                            tournamentTier,
                            new EsportsTournamentOptionResponse(tournamentName, tournamentTier)
                    );
                });

        return uniqueTournaments.values().stream().toList();
    }

    public List<EsportsHeroBanStatResponse> getTopBannedHeroes(String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentName, limit, null);
    }

    public List<EsportsHeroBanStatResponse> getTopBlueBannedHeroes(String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentName, limit, BanPickTeamSide.BLUE);
    }

    public List<EsportsHeroStatResponse> getHeroStats(String tournamentName) {
        String tournamentTier = resolveTournamentTier(tournamentName);

        Map<Long, AggregateHeroStatAccumulator> statsByHeroId = new LinkedHashMap<>();

        esportsMatchDraftActionRepository.findHeroPickStats(tournamentTier)
                .forEach(item -> statsByHeroId
                        .computeIfAbsent(item.heroId(), ignored -> new AggregateHeroStatAccumulator(item.heroId()))
                        .applyPickStats(item));

        esportsMatchDraftActionRepository.findHeroBanStats(tournamentTier)
                .forEach(item -> statsByHeroId
                        .computeIfAbsent(item.heroId(), ignored -> new AggregateHeroStatAccumulator(item.heroId()))
                        .applyBanStats(item));

        return statsByHeroId.values().stream()
                .map(AggregateHeroStatAccumulator::toResponse)
                .sorted(heroStatComparator())
                .toList();
    }

    public EsportsDashboardResponse getDashboard(String tournamentName,
                                                 String teamCode,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo) {
        AnalyticsFilter filter = resolveAnalyticsFilter(tournamentName, teamCode, dateFrom, dateTo);
        List<EsportsMatchGame> filteredGames = esportsMatchGameRepository.findAllForAnalytics(
                filter.tournamentTier(),
                filter.teamCode(),
                filter.dateFrom(),
                filter.dateTo()
        );
        List<EsportsMatchDraftAction> filteredActions = esportsMatchDraftActionRepository.findAllForAnalytics(
                filter.tournamentTier(),
                filter.teamCode(),
                filter.dateFrom(),
                filter.dateTo()
        );

        List<EsportsHeroStatResponse> heroStats = buildDashboardHeroStats(filteredActions);
        EsportsDashboardResponse.SideAdvantage sideAdvantage = buildSideAdvantage(filteredGames);
        DraftAccuracySnapshot draftAccuracy = buildDraftAccuracy(filteredGames, filteredActions);

        return new EsportsDashboardResponse(
                buildSummary(filteredGames, heroStats, sideAdvantage, draftAccuracy),
                buildMatchActivity(filteredGames),
                sideAdvantage,
                buildHeroInsights(heroStats, true),
                buildHeroInsights(heroStats, false),
                buildTopTeams(filteredGames),
                heroStats,
                buildTopBannedHeroStats(filteredActions, filter.resolvedTournamentName(), DASHBOARD_BAN_LIMIT, null),
                buildTopBannedHeroStats(filteredActions, filter.resolvedTournamentName(), 1, BanPickTeamSide.BLUE)
                        .stream()
                        .findFirst()
                        .orElse(null),
                buildTeamOptions(filter.withoutTeamCode())
        );
    }

    private List<EsportsHeroBanStatResponse> getTopHeroBanStats(String tournamentName,
                                                                Integer limit,
                                                                BanPickTeamSide teamSide) {
        String tournamentTier = resolveTournamentTier(tournamentName);
        String resolvedTournamentName = tournamentTier != null
                ? EsportsTournamentCatalog.resolveTournamentName(tournamentTier)
                : null;

        return esportsMatchDraftActionRepository.findTopHeroBanStats(
                        tournamentTier,
                        teamSide,
                        PageRequest.of(0, sanitizeLimit(limit))
                ).stream()
                .map(item -> new EsportsHeroBanStatResponse(
                        item.heroId(),
                        item.heroName(),
                        item.heroAvatarUrl(),
                        item.banCount(),
                        resolvedTournamentName
                ))
                .toList();
    }

    private AnalyticsFilter resolveAnalyticsFilter(String tournamentName,
                                                   String teamCode,
                                                   LocalDate dateFrom,
                                                   LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("date range khong hop le.");
        }

        String tournamentTier = resolveTournamentTier(tournamentName);
        String resolvedTournamentName = tournamentTier != null
                ? EsportsTournamentCatalog.resolveTournamentName(tournamentTier)
                : null;

        return new AnalyticsFilter(
                tournamentTier,
                normalizeTeamCode(teamCode),
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? LocalDateTime.of(dateTo, LocalTime.MAX) : null,
                resolvedTournamentName
        );
    }

    private List<EsportsHeroStatResponse> buildDashboardHeroStats(List<EsportsMatchDraftAction> actions) {
        Map<Long, ActionHeroStatAccumulator> statsByHeroId = new LinkedHashMap<>();

        for (EsportsMatchDraftAction action : actions) {
            Hero hero = action.getHero();
            Long heroId = hero != null ? hero.getId() : null;
            if (heroId == null) {
                continue;
            }

            ActionHeroStatAccumulator accumulator = statsByHeroId.computeIfAbsent(
                    heroId,
                    ignored -> new ActionHeroStatAccumulator(heroId)
            );
            accumulator.apply(action);
        }

        return statsByHeroId.values().stream()
                .map(ActionHeroStatAccumulator::toResponse)
                .sorted(heroStatComparator())
                .toList();
    }

    private List<EsportsHeroBanStatResponse> buildTopBannedHeroStats(List<EsportsMatchDraftAction> actions,
                                                                     String tournamentName,
                                                                     int limit,
                                                                     BanPickTeamSide teamSide) {
        Map<Long, HeroBanAccumulator> bansByHeroId = new LinkedHashMap<>();

        for (EsportsMatchDraftAction action : actions) {
            if (action.getActionType() != BanPickActionType.BAN) {
                continue;
            }
            if (teamSide != null && action.getTeamSide() != teamSide) {
                continue;
            }

            Hero hero = action.getHero();
            Long heroId = hero != null ? hero.getId() : null;
            if (heroId == null) {
                continue;
            }

            bansByHeroId.computeIfAbsent(heroId, ignored -> new HeroBanAccumulator(heroId))
                    .apply(action);
        }

        return bansByHeroId.values().stream()
                .map(item -> new EsportsHeroBanStatResponse(
                        item.heroId,
                        item.heroName,
                        item.heroAvatarUrl,
                        item.banCount,
                        tournamentName
                ))
                .sorted(Comparator
                        .comparing(EsportsHeroBanStatResponse::banCount, Comparator.reverseOrder())
                        .thenComparing(response -> safeText(response.heroName())))
                .limit(limit)
                .toList();
    }

    private EsportsDashboardResponse.Summary buildSummary(List<EsportsMatchGame> games,
                                                          List<EsportsHeroStatResponse> heroStats,
                                                          EsportsDashboardResponse.SideAdvantage sideAdvantage,
                                                          DraftAccuracySnapshot draftAccuracy) {
        Set<Long> distinctMatchIds = new HashSet<>();
        for (EsportsMatchGame game : games) {
            if (game.getMatch() != null && game.getMatch().getId() != null) {
                distinctMatchIds.add(game.getMatch().getId());
            }
        }

        return new EsportsDashboardResponse.Summary(
                (long) distinctMatchIds.size(),
                (long) games.size(),
                (long) heroStats.size(),
                sideAdvantage.blueWinRate(),
                draftAccuracy.accuracy(),
                sideAdvantage.completedGames(),
                draftAccuracy.sampleSize()
        );
    }

    private List<EsportsDashboardResponse.ActivityPoint> buildMatchActivity(List<EsportsMatchGame> games) {
        Map<LocalDate, ActivityAccumulator> activityByDate = new TreeMap<>();

        for (EsportsMatchGame game : games) {
            if (game.getMatch() == null || game.getMatch().getMatchDate() == null) {
                continue;
            }

            LocalDate activityDate = game.getMatch().getMatchDate().toLocalDate();
            activityByDate.computeIfAbsent(activityDate, ignored -> new ActivityAccumulator())
                    .apply(game);
        }

        List<EsportsDashboardResponse.ActivityPoint> points = new ArrayList<>();
        activityByDate.forEach((activityDate, accumulator) -> points.add(
                new EsportsDashboardResponse.ActivityPoint(
                        activityDate,
                        (long) accumulator.matchIds.size(),
                        accumulator.gameCount
                )
        ));
        return points;
    }

    private EsportsDashboardResponse.SideAdvantage buildSideAdvantage(List<EsportsMatchGame> games) {
        long blueWins = 0L;
        long redWins = 0L;

        for (EsportsMatchGame game : games) {
            Long winnerTeamId = safeEntityId(game.getWinnerTeam());
            if (winnerTeamId == null) {
                continue;
            }

            Long blueTeamId = safeEntityId(game.getBlueTeam());
            Long redTeamId = safeEntityId(game.getRedTeam());
            if (winnerTeamId.equals(blueTeamId)) {
                blueWins++;
            } else if (winnerTeamId.equals(redTeamId)) {
                redWins++;
            }
        }

        long completedGames = blueWins + redWins;
        return new EsportsDashboardResponse.SideAdvantage(
                blueWins,
                redWins,
                completedGames,
                calculateWinRate(blueWins, completedGames),
                calculateWinRate(redWins, completedGames)
        );
    }

    private DraftAccuracySnapshot buildDraftAccuracy(List<EsportsMatchGame> games,
                                                     List<EsportsMatchDraftAction> actions) {
        Map<Long, List<EsportsMatchDraftAction>> actionsByGameId = new LinkedHashMap<>();
        for (EsportsMatchDraftAction action : actions) {
            Long gameId = action.getGame() != null ? action.getGame().getId() : null;
            if (gameId == null) {
                continue;
            }
            actionsByGameId.computeIfAbsent(gameId, ignored -> new ArrayList<>()).add(action);
        }

        long sampleSize = 0L;
        long correctPredictions = 0L;

        for (EsportsMatchGame game : games) {
            Long winnerTeamId = safeEntityId(game.getWinnerTeam());
            if (winnerTeamId == null) {
                continue;
            }

            List<EsportsMatchDraftAction> gameActions = actionsByGameId.getOrDefault(game.getId(), List.of());
            double blueScore = 0D;
            double redScore = 0D;
            int bluePickCount = 0;
            int redPickCount = 0;
            boolean missingHeroScore = false;

            for (EsportsMatchDraftAction action : gameActions) {
                if (action.getActionType() != BanPickActionType.PICK) {
                    continue;
                }

                Double heroScore = readHeroScore(action.getHero());
                if (heroScore == null) {
                    missingHeroScore = true;
                    continue;
                }

                if (action.getTeamSide() == BanPickTeamSide.BLUE) {
                    blueScore += heroScore;
                    bluePickCount++;
                } else if (action.getTeamSide() == BanPickTeamSide.RED) {
                    redScore += heroScore;
                    redPickCount++;
                }
            }

            if (missingHeroScore
                    || bluePickCount < REQUIRED_PICKS_PER_SIDE
                    || redPickCount < REQUIRED_PICKS_PER_SIDE
                    || Double.compare(blueScore, redScore) == 0) {
                continue;
            }

            Long predictedWinnerId = blueScore > redScore
                    ? safeEntityId(game.getBlueTeam())
                    : safeEntityId(game.getRedTeam());
            if (predictedWinnerId == null) {
                continue;
            }

            sampleSize++;
            if (predictedWinnerId.equals(winnerTeamId)) {
                correctPredictions++;
            }
        }

        if (sampleSize == 0L) {
            return new DraftAccuracySnapshot(null, 0L);
        }

        return new DraftAccuracySnapshot(calculateWinRate(correctPredictions, sampleSize), sampleSize);
    }

    private List<EsportsDashboardResponse.HeroInsight> buildHeroInsights(List<EsportsHeroStatResponse> heroStats,
                                                                         boolean descending) {
        Comparator<EsportsHeroStatResponse> comparator = Comparator
                .comparing(
                        (EsportsHeroStatResponse item) -> safeDouble(item.pickWinRate()),
                        descending ? Comparator.reverseOrder() : Comparator.naturalOrder()
                )
                .thenComparing(EsportsHeroStatResponse::pickCount, Comparator.reverseOrder())
                .thenComparing(item -> safeDouble(item.presenceCount()), Comparator.reverseOrder())
                .thenComparing(item -> safeText(item.heroName()));

        return heroStats.stream()
                .filter(item -> safeLong(item.pickCount()) >= MIN_HERO_PICK_SAMPLE)
                .sorted(comparator)
                .limit(DASHBOARD_HERO_INSIGHT_LIMIT)
                .map(item -> new EsportsDashboardResponse.HeroInsight(
                        item.heroId(),
                        item.heroName(),
                        item.heroAvatarUrl(),
                        item.pickCount(),
                        item.pickWins(),
                        item.pickLosses(),
                        item.pickWinRate()
                ))
                .toList();
    }

    private List<EsportsDashboardResponse.TeamInsight> buildTopTeams(List<EsportsMatchGame> games) {
        Map<Long, TeamInsightAccumulator> teamsById = new LinkedHashMap<>();

        for (EsportsMatchGame game : games) {
            Long winnerTeamId = safeEntityId(game.getWinnerTeam());
            if (winnerTeamId == null) {
                continue;
            }

            applyTeamResult(teamsById, game.getBlueTeam(), winnerTeamId.equals(safeEntityId(game.getBlueTeam())));
            applyTeamResult(teamsById, game.getRedTeam(), winnerTeamId.equals(safeEntityId(game.getRedTeam())));
        }

        return teamsById.values().stream()
                .map(TeamInsightAccumulator::toResponse)
                .filter(item -> safeLong(item.gamesPlayed()) >= MIN_TEAM_SAMPLE)
                .sorted(Comparator
                        .comparing(
                                (EsportsDashboardResponse.TeamInsight item) -> safeDouble(item.winRate()),
                                Comparator.reverseOrder()
                        )
                        .thenComparing(EsportsDashboardResponse.TeamInsight::gamesPlayed, Comparator.reverseOrder())
                        .thenComparing(EsportsDashboardResponse.TeamInsight::wins, Comparator.reverseOrder())
                        .thenComparing(item -> safeText(item.teamName())))
                .limit(DASHBOARD_TOP_TEAM_LIMIT)
                .toList();
    }

    private void applyTeamResult(Map<Long, TeamInsightAccumulator> teamsById,
                                 EsportsTeam team,
                                 boolean teamWon) {
        Long teamId = safeEntityId(team);
        if (teamId == null) {
            return;
        }

        teamsById.computeIfAbsent(teamId, ignored -> new TeamInsightAccumulator(team))
                .applyResult(teamWon);
    }

    private List<EsportsDashboardResponse.TeamOption> buildTeamOptions(AnalyticsFilter filter) {
        List<EsportsMatchGame> scopeGames = esportsMatchGameRepository.findAllForAnalytics(
                filter.tournamentTier(),
                null,
                filter.dateFrom(),
                filter.dateTo()
        );
        Map<String, EsportsDashboardResponse.TeamOption> teamsByCode = new LinkedHashMap<>();

        for (EsportsMatchGame game : scopeGames) {
            applyTeamOption(teamsByCode, game.getBlueTeam());
            applyTeamOption(teamsByCode, game.getRedTeam());
        }

        return teamsByCode.values().stream()
                .sorted(Comparator
                        .comparing((EsportsDashboardResponse.TeamOption item) -> safeText(item.teamName()))
                        .thenComparing(item -> safeText(item.teamCode())))
                .toList();
    }

    private void applyTeamOption(Map<String, EsportsDashboardResponse.TeamOption> teamsByCode, EsportsTeam team) {
        String teamCode = normalizeTeamCode(team != null ? team.getTeamCode() : null);
        if (!StringUtils.hasText(teamCode)) {
            return;
        }

        teamsByCode.putIfAbsent(
                teamCode,
                new EsportsDashboardResponse.TeamOption(
                        teamCode,
                        displayTeamName(team),
                        team != null ? team.getLogoUrl() : null
                )
        );
    }

    private String resolveTournamentTier(String tournamentName) {
        if (!StringUtils.hasText(tournamentName)) {
            return null;
        }

        String normalizedTournamentName = tournamentName.trim();
        String resolvedTier = EsportsTournamentCatalog.resolveTournamentTier(normalizedTournamentName);
        if (resolvedTier != null) {
            return resolvedTier;
        }

        boolean isExistingDraftTier = esportsMatchDraftActionRepository.findDraftTournamentsOrderByLatestMatchDesc()
                .stream()
                .map(tournament -> tournament.tournamentTier() == null ? "" : tournament.tournamentTier().trim())
                .anyMatch(tournamentTier -> tournamentTier.equalsIgnoreCase(normalizedTournamentName));
        if (isExistingDraftTier) {
            return normalizedTournamentName;
        }

        throw new IllegalArgumentException("tournamentName khong hop le.");
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static Comparator<EsportsHeroStatResponse> heroStatComparator() {
        return Comparator
                .comparing(EsportsHeroStatResponse::presenceCount, Comparator.reverseOrder())
                .thenComparing(EsportsHeroStatResponse::pickCount, Comparator.reverseOrder())
                .thenComparing(EsportsHeroStatResponse::banCount, Comparator.reverseOrder())
                .thenComparing(response -> safeText(response.heroName()))
                .thenComparing(response -> response.heroId() == null ? Long.MAX_VALUE : response.heroId());
    }

    private static double calculateWinRate(long wins, long total) {
        if (total <= 0L) {
            return 0D;
        }
        return (wins * 100.0D) / total;
    }

    private static Long safeEntityId(EsportsTeam team) {
        return team == null ? null : team.getId();
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static double safeDouble(Number value) {
        return value == null ? 0D : value.doubleValue();
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeTeamCode(String teamCode) {
        return StringUtils.hasText(teamCode) ? teamCode.trim().toUpperCase() : null;
    }

    private static String displayTeamName(EsportsTeam team) {
        if (team == null) {
            return "";
        }
        if (StringUtils.hasText(team.getTeamName())) {
            return team.getTeamName().trim();
        }
        return safeText(team.getTeamCode()).trim();
    }

    private static Double readHeroScore(Hero hero) {
        if (hero == null) {
            return null;
        }
        BigDecimal score = hero.getBanPickScore();
        return score == null ? null : score.doubleValue();
    }

    private record AnalyticsFilter(
            String tournamentTier,
            String teamCode,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String resolvedTournamentName
    ) {
        private AnalyticsFilter withoutTeamCode() {
            return new AnalyticsFilter(tournamentTier, null, dateFrom, dateTo, resolvedTournamentName);
        }
    }

    private record DraftAccuracySnapshot(Double accuracy, Long sampleSize) {
    }

    private static final class AggregateHeroStatAccumulator {

        private final Long heroId;
        private String heroName;
        private String heroAvatarUrl;
        private long pickCount;
        private long pickWins;
        private long bluePickCount;
        private long blueWins;
        private long redPickCount;
        private long redWins;
        private long banCount;
        private long blueBanCount;
        private long redBanCount;

        private AggregateHeroStatAccumulator(Long heroId) {
            this.heroId = heroId;
        }

        private void applyPickStats(EsportsHeroPickStatAggregate item) {
            updateHeroMeta(item.heroName(), item.heroAvatarUrl());
            pickCount = safeLong(item.pickCount());
            pickWins = safeLong(item.pickWins());
            bluePickCount = safeLong(item.bluePickCount());
            blueWins = safeLong(item.blueWins());
            redPickCount = safeLong(item.redPickCount());
            redWins = safeLong(item.redWins());
        }

        private void applyBanStats(EsportsHeroBanBreakdownAggregate item) {
            updateHeroMeta(item.heroName(), item.heroAvatarUrl());
            banCount = safeLong(item.banCount());
            blueBanCount = safeLong(item.blueBanCount());
            redBanCount = safeLong(item.redBanCount());
        }

        private void updateHeroMeta(String nextHeroName, String nextHeroAvatarUrl) {
            if (!StringUtils.hasText(heroName) && StringUtils.hasText(nextHeroName)) {
                heroName = nextHeroName;
            }
            if (!StringUtils.hasText(heroAvatarUrl) && StringUtils.hasText(nextHeroAvatarUrl)) {
                heroAvatarUrl = nextHeroAvatarUrl;
            }
        }

        private EsportsHeroStatResponse toResponse() {
            long pickLosses = Math.max(0L, pickCount - pickWins);
            long blueLosses = Math.max(0L, bluePickCount - blueWins);
            long redLosses = Math.max(0L, redPickCount - redWins);

            return new EsportsHeroStatResponse(
                    heroId,
                    heroName,
                    heroAvatarUrl,
                    pickCount,
                    pickWins,
                    pickLosses,
                    calculateWinRate(pickWins, pickCount),
                    bluePickCount,
                    blueWins,
                    blueLosses,
                    calculateWinRate(blueWins, bluePickCount),
                    redPickCount,
                    redWins,
                    redLosses,
                    calculateWinRate(redWins, redPickCount),
                    banCount,
                    blueBanCount,
                    redBanCount,
                    pickCount + banCount
            );
        }
    }

    private static final class ActionHeroStatAccumulator {

        private final Long heroId;
        private String heroName;
        private String heroAvatarUrl;
        private long pickCount;
        private long pickWins;
        private long bluePickCount;
        private long blueWins;
        private long redPickCount;
        private long redWins;
        private long banCount;
        private long blueBanCount;
        private long redBanCount;

        private ActionHeroStatAccumulator(Long heroId) {
            this.heroId = heroId;
        }

        private void apply(EsportsMatchDraftAction action) {
            Hero hero = action.getHero();
            if (hero != null) {
                if (!StringUtils.hasText(heroName) && StringUtils.hasText(hero.getName())) {
                    heroName = hero.getName();
                }
                if (!StringUtils.hasText(heroAvatarUrl) && StringUtils.hasText(hero.getAvatarUrl())) {
                    heroAvatarUrl = hero.getAvatarUrl();
                }
            }

            if (action.getActionType() == BanPickActionType.BAN) {
                banCount++;
                if (action.getTeamSide() == BanPickTeamSide.BLUE) {
                    blueBanCount++;
                } else if (action.getTeamSide() == BanPickTeamSide.RED) {
                    redBanCount++;
                }
                return;
            }

            if (action.getActionType() != BanPickActionType.PICK) {
                return;
            }

            pickCount++;
            if (action.getTeamSide() == BanPickTeamSide.BLUE) {
                bluePickCount++;
            } else if (action.getTeamSide() == BanPickTeamSide.RED) {
                redPickCount++;
            }

            Long winnerTeamId = action.getGame() != null ? safeEntityId(action.getGame().getWinnerTeam()) : null;
            Long actionTeamId = safeEntityId(action.getTeam());
            if (winnerTeamId != null && winnerTeamId.equals(actionTeamId)) {
                pickWins++;
                if (action.getTeamSide() == BanPickTeamSide.BLUE) {
                    blueWins++;
                } else if (action.getTeamSide() == BanPickTeamSide.RED) {
                    redWins++;
                }
            }
        }

        private EsportsHeroStatResponse toResponse() {
            long pickLosses = Math.max(0L, pickCount - pickWins);
            long blueLosses = Math.max(0L, bluePickCount - blueWins);
            long redLosses = Math.max(0L, redPickCount - redWins);

            return new EsportsHeroStatResponse(
                    heroId,
                    heroName,
                    heroAvatarUrl,
                    pickCount,
                    pickWins,
                    pickLosses,
                    calculateWinRate(pickWins, pickCount),
                    bluePickCount,
                    blueWins,
                    blueLosses,
                    calculateWinRate(blueWins, bluePickCount),
                    redPickCount,
                    redWins,
                    redLosses,
                    calculateWinRate(redWins, redPickCount),
                    banCount,
                    blueBanCount,
                    redBanCount,
                    pickCount + banCount
            );
        }
    }

    private static final class HeroBanAccumulator {

        private final Long heroId;
        private String heroName;
        private String heroAvatarUrl;
        private long banCount;

        private HeroBanAccumulator(Long heroId) {
            this.heroId = heroId;
        }

        private void apply(EsportsMatchDraftAction action) {
            Hero hero = action.getHero();
            if (hero != null) {
                if (!StringUtils.hasText(heroName) && StringUtils.hasText(hero.getName())) {
                    heroName = hero.getName();
                }
                if (!StringUtils.hasText(heroAvatarUrl) && StringUtils.hasText(hero.getAvatarUrl())) {
                    heroAvatarUrl = hero.getAvatarUrl();
                }
            }
            banCount++;
        }
    }

    private static final class ActivityAccumulator {

        private final Set<Long> matchIds = new HashSet<>();
        private long gameCount;

        private void apply(EsportsMatchGame game) {
            if (game.getMatch() != null && game.getMatch().getId() != null) {
                matchIds.add(game.getMatch().getId());
            }
            gameCount++;
        }
    }

    private static final class TeamInsightAccumulator {

        private final Long teamId;
        private final String teamCode;
        private final String teamName;
        private final String logoUrl;
        private long wins;
        private long losses;

        private TeamInsightAccumulator(EsportsTeam team) {
            this.teamId = safeEntityId(team);
            this.teamCode = team == null ? null : team.getTeamCode();
            this.teamName = displayTeamName(team);
            this.logoUrl = team == null ? null : team.getLogoUrl();
        }

        private void applyResult(boolean teamWon) {
            if (teamWon) {
                wins++;
            } else {
                losses++;
            }
        }

        private EsportsDashboardResponse.TeamInsight toResponse() {
            long gamesPlayed = wins + losses;
            return new EsportsDashboardResponse.TeamInsight(
                    teamId,
                    teamCode,
                    teamName,
                    logoUrl,
                    wins,
                    losses,
                    gamesPlayed,
                    calculateWinRate(wins, gamesPlayed)
            );
        }
    }
}
