package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsDraftTournamentScopeAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsGameDraft;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.EsportsTournamentCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    private final EsportsGameDraftRepository esportsGameDraftRepository;
    private final EsportsTournamentRepository esportsTournamentRepository;
    private final HeroRepository heroRepository;

    public EsportsDataService(EsportsGameDraftRepository esportsGameDraftRepository,
                              EsportsTournamentRepository esportsTournamentRepository,
                              HeroRepository heroRepository) {
        this.esportsGameDraftRepository = esportsGameDraftRepository;
        this.esportsTournamentRepository = esportsTournamentRepository;
        this.heroRepository = heroRepository;
    }

    public List<EsportsTournamentOptionResponse> getAvailableTournaments() {
        Map<String, EsportsTournamentOptionResponse> uniqueTournaments = new LinkedHashMap<>();

        esportsGameDraftRepository.findDraftTournamentScopesOrderByLatestMatchDesc()
                .forEach(tournament -> {
                    String optionKey;
                    String tournamentName;
                    boolean legacyScope = tournament.tournamentId() == null;
                    if (legacyScope) {
                        optionKey = "legacy:" + safeText(tournament.tournamentTier()).trim().toUpperCase(Locale.ROOT);
                        tournamentName = resolveLegacyTournamentLabel(tournament.tournamentTier());
                    } else {
                        optionKey = "official:" + tournament.tournamentId();
                        tournamentName = StringUtils.hasText(tournament.tournamentName())
                                ? tournament.tournamentName().trim()
                                : resolveLegacyTournamentLabel(tournament.tournamentTier());
                    }
                    uniqueTournaments.putIfAbsent(
                            optionKey,
                            new EsportsTournamentOptionResponse(
                                    tournament.tournamentId(),
                                    tournamentName,
                                    tournament.tournamentTier(),
                                    tournament.franchiseCode(),
                                    legacyScope
                            )
                    );
                });

        return uniqueTournaments.values().stream().toList();
    }

    public List<EsportsHeroBanStatResponse> getTopBannedHeroes(String tournamentName, Integer limit) {
        return getTopBannedHeroes(null, tournamentName, limit);
    }

    public List<EsportsHeroBanStatResponse> getTopBannedHeroes(Long tournamentId, String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentId, tournamentName, limit, null);
    }

    public List<EsportsHeroBanStatResponse> getTopBlueBannedHeroes(String tournamentName, Integer limit) {
        return getTopBlueBannedHeroes(null, tournamentName, limit);
    }

    public List<EsportsHeroBanStatResponse> getTopBlueBannedHeroes(Long tournamentId, String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentId, tournamentName, limit, BanPickTeamSide.BLUE);
    }

    public List<EsportsHeroStatResponse> getHeroStats(String tournamentName) {
        return getHeroStats(null, tournamentName);
    }

    public List<EsportsHeroStatResponse> getHeroStats(Long tournamentId, String tournamentName) {
        TournamentScope tournamentScope = resolveTournamentScope(tournamentId, tournamentName);
        List<EsportsGameDraft> drafts = esportsGameDraftRepository.findAllForAnalyticsScope(
                tournamentScope.tournamentId(),
                tournamentScope.tournamentTier(),
                null,
                null,
                null
        );
        Map<Long, Hero> heroesById = loadHeroesForDrafts(drafts);
        return buildHeroStats(drafts, heroesById);
    }

    public EsportsDashboardResponse getDashboard(String tournamentName,
                                                 String teamCode,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo) {
        return getDashboard(null, tournamentName, teamCode, dateFrom, dateTo);
    }

    public EsportsDashboardResponse getDashboard(Long tournamentId,
                                                 String tournamentName,
                                                 String teamCode,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo) {
        AnalyticsFilter filter = resolveAnalyticsFilter(tournamentId, tournamentName, teamCode, dateFrom, dateTo);
        List<EsportsGameDraft> filteredDrafts = esportsGameDraftRepository.findAllForAnalyticsScope(
                filter.tournamentId(),
                filter.tournamentTier(),
                filter.teamCode(),
                filter.dateFrom(),
                filter.dateTo()
        );
        Map<Long, Hero> heroesById = loadHeroesForDrafts(filteredDrafts);

        List<EsportsHeroStatResponse> heroStats = buildHeroStats(filteredDrafts, heroesById);
        EsportsDashboardResponse.SideAdvantage sideAdvantage = buildSideAdvantage(filteredDrafts);
        DraftAccuracySnapshot draftAccuracy = buildDraftAccuracy(filteredDrafts, heroesById);

        return new EsportsDashboardResponse(
                buildSummary(filteredDrafts, heroStats, sideAdvantage, draftAccuracy),
                buildMatchActivity(filteredDrafts),
                sideAdvantage,
                buildHeroInsights(heroStats, true),
                buildHeroInsights(heroStats, false),
                buildTopTeams(filteredDrafts),
                heroStats,
                buildTopBannedHeroStats(filteredDrafts, heroesById, filter.resolvedTournamentName(), DASHBOARD_BAN_LIMIT, null),
                buildTopBannedHeroStats(filteredDrafts, heroesById, filter.resolvedTournamentName(), 1, BanPickTeamSide.BLUE)
                        .stream()
                        .findFirst()
                        .orElse(null),
                buildTeamOptions(filter.withoutTeamCode())
        );
    }

    private List<EsportsHeroBanStatResponse> getTopHeroBanStats(Long tournamentId,
                                                                String tournamentName,
                                                                Integer limit,
                                                                BanPickTeamSide teamSide) {
        TournamentScope tournamentScope = resolveTournamentScope(tournamentId, tournamentName);
        List<EsportsGameDraft> drafts = esportsGameDraftRepository.findAllForAnalyticsScope(
                tournamentScope.tournamentId(),
                tournamentScope.tournamentTier(),
                null,
                null,
                null
        );
        Map<Long, Hero> heroesById = loadHeroesForDrafts(drafts);
        return buildTopBannedHeroStats(drafts, heroesById, tournamentScope.resolvedTournamentName(), sanitizeLimit(limit), teamSide);
    }

    private AnalyticsFilter resolveAnalyticsFilter(Long tournamentId,
                                                   String tournamentName,
                                                   String teamCode,
                                                   LocalDate dateFrom,
                                                   LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("date range khong hop le.");
        }

        TournamentScope tournamentScope = resolveTournamentScope(tournamentId, tournamentName);

        return new AnalyticsFilter(
                tournamentScope.tournamentId(),
                tournamentScope.tournamentTier(),
                normalizeTeamCode(teamCode),
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? LocalDateTime.of(dateTo, LocalTime.MAX) : null,
                tournamentScope.resolvedTournamentName()
        );
    }

    private List<EsportsHeroStatResponse> buildHeroStats(List<EsportsGameDraft> drafts, Map<Long, Hero> heroesById) {
        Map<Long, HeroStatAccumulator> statsByHeroId = new LinkedHashMap<>();

        for (EsportsGameDraft draft : drafts) {
            boolean blueWon = didSideWin(draft, BanPickTeamSide.BLUE);
            boolean redWon = didSideWin(draft, BanPickTeamSide.RED);

            accumulateBan(statsByHeroId, heroesById, draft.getBlueBan1HeroId(), BanPickTeamSide.BLUE);
            accumulateBan(statsByHeroId, heroesById, draft.getBlueBan2HeroId(), BanPickTeamSide.BLUE);
            accumulateBan(statsByHeroId, heroesById, draft.getBlueBan3HeroId(), BanPickTeamSide.BLUE);
            accumulateBan(statsByHeroId, heroesById, draft.getBlueBan4HeroId(), BanPickTeamSide.BLUE);
            accumulateBan(statsByHeroId, heroesById, draft.getBlueBan5HeroId(), BanPickTeamSide.BLUE);

            accumulateBan(statsByHeroId, heroesById, draft.getRedBan1HeroId(), BanPickTeamSide.RED);
            accumulateBan(statsByHeroId, heroesById, draft.getRedBan2HeroId(), BanPickTeamSide.RED);
            accumulateBan(statsByHeroId, heroesById, draft.getRedBan3HeroId(), BanPickTeamSide.RED);
            accumulateBan(statsByHeroId, heroesById, draft.getRedBan4HeroId(), BanPickTeamSide.RED);
            accumulateBan(statsByHeroId, heroesById, draft.getRedBan5HeroId(), BanPickTeamSide.RED);

            accumulatePick(statsByHeroId, heroesById, draft.getBlueDslHeroId(), BanPickTeamSide.BLUE, blueWon);
            accumulatePick(statsByHeroId, heroesById, draft.getBlueJglHeroId(), BanPickTeamSide.BLUE, blueWon);
            accumulatePick(statsByHeroId, heroesById, draft.getBlueMidHeroId(), BanPickTeamSide.BLUE, blueWon);
            accumulatePick(statsByHeroId, heroesById, draft.getBlueAdlHeroId(), BanPickTeamSide.BLUE, blueWon);
            accumulatePick(statsByHeroId, heroesById, draft.getBlueSupHeroId(), BanPickTeamSide.BLUE, blueWon);

            accumulatePick(statsByHeroId, heroesById, draft.getRedDslHeroId(), BanPickTeamSide.RED, redWon);
            accumulatePick(statsByHeroId, heroesById, draft.getRedJglHeroId(), BanPickTeamSide.RED, redWon);
            accumulatePick(statsByHeroId, heroesById, draft.getRedMidHeroId(), BanPickTeamSide.RED, redWon);
            accumulatePick(statsByHeroId, heroesById, draft.getRedAdlHeroId(), BanPickTeamSide.RED, redWon);
            accumulatePick(statsByHeroId, heroesById, draft.getRedSupHeroId(), BanPickTeamSide.RED, redWon);
        }

        return statsByHeroId.values().stream()
                .map(HeroStatAccumulator::toResponse)
                .sorted(heroStatComparator())
                .toList();
    }

    private void accumulateBan(Map<Long, HeroStatAccumulator> statsByHeroId,
                               Map<Long, Hero> heroesById,
                               Long heroId,
                               BanPickTeamSide teamSide) {
        if (heroId == null) {
            return;
        }
        Hero hero = heroesById.get(heroId);
        if (hero == null) {
            return;
        }
        statsByHeroId.computeIfAbsent(heroId, ignored -> new HeroStatAccumulator(hero))
                .applyBan(teamSide);
    }

    private void accumulatePick(Map<Long, HeroStatAccumulator> statsByHeroId,
                                Map<Long, Hero> heroesById,
                                Long heroId,
                                BanPickTeamSide teamSide,
                                boolean teamWon) {
        if (heroId == null) {
            return;
        }
        Hero hero = heroesById.get(heroId);
        if (hero == null) {
            return;
        }
        statsByHeroId.computeIfAbsent(heroId, ignored -> new HeroStatAccumulator(hero))
                .applyPick(teamSide, teamWon);
    }

    private List<EsportsHeroBanStatResponse> buildTopBannedHeroStats(List<EsportsGameDraft> drafts,
                                                                     Map<Long, Hero> heroesById,
                                                                     String tournamentName,
                                                                     int limit,
                                                                     BanPickTeamSide teamSide) {
        Map<Long, Long> countsByHeroId = new HashMap<>();

        for (EsportsGameDraft draft : drafts) {
            if (teamSide == null || teamSide == BanPickTeamSide.BLUE) {
                incrementBanCount(countsByHeroId, draft.getBlueBan1HeroId(), teamSide);
                incrementBanCount(countsByHeroId, draft.getBlueBan2HeroId(), teamSide);
                incrementBanCount(countsByHeroId, draft.getBlueBan3HeroId(), teamSide);
                incrementBanCount(countsByHeroId, draft.getBlueBan4HeroId(), teamSide);
                incrementBanCount(countsByHeroId, draft.getBlueBan5HeroId(), teamSide);
            }
            if (teamSide == null || teamSide == BanPickTeamSide.RED) {
                incrementBanCount(countsByHeroId, draft.getRedBan1HeroId(), teamSide == null ? null : BanPickTeamSide.RED);
                incrementBanCount(countsByHeroId, draft.getRedBan2HeroId(), teamSide == null ? null : BanPickTeamSide.RED);
                incrementBanCount(countsByHeroId, draft.getRedBan3HeroId(), teamSide == null ? null : BanPickTeamSide.RED);
                incrementBanCount(countsByHeroId, draft.getRedBan4HeroId(), teamSide == null ? null : BanPickTeamSide.RED);
                incrementBanCount(countsByHeroId, draft.getRedBan5HeroId(), teamSide == null ? null : BanPickTeamSide.RED);
            }
        }

        return countsByHeroId.entrySet().stream()
                .filter(entry -> heroesById.containsKey(entry.getKey()))
                .sorted(Comparator
                        .comparing(Map.Entry<Long, Long>::getValue, Comparator.reverseOrder())
                        .thenComparing(entry -> safeText(heroesById.get(entry.getKey()).getName())))
                .limit(limit)
                .map(entry -> {
                    Hero hero = heroesById.get(entry.getKey());
                    return new EsportsHeroBanStatResponse(
                            hero.getId(),
                            hero.getName(),
                            hero.getAvatarUrl(),
                            entry.getValue(),
                            tournamentName
                    );
                })
                .toList();
    }

    private void incrementBanCount(Map<Long, Long> countsByHeroId, Long heroId, BanPickTeamSide sourceSide) {
        if (heroId == null) {
            return;
        }
        if (sourceSide == null || sourceSide == BanPickTeamSide.BLUE || sourceSide == BanPickTeamSide.RED) {
            countsByHeroId.merge(heroId, 1L, Long::sum);
        }
    }

    private EsportsDashboardResponse.Summary buildSummary(List<EsportsGameDraft> drafts,
                                                          List<EsportsHeroStatResponse> heroStats,
                                                          EsportsDashboardResponse.SideAdvantage sideAdvantage,
                                                          DraftAccuracySnapshot draftAccuracy) {
        Set<Long> distinctMatchIds = new HashSet<>();
        for (EsportsGameDraft draft : drafts) {
            if (draft.getMatch() != null && draft.getMatch().getId() != null) {
                distinctMatchIds.add(draft.getMatch().getId());
            }
        }

        return new EsportsDashboardResponse.Summary(
                (long) distinctMatchIds.size(),
                (long) drafts.size(),
                (long) heroStats.size(),
                sideAdvantage.blueWinRate(),
                draftAccuracy.accuracy(),
                sideAdvantage.completedGames(),
                draftAccuracy.sampleSize()
        );
    }

    private List<EsportsDashboardResponse.ActivityPoint> buildMatchActivity(List<EsportsGameDraft> drafts) {
        Map<LocalDate, ActivityAccumulator> activityByDate = new TreeMap<>();

        for (EsportsGameDraft draft : drafts) {
            if (draft.getMatch() == null || draft.getMatch().getMatchDate() == null) {
                continue;
            }

            LocalDate activityDate = draft.getMatch().getMatchDate().toLocalDate();
            activityByDate.computeIfAbsent(activityDate, ignored -> new ActivityAccumulator())
                    .apply(draft);
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

    private EsportsDashboardResponse.SideAdvantage buildSideAdvantage(List<EsportsGameDraft> drafts) {
        long blueWins = 0L;
        long redWins = 0L;

        for (EsportsGameDraft draft : drafts) {
            if (didSideWin(draft, BanPickTeamSide.BLUE)) {
                blueWins++;
            } else if (didSideWin(draft, BanPickTeamSide.RED)) {
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

    private DraftAccuracySnapshot buildDraftAccuracy(List<EsportsGameDraft> drafts,
                                                     Map<Long, Hero> heroesById) {
        long sampleSize = 0L;
        long correctPredictions = 0L;

        for (EsportsGameDraft draft : drafts) {
            if (draft.getWinnerTeam() == null) {
                continue;
            }

            List<Long> blueLineupHeroIds = java.util.Arrays.asList(
                    draft.getBlueDslHeroId(),
                    draft.getBlueJglHeroId(),
                    draft.getBlueMidHeroId(),
                    draft.getBlueAdlHeroId(),
                    draft.getBlueSupHeroId()
            );
            List<Long> redLineupHeroIds = java.util.Arrays.asList(
                    draft.getRedDslHeroId(),
                    draft.getRedJglHeroId(),
                    draft.getRedMidHeroId(),
                    draft.getRedAdlHeroId(),
                    draft.getRedSupHeroId()
            );

            if (countNonNull(blueLineupHeroIds) < REQUIRED_PICKS_PER_SIDE
                    || countNonNull(redLineupHeroIds) < REQUIRED_PICKS_PER_SIDE) {
                continue;
            }

            double blueScore = 0D;
            double redScore = 0D;
            boolean missingScore = false;

            for (Long heroId : blueLineupHeroIds) {
                Hero hero = heroesById.get(heroId);
                Double score = readHeroScore(hero);
                if (score == null) {
                    missingScore = true;
                    break;
                }
                blueScore += score;
            }
            if (missingScore) {
                continue;
            }

            for (Long heroId : redLineupHeroIds) {
                Hero hero = heroesById.get(heroId);
                Double score = readHeroScore(hero);
                if (score == null) {
                    missingScore = true;
                    break;
                }
                redScore += score;
            }

            if (missingScore || Double.compare(blueScore, redScore) == 0) {
                continue;
            }

            Long predictedWinnerId = blueScore > redScore
                    ? safeEntityId(draft.getBlueTeam())
                    : safeEntityId(draft.getRedTeam());
            Long actualWinnerId = safeEntityId(draft.getWinnerTeam());
            if (predictedWinnerId == null || actualWinnerId == null) {
                continue;
            }

            sampleSize++;
            if (predictedWinnerId.equals(actualWinnerId)) {
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

    private List<EsportsDashboardResponse.TeamInsight> buildTopTeams(List<EsportsGameDraft> drafts) {
        Map<Long, TeamInsightAccumulator> teamsById = new LinkedHashMap<>();

        for (EsportsGameDraft draft : drafts) {
            Long winnerTeamId = safeEntityId(draft.getWinnerTeam());
            if (winnerTeamId == null) {
                continue;
            }

            applyTeamResult(teamsById, draft.getBlueTeam(), winnerTeamId.equals(safeEntityId(draft.getBlueTeam())));
            applyTeamResult(teamsById, draft.getRedTeam(), winnerTeamId.equals(safeEntityId(draft.getRedTeam())));
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
        List<EsportsGameDraft> scopeDrafts = esportsGameDraftRepository.findAllForAnalyticsScope(
                filter.tournamentId(),
                filter.tournamentTier(),
                null,
                filter.dateFrom(),
                filter.dateTo()
        );
        Map<String, EsportsDashboardResponse.TeamOption> teamsByCode = new LinkedHashMap<>();

        for (EsportsGameDraft draft : scopeDrafts) {
            applyTeamOption(teamsByCode, draft.getBlueTeam());
            applyTeamOption(teamsByCode, draft.getRedTeam());
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

    private TournamentScope resolveTournamentScope(Long tournamentId, String tournamentName) {
        if (tournamentId != null) {
            EsportsTournament tournament = esportsTournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("tournamentId khong hop le."));
            return new TournamentScope(tournament.getId(), null, tournament.getName());
        }

        if (!StringUtils.hasText(tournamentName)) {
            return new TournamentScope(null, null, null);
        }

        String normalizedTournamentName = tournamentName.trim();
        Optional<EsportsTournament> officialTournament = esportsTournamentRepository.findByNameIgnoreCase(normalizedTournamentName)
                .or(() -> esportsTournamentRepository.findBySlugIgnoreCase(normalizedTournamentName));
        if (officialTournament.isPresent()) {
            EsportsTournament tournament = officialTournament.get();
            return new TournamentScope(tournament.getId(), null, tournament.getName());
        }

        String resolvedTier = EsportsTournamentCatalog.resolveTournamentTier(normalizedTournamentName);
        if (resolvedTier != null) {
            return new TournamentScope(null, resolvedTier, resolveLegacyTournamentLabel(resolvedTier));
        }

        for (EsportsDraftTournamentScopeAggregate aggregate : esportsGameDraftRepository.findDraftTournamentScopesOrderByLatestMatchDesc()) {
            if (aggregate.tournamentId() != null) {
                continue;
            }
            String legacyTier = safeText(aggregate.tournamentTier()).trim();
            String legacyLabel = resolveLegacyTournamentLabel(aggregate.tournamentTier());
            if (legacyTier.equalsIgnoreCase(normalizedTournamentName)
                    || legacyLabel.equalsIgnoreCase(normalizedTournamentName)) {
                return new TournamentScope(null, legacyTier, legacyLabel);
            }
        }

        throw new IllegalArgumentException("tournamentName khong hop le.");
    }

    private String resolveLegacyTournamentLabel(String tournamentTier) {
        if (!StringUtils.hasText(tournamentTier)) {
            return null;
        }
        return EsportsTournamentCatalog.resolveTournamentName(tournamentTier.trim());
    }

    private Map<Long, Hero> loadHeroesForDrafts(List<EsportsGameDraft> drafts) {
        Set<Long> heroIds = new LinkedHashSet<>();
        for (EsportsGameDraft draft : drafts) {
            collectHeroIds(heroIds, java.util.Arrays.asList(
                    draft.getBlueBan1HeroId(),
                    draft.getBlueBan2HeroId(),
                    draft.getBlueBan3HeroId(),
                    draft.getBlueBan4HeroId(),
                    draft.getBlueBan5HeroId(),
                    draft.getRedBan1HeroId(),
                    draft.getRedBan2HeroId(),
                    draft.getRedBan3HeroId(),
                    draft.getRedBan4HeroId(),
                    draft.getRedBan5HeroId(),
                    draft.getBlueDslHeroId(),
                    draft.getBlueJglHeroId(),
                    draft.getBlueMidHeroId(),
                    draft.getBlueAdlHeroId(),
                    draft.getBlueSupHeroId(),
                    draft.getRedDslHeroId(),
                    draft.getRedJglHeroId(),
                    draft.getRedMidHeroId(),
                    draft.getRedAdlHeroId(),
                    draft.getRedSupHeroId()
            ));
        }

        if (heroIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Hero> heroesById = new LinkedHashMap<>();
        heroRepository.findAllById(heroIds)
                .forEach(hero -> heroesById.put(hero.getId(), hero));
        return heroesById;
    }

    private void collectHeroIds(Set<Long> destination, Collection<Long> heroIds) {
        heroIds.stream()
                .filter(id -> id != null && id > 0)
                .forEach(destination::add);
    }

    private boolean didSideWin(EsportsGameDraft draft, BanPickTeamSide side) {
        Long winnerId = safeEntityId(draft.getWinnerTeam());
        if (winnerId == null) {
            return false;
        }
        Long sideTeamId = side == BanPickTeamSide.BLUE
                ? safeEntityId(draft.getBlueTeam())
                : safeEntityId(draft.getRedTeam());
        return winnerId.equals(sideTeamId);
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
        return StringUtils.hasText(teamCode) ? teamCode.trim().toUpperCase(Locale.ROOT) : null;
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

    private static int countNonNull(Collection<Long> values) {
        return (int) values.stream().filter(value -> value != null && value > 0).count();
    }

    private record AnalyticsFilter(
            Long tournamentId,
            String tournamentTier,
            String teamCode,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String resolvedTournamentName
    ) {
        private AnalyticsFilter withoutTeamCode() {
            return new AnalyticsFilter(tournamentId, tournamentTier, null, dateFrom, dateTo, resolvedTournamentName);
        }
    }

    private record TournamentScope(
            Long tournamentId,
            String tournamentTier,
            String resolvedTournamentName
    ) {
    }

    private record DraftAccuracySnapshot(Double accuracy, Long sampleSize) {
    }

    private static final class HeroStatAccumulator {

        private final Long heroId;
        private final String heroName;
        private final String heroAvatarUrl;
        private long pickCount;
        private long pickWins;
        private long bluePickCount;
        private long blueWins;
        private long redPickCount;
        private long redWins;
        private long banCount;
        private long blueBanCount;
        private long redBanCount;

        private HeroStatAccumulator(Hero hero) {
            this.heroId = hero.getId();
            this.heroName = hero.getName();
            this.heroAvatarUrl = hero.getAvatarUrl();
        }

        private void applyBan(BanPickTeamSide side) {
            banCount++;
            if (side == BanPickTeamSide.BLUE) {
                blueBanCount++;
            } else if (side == BanPickTeamSide.RED) {
                redBanCount++;
            }
        }

        private void applyPick(BanPickTeamSide side, boolean won) {
            pickCount++;
            if (side == BanPickTeamSide.BLUE) {
                bluePickCount++;
                if (won) {
                    blueWins++;
                }
            } else if (side == BanPickTeamSide.RED) {
                redPickCount++;
                if (won) {
                    redWins++;
                }
            }

            if (won) {
                pickWins++;
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

    private static final class ActivityAccumulator {

        private final Set<Long> matchIds = new HashSet<>();
        private long gameCount;

        private void apply(EsportsGameDraft draft) {
            if (draft.getMatch() != null && draft.getMatch().getId() != null) {
                matchIds.add(draft.getMatch().getId());
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
