package com.example.demo.service;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EloCalculationServiceTest {

    private static final double TOLERANCE = 1e-6;
    private static final double BASE_VAL = 20.0;
    private static final double X_VAL = 1.0;
    private static final double Y_VAL = 12.0;
    private static final double MIN_PROTECTED = 5.0;
    private static final double RDP_BASE = 30.0;
    private static final double RDP_DENOMINATOR = 1200.0;
    private static final double CP_GLOBAL = 75.0;
    private static final double CP_LOCAL = 50.0;
    private static final double HARD_PENALTY_RATIO = 0.7;
    private static final double SOFT_PENALTY_RATIO = 0.3;
    private static final double INITIAL_SCORE = 1200.0;
    private static final LocalDateTime BASE_MATCH_DATE = LocalDateTime.of(2026, 5, 14, 12, 0);

    private static final Map<String, Double> TIER_CONF = Map.of(
            "0", 1.5,
            "1", 1.0,
            "2", 0.5
    );

    private static final Map<String, Double> STAGE_CONF = Map.of(
            "ck", 1.4,
            "playoff", 1.0,
            "bang", 0.7,
            "vongloai", 0.5
    );

    private static final Map<String, List<String>> REGIONS = Map.of(
            "RPL", List.of("FS", "BAC", "BRU", "SLX", "eA", "TEN", "HD", "KOG", "GJC"),
            "AOG", List.of("SGP", "FPT", "1S", "BOX", "SPN", "FPL", "TS", "GAM"),
            "GCS", List.of("FW", "HKA", "ONE", "DCG", "BMG", "ANK", "LIT")
    );

    private static final List<String> KNOWN_TEAM_CODES = REGIONS.values().stream()
            .flatMap(List::stream)
            .toList();

    @Test
    void calculateAllRankingsMatchesPythonTierOneBaselineAndNormalTransfer() {
        List<EsportsMatch> matches = List.of(match(1L, "FS", "BAC", 3, 0, "1", "bang"));

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);

        double expectedChange = BASE_VAL * TIER_CONF.get("1") * STAGE_CONF.get("bang") * (1.0 + 3.0 / 3.0);

        assertThat(actual.get("FS").score()).isCloseTo(INITIAL_SCORE + expectedChange, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isCloseTo(INITIAL_SCORE - expectedChange, within(TOLERANCE));
        assertThat(actual.get("BRU").score()).isCloseTo(INITIAL_SCORE, within(TOLERANCE));
        assertThat(expected.steps().get(0).shockwave()).isFalse();
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsUsesTournamentAerTierZeroOverLegacyMatchTier() {
        List<EsportsMatch> matches = List.of(
                match(1L, "FS", "BAC", 3, 0, "1", "bang", tournamentWithAerTier(0))
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);

        double expectedChange = BASE_VAL * TIER_CONF.get("0") * STAGE_CONF.get("bang") * (1.0 + 3.0 / 3.0);

        assertThat(actual.get("FS").score()).isCloseTo(INITIAL_SCORE + expectedChange, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isCloseTo(INITIAL_SCORE - expectedChange, within(TOLERANCE));
        assertThat(expected.steps().get(0).resolvedTier()).isEqualTo("0");
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsUsesTournamentAerTierTwoOverLegacyMatchTier() {
        List<EsportsMatch> matches = List.of(
                match(1L, "FS", "BAC", 3, 0, "1", "bang", tournamentWithAerTier(2))
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);

        double expectedChange = BASE_VAL * TIER_CONF.get("2") * STAGE_CONF.get("bang") * (1.0 + 3.0 / 3.0);

        assertThat(actual.get("FS").score()).isCloseTo(INITIAL_SCORE + expectedChange, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isCloseTo(INITIAL_SCORE - expectedChange, within(TOLERANCE));
        assertThat(expected.steps().get(0).resolvedTier()).isEqualTo("2");
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsFallsBackToLegacyMatchTierWhenTournamentIsMissing() {
        List<EsportsMatch> matches = List.of(match(1L, "FS", "BAC", 3, 0, "2", "bang"));

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);

        double expectedChange = BASE_VAL * TIER_CONF.get("2") * STAGE_CONF.get("bang") * (1.0 + 3.0 / 3.0);

        assertThat(actual.get("FS").score()).isCloseTo(INITIAL_SCORE + expectedChange, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isCloseTo(INITIAL_SCORE - expectedChange, within(TOLERANCE));
        assertThat(expected.steps().get(0).resolvedTier()).isEqualTo("2");
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesProtectedMinimumForNonTierTwo() {
        List<EsportsMatch> matches = new ArrayList<>(repeatedMatches(4, 1L, "FS", "BAC", 4, 0, "0", "ck"));
        matches.add(match(5L, "FS", "BAC", 2, 0, "1", "bang"));

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep protectedMatch = expected.steps().get(expected.steps().size() - 1);

        assertThat(protectedMatch.rawMatchChange()).isLessThan(MIN_PROTECTED);
        assertThat(protectedMatch.protectionApplied()).isTrue();
        assertThat(protectedMatch.protectedMinimum()).isCloseTo(MIN_PROTECTED, within(TOLERANCE));
        assertThat(protectedMatch.appliedMatchChange()).isCloseTo(MIN_PROTECTED, within(TOLERANCE));
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesTierTwoSafeModeProtectedMinimumOfOne() {
        List<EsportsMatch> matches = new ArrayList<>(repeatedMatches(7, 1L, "FS", "BAC", 4, 0, "0", "ck"));
        matches.add(match(8L, "FS", "BAC", 2, 0, "2", "bang"));

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep protectedMatch = expected.steps().get(expected.steps().size() - 1);

        assertThat(protectedMatch.rawMatchChange()).isLessThan(1.0);
        assertThat(protectedMatch.protectionApplied()).isTrue();
        assertThat(protectedMatch.protectedMinimum()).isCloseTo(1.0, within(TOLERANCE));
        assertThat(protectedMatch.appliedMatchChange()).isCloseTo(1.0, within(TOLERANCE));
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesTierOneShockwaveWithinLoserRegionOnly() {
        List<EsportsMatch> matches = List.of(
                match(1L, "SGP", "FPT", 0, 3, "1", "bang"),
                match(2L, "FS", "BAC", 3, 0, "1", "bang"),
                match(3L, "SGP", "FS", 3, 0, "1", "bang")
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep shockwave = expected.steps().get(2);

        assertThat(shockwave.shockwave()).isTrue();
        assertThat(shockwave.softVictims()).contains("FS", "BRU", "SLX", "eA", "TEN", "HD", "KOG", "GJC");
        assertThat(shockwave.softVictims()).doesNotContain("FPT", "FW", "SGP");
        assertThat(actual.get("BRU").score()).isLessThan(INITIAL_SCORE);
        assertThat(actual.get("FW").score()).isCloseTo(INITIAL_SCORE, within(TOLERANCE));
        assertThat(actual.get("FPT").score()).isCloseTo(1228.0, within(TOLERANCE));
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesTierZeroShockwaveGlobally() {
        List<EsportsMatch> matches = List.of(
                match(1L, "SGP", "FPT", 0, 3, "1", "bang"),
                match(2L, "FS", "BAC", 3, 0, "1", "bang"),
                match(3L, "SGP", "FS", 3, 0, "1", "bang", tournamentWithAerTier(0))
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep shockwave = expected.steps().get(2);

        assertThat(shockwave.shockwave()).isTrue();
        assertThat(shockwave.resolvedTier()).isEqualTo("0");
        assertThat(shockwave.softVictims()).contains("FS", "FPT", "BRU", "FW", "HKA", "ONE", "DCG");
        assertThat(shockwave.softVictims()).doesNotContain("BAC", "SGP");
        assertThat(actual.get("FW").score()).isLessThan(INITIAL_SCORE);
        assertThat(actual.get("BRU").score()).isLessThan(INITIAL_SCORE);
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesTierZeroRdpAcrossRegions() {
        List<EsportsMatch> matches = List.of(
                match(1L, "FS", "SGP", 3, 0, "1", "bang", tournamentWithAerTier(0))
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep rdpMatch = expected.steps().get(0);

        double kDiff = 1.0 - (rdpMatch.argWinner() - rdpMatch.argLoser()) / RDP_DENOMINATOR;
        double expectedRdpTotal = Math.max(
                10.0,
                RDP_BASE * (1.0 + (double) rdpMatch.winnerGames() / rdpMatch.totalGames()) * kDiff
        );

        assertThat(rdpMatch.rdpTotal()).isCloseTo(expectedRdpTotal, within(TOLERANCE));
        assertThat(actual.get("BRU").score()).isGreaterThan(INITIAL_SCORE);
        assertThat(actual.get("FPT").score()).isLessThan(INITIAL_SCORE);
        assertThat(actual.get("FS").score()).isGreaterThan(actual.get("BRU").score());
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesChampionPointTierOneLocally() {
        List<EsportsMatch> matches = List.of(match(1L, "SGP", "FS", 4, 0, "1", "ck"));

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep championMatch = expected.steps().get(0);

        assertThat(championMatch.championPoints()).isCloseTo(CP_LOCAL, within(TOLERANCE));
        assertThat(actual.get("SGP").score()).isCloseTo(1306.0, within(TOLERANCE));
        assertThat(actual.get("FPT").score()).isCloseTo(1192.857142857143, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isCloseTo(INITIAL_SCORE, within(TOLERANCE));
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsAppliesChampionPointTierZeroGlobally() {
        List<EsportsMatch> matches = List.of(
                match(1L, "SGP", "FPT", 4, 0, "1", "ck", tournamentWithAerTier(0))
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);
        MatchStep championMatch = expected.steps().get(0);

        assertThat(championMatch.championPoints()).isCloseTo(CP_GLOBAL, within(TOLERANCE));
        assertThat(actual.get("SGP").score()).isCloseTo(1359.0, within(TOLERANCE));
        assertThat(actual.get("BAC").score()).isLessThan(INITIAL_SCORE);
        assertThat(actual.get("HKA").score()).isLessThan(INITIAL_SCORE);
        assertThat(actual.get("FPT").score()).isLessThan(1116.0);
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsTracksGameAndMatchStatsSeparately() {
        List<EsportsMatch> matches = List.of(
                match(1L, "FS", "BAC", 3, 1, "1", "bang"),
                match(2L, "FS", "BRU", 2, 3, "1", "bang")
        );

        Map<String, TeamSnapshot> actual = calculateActual(matches);
        SimulationResult expected = simulatePython(matches);

        assertThat(actual.get("FS").gameWins()).isEqualTo(5);
        assertThat(actual.get("FS").gameLosses()).isEqualTo(4);
        assertThat(actual.get("FS").matchWins()).isEqualTo(1);
        assertThat(actual.get("FS").matchLosses()).isEqualTo(1);

        assertThat(actual.get("BAC").gameWins()).isEqualTo(1);
        assertThat(actual.get("BAC").gameLosses()).isEqualTo(3);
        assertThat(actual.get("BAC").matchWins()).isEqualTo(0);
        assertThat(actual.get("BAC").matchLosses()).isEqualTo(1);

        assertThat(actual.get("BRU").gameWins()).isEqualTo(3);
        assertThat(actual.get("BRU").gameLosses()).isEqualTo(2);
        assertThat(actual.get("BRU").matchWins()).isEqualTo(1);
        assertThat(actual.get("BRU").matchLosses()).isEqualTo(0);
        assertParity(actual, expected);
    }

    @Test
    void calculateAllRankingsReplaysSameTimestampMatchesByAscendingId() {
        EsportsMatch lowerIdMatch = matchAt(BASE_MATCH_DATE, 10L, "FS", "SGP", 3, 0, "2", "bang");
        EsportsMatch higherIdMatch = matchAt(BASE_MATCH_DATE, 11L, "FS", "SGP", 0, 3, "2", "bang");
        List<EsportsMatch> orderedMatches = List.of(lowerIdMatch, higherIdMatch);

        Map<String, TeamSnapshot> actual = calculateActual(orderedMatches);
        SimulationResult expectedAscending = simulatePython(orderedMatches);
        SimulationResult expectedDescending = simulatePython(List.of(higherIdMatch, lowerIdMatch));

        assertParity(actual, expectedAscending);
        assertThat(actual.get("FS").score()).isCloseTo(1199.1833333333334, within(TOLERANCE));
        assertThat(actual.get("SGP").score()).isCloseTo(1200.8166666666666, within(TOLERANCE));
        assertThat(actual.get("FS").score()).isNotCloseTo(
                expectedDescending.teams().get("FS").score(),
                within(0.1)
        );
    }

    private Map<String, TeamSnapshot> calculateActual(List<EsportsMatch> matches) {
        Map<String, EsportsTeam> storedTeams = new LinkedHashMap<>();
        EsportsTeamRepository teamRepository = mock(EsportsTeamRepository.class);
        EsportsMatchRepository matchRepository = mock(EsportsMatchRepository.class);

        when(matchRepository.findAllByOrderByMatchDateAscIdAsc()).thenReturn(matches);
        when(teamRepository.findByTeamCode(anyString())).thenAnswer(invocation -> {
            String teamCode = invocation.getArgument(0, String.class);
            return Optional.of(storedTeams.computeIfAbsent(teamCode, this::team));
        });
        when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EloCalculationService service = new EloCalculationService(teamRepository, matchRepository);
        service.calculateAllRankings();

        verify(matchRepository).findAllByOrderByMatchDateAscIdAsc();
        return toSnapshots(storedTeams);
    }

    private SimulationResult simulatePython(List<EsportsMatch> matches) {
        Map<String, Double> teamScores = new LinkedHashMap<>();
        Map<String, int[]> teamStats = new LinkedHashMap<>();
        List<MatchStep> steps = new ArrayList<>();

        for (String teamCode : KNOWN_TEAM_CODES) {
            teamScores.put(teamCode, INITIAL_SCORE);
            teamStats.put(teamCode, new int[]{0, 0, 0, 0});
        }

        for (EsportsMatch match : matches) {
            String doi1 = match.getTeam1Code();
            String doi2 = match.getTeam2Code();
            int ts1 = match.getScore1();
            int ts2 = match.getScore2();
            String tierIn = resolveTier(match);
            String stageIn = match.getStage() != null ? match.getStage() : "bang";

            double tierVal = TIER_CONF.getOrDefault(tierIn, 1.0);
            double stageVal = STAGE_CONF.getOrDefault(stageIn, 1.0);

            teamScores.putIfAbsent(doi1, INITIAL_SCORE);
            teamScores.putIfAbsent(doi2, INITIAL_SCORE);
            teamStats.putIfAbsent(doi1, new int[]{0, 0, 0, 0});
            teamStats.putIfAbsent(doi2, new int[]{0, 0, 0, 0});

            teamStats.get(doi1)[0] += ts1;
            teamStats.get(doi1)[1] += ts2;
            teamStats.get(doi2)[0] += ts2;
            teamStats.get(doi2)[1] += ts1;

            if (ts1 > ts2) {
                teamStats.get(doi1)[2] += 1;
                teamStats.get(doi2)[3] += 1;
            } else if (ts2 > ts1) {
                teamStats.get(doi2)[2] += 1;
                teamStats.get(doi1)[3] += 1;
            }

            double currentBase = BASE_VAL * tierVal * stageVal;
            double sc1 = teamScores.get(doi1);
            double sc2 = teamScores.get(doi2);
            int totalGames = (ts1 + ts2) > 0 ? (ts1 + ts2) : 1;
            double sf1 = 1.0 + (double) ts1 / totalGames;
            double sf2 = 1.0 + (double) ts2 / totalGames;

            String winner = "";
            String loser = "";
            double rawMatchChange = 0.0;
            double appliedMatchChange = 0.0;
            double scWinner = 0.0;
            double scLoser = 0.0;
            double protectedMinimum = "2".equals(tierIn) ? 1.0 : MIN_PROTECTED;
            boolean protectionApplied = false;

            if (ts1 > ts2) {
                winner = doi1;
                loser = doi2;
                scWinner = sc1;
                scLoser = sc2;
                double diff = sc1 - sc2;
                double baseEarn = currentBase * sf1;
                double adj = ((diff * X_VAL) / Y_VAL) * tierVal * stageVal;
                rawMatchChange = baseEarn - adj;
                appliedMatchChange = rawMatchChange;

                if (isProtected(ts1, ts2) && appliedMatchChange < protectedMinimum) {
                    appliedMatchChange = protectedMinimum;
                    protectionApplied = true;
                }
            } else if (ts2 > ts1) {
                winner = doi2;
                loser = doi1;
                scWinner = sc2;
                scLoser = sc1;
                double diff = sc2 - sc1;
                double baseEarn = currentBase * sf2;
                double adj = ((diff * X_VAL) / Y_VAL) * tierVal * stageVal;
                rawMatchChange = baseEarn - adj;
                appliedMatchChange = rawMatchChange;

                if (isProtected(ts2, ts1) && appliedMatchChange < protectedMinimum) {
                    appliedMatchChange = protectedMinimum;
                    protectionApplied = true;
                }
            }

            boolean shockwave = !winner.isEmpty()
                    && scWinner < scLoser
                    && appliedMatchChange > 0
                    && !"2".equals(tierIn);

            Set<String> softVictims = new LinkedHashSet<>();

            if (!shockwave) {
                if (winner.equals(doi1)) {
                    teamScores.merge(doi1, appliedMatchChange, Double::sum);
                    teamScores.merge(doi2, -appliedMatchChange, Double::sum);
                } else if (winner.equals(doi2)) {
                    teamScores.merge(doi2, appliedMatchChange, Double::sum);
                    teamScores.merge(doi1, -appliedMatchChange, Double::sum);
                }
            } else {
                teamScores.merge(winner, appliedMatchChange, Double::sum);

                List<VictimPenalty> victims = new ArrayList<>();
                double sumDistance = 0.0;

                for (Map.Entry<String, Double> entry : teamScores.entrySet()) {
                    String teamCode = entry.getKey();
                    double score = entry.getValue();
                    if (!teamCode.equals(winner) && score > scWinner && score <= scLoser) {
                        boolean isVictim = "0".equals(tierIn)
                                || regionOf(teamCode).equals(regionOf(loser));
                        if (isVictim) {
                            double distance = score - scWinner;
                            if (distance > 0) {
                                victims.add(new VictimPenalty(teamCode, distance));
                                sumDistance += distance;
                            }
                        }
                    }
                }

                if (!victims.isEmpty() && sumDistance > 0) {
                    double hardPool = appliedMatchChange * HARD_PENALTY_RATIO;
                    double softPool = appliedMatchChange * SOFT_PENALTY_RATIO;
                    teamScores.merge(loser, -hardPool, Double::sum);

                    for (VictimPenalty victim : victims) {
                        softVictims.add(victim.teamCode());
                        double penalty = softPool * (victim.distance() / sumDistance);
                        teamScores.merge(victim.teamCode(), -penalty, Double::sum);
                    }
                } else {
                    teamScores.merge(loser, -appliedMatchChange, Double::sum);
                }
            }

            double argWinner = 0.0;
            double argLoser = 0.0;
            double rdpTotal = 0.0;
            int winnerGames = 0;

            String reg1 = regionOf(doi1);
            String reg2 = regionOf(doi2);
            if ("0".equals(tierIn)
                    && !reg1.equals(reg2)
                    && !"OTHER".equals(reg1)
                    && !"OTHER".equals(reg2)
                    && !winner.isEmpty()) {

                String winnerRegion;
                String loserRegion;
                if (winner.equals(doi1)) {
                    winnerRegion = reg1;
                    loserRegion = reg2;
                    winnerGames = ts1;
                } else {
                    winnerRegion = reg2;
                    loserRegion = reg1;
                    winnerGames = ts2;
                }

                argWinner = calculateArg(winnerRegion, teamScores);
                argLoser = calculateArg(loserRegion, teamScores);
                double kDiff = 1.0 - (argWinner - argLoser) / RDP_DENOMINATOR;
                rdpTotal = Math.max(10.0, RDP_BASE * (1.0 + (double) winnerGames / totalGames) * kDiff);

                double winnerRegionTotal = calculateTotalRegionScore(winnerRegion, teamScores);
                double loserRegionTotal = calculateTotalRegionScore(loserRegion, teamScores);

                for (String teamCode : new ArrayList<>(teamScores.keySet())) {
                    String teamRegion = regionOf(teamCode);
                    if (teamRegion.equals(winnerRegion) && winnerRegionTotal != 0) {
                        double bonus = rdpTotal * (teamScores.get(teamCode) / winnerRegionTotal);
                        teamScores.merge(teamCode, bonus, Double::sum);
                    } else if (teamRegion.equals(loserRegion) && loserRegionTotal != 0) {
                        double penalty = rdpTotal * (teamScores.get(teamCode) / loserRegionTotal);
                        teamScores.merge(teamCode, -penalty, Double::sum);
                    }
                }
            }

            double championPoints = 0.0;
            if ("ck".equals(stageIn) && !winner.isEmpty()) {
                if ("0".equals(tierIn)) {
                    championPoints = CP_GLOBAL;
                    final String championWinner = winner;
                    double totalVictims = teamScores.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(championWinner))
                            .mapToDouble(Map.Entry::getValue)
                            .sum();
                    if (totalVictims > 0) {
                        teamScores.merge(winner, championPoints, Double::sum);
                        for (String teamCode : new ArrayList<>(teamScores.keySet())) {
                            if (!teamCode.equals(championWinner)) {
                                double deduction = championPoints * (teamScores.get(teamCode) / totalVictims);
                                teamScores.merge(teamCode, -deduction, Double::sum);
                            }
                        }
                    }
                } else if ("1".equals(tierIn)) {
                    championPoints = CP_LOCAL;
                    final String championWinner = winner;
                    final String winnerRegion = regionOf(championWinner);
                    double totalVictims = teamScores.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(championWinner))
                            .filter(entry -> regionOf(entry.getKey()).equals(winnerRegion))
                            .mapToDouble(Map.Entry::getValue)
                            .sum();
                    if (totalVictims > 0) {
                        teamScores.merge(winner, championPoints, Double::sum);
                        for (String teamCode : new ArrayList<>(teamScores.keySet())) {
                            if (!teamCode.equals(championWinner) && regionOf(teamCode).equals(winnerRegion)) {
                                double deduction = championPoints * (teamScores.get(teamCode) / totalVictims);
                                teamScores.merge(teamCode, -deduction, Double::sum);
                            }
                        }
                    }
                }
            }

            steps.add(new MatchStep(
                    match.getId(),
                    winner,
                    loser,
                    tierIn,
                    stageIn,
                    scWinner,
                    scLoser,
                    rawMatchChange,
                    appliedMatchChange,
                    protectionApplied,
                    protectedMinimum,
                    shockwave,
                    List.copyOf(softVictims),
                    argWinner,
                    argLoser,
                    rdpTotal,
                    championPoints,
                    winnerGames,
                    totalGames
            ));
        }

        return new SimulationResult(snapshotState(teamScores, teamStats), steps);
    }

    private void assertParity(Map<String, TeamSnapshot> actual, SimulationResult expected) {
        assertThat(actual).hasSize(expected.teams().size());
        assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(expected.teams().keySet());

        expected.teams().forEach((teamCode, expectedSnapshot) -> {
            TeamSnapshot actualSnapshot = actual.get(teamCode);
            assertThat(actualSnapshot.score())
                    .as("%s score", teamCode)
                    .isCloseTo(expectedSnapshot.score(), within(TOLERANCE));
            assertThat(actualSnapshot.gameWins())
                    .as("%s game wins", teamCode)
                    .isEqualTo(expectedSnapshot.gameWins());
            assertThat(actualSnapshot.gameLosses())
                    .as("%s game losses", teamCode)
                    .isEqualTo(expectedSnapshot.gameLosses());
            assertThat(actualSnapshot.matchWins())
                    .as("%s match wins", teamCode)
                    .isEqualTo(expectedSnapshot.matchWins());
            assertThat(actualSnapshot.matchLosses())
                    .as("%s match losses", teamCode)
                    .isEqualTo(expectedSnapshot.matchLosses());
        });
    }

    private Map<String, TeamSnapshot> toSnapshots(Map<String, EsportsTeam> storedTeams) {
        Map<String, TeamSnapshot> snapshots = new LinkedHashMap<>();
        for (String teamCode : KNOWN_TEAM_CODES) {
            EsportsTeam team = storedTeams.get(teamCode);
            snapshots.put(teamCode, snapshot(team));
        }
        return snapshots;
    }

    private Map<String, TeamSnapshot> snapshotState(Map<String, Double> teamScores, Map<String, int[]> teamStats) {
        Map<String, TeamSnapshot> snapshots = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : teamScores.entrySet()) {
            int[] stats = teamStats.getOrDefault(entry.getKey(), new int[]{0, 0, 0, 0});
            snapshots.put(entry.getKey(), new TeamSnapshot(
                    entry.getValue(),
                    stats[0],
                    stats[1],
                    stats[2],
                    stats[3]
            ));
        }
        return snapshots;
    }

    private TeamSnapshot snapshot(EsportsTeam team) {
        return new TeamSnapshot(
                team.getScore(),
                team.getGameWins(),
                team.getGameLosses(),
                team.getMatchWins(),
                team.getMatchLosses()
        );
    }

    private List<EsportsMatch> repeatedMatches(int count,
                                               long startingId,
                                               String team1Code,
                                               String team2Code,
                                               int score1,
                                               int score2,
                                               String legacyTier,
                                               String stage) {
        List<EsportsMatch> matches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            matches.add(match(startingId + i, team1Code, team2Code, score1, score2, legacyTier, stage));
        }
        return matches;
    }

    private EsportsMatch match(long id,
                               String team1Code,
                               String team2Code,
                               int score1,
                               int score2,
                               String legacyTier,
                               String stage) {
        return match(id, team1Code, team2Code, score1, score2, legacyTier, stage, null);
    }

    private EsportsMatch match(long id,
                               String team1Code,
                               String team2Code,
                               int score1,
                               int score2,
                               String legacyTier,
                               String stage,
                               EsportsTournament tournament) {
        return matchAt(BASE_MATCH_DATE.plusHours(id), id, team1Code, team2Code, score1, score2, legacyTier, stage, tournament);
    }

    private EsportsMatch matchAt(LocalDateTime matchDate,
                                 long id,
                                 String team1Code,
                                 String team2Code,
                                 int score1,
                                 int score2,
                                 String legacyTier,
                                 String stage) {
        return matchAt(matchDate, id, team1Code, team2Code, score1, score2, legacyTier, stage, null);
    }

    private EsportsMatch matchAt(LocalDateTime matchDate,
                                 long id,
                                 String team1Code,
                                 String team2Code,
                                 int score1,
                                 int score2,
                                 String legacyTier,
                                 String stage,
                                 EsportsTournament tournament) {
        EsportsMatch match = new EsportsMatch();
        match.setId(id);
        match.setMatchDate(matchDate);
        match.setTeam1Code(team1Code);
        match.setTeam2Code(team2Code);
        match.setScore1(score1);
        match.setScore2(score2);
        match.setTier(legacyTier);
        match.setStage(stage);
        match.setTournament(tournament);
        return match;
    }

    private EsportsTournament tournamentWithAerTier(int aerTier) {
        EsportsTournament tournament = new EsportsTournament();
        tournament.setId(1L);
        tournament.setAerTier(aerTier);
        tournament.setName("Tournament");
        return tournament;
    }

    private EsportsTeam team(String teamCode) {
        EsportsTeam team = new EsportsTeam();
        team.setTeamCode(teamCode);
        team.setRegion(regionOf(teamCode));
        team.setScore(INITIAL_SCORE);
        team.setGameWins(0);
        team.setGameLosses(0);
        team.setMatchWins(0);
        team.setMatchLosses(0);
        return team;
    }

    private String resolveTier(EsportsMatch match) {
        if (match.getTournament() != null && match.getTournament().getAerTier() != null) {
            int aerTier = match.getTournament().getAerTier();
            if (aerTier >= 0 && aerTier <= 2) {
                return String.valueOf(aerTier);
            }
        }
        return switch (match.getTier()) {
            case "0", "1", "2" -> match.getTier();
            default -> "1";
        };
    }

    private double calculateArg(String regionName, Map<String, Double> currentScores) {
        List<Double> scores = currentScores.entrySet().stream()
                .filter(entry -> regionOf(entry.getKey()).equals(regionName))
                .map(Map.Entry::getValue)
                .toList();
        if (scores.isEmpty()) {
            return INITIAL_SCORE;
        }
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(INITIAL_SCORE);
    }

    private double calculateTotalRegionScore(String regionName, Map<String, Double> currentScores) {
        return currentScores.entrySet().stream()
                .filter(entry -> regionOf(entry.getKey()).equals(regionName))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    private boolean isProtected(int wins, int losses) {
        return (wins == 4 && losses <= 1)
                || (wins == 3 && losses <= 1)
                || (wins == 2 && losses == 0);
    }

    private String regionOf(String teamCode) {
        return REGIONS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(teamCode))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("OTHER");
    }

    private record TeamSnapshot(double score,
                                int gameWins,
                                int gameLosses,
                                int matchWins,
                                int matchLosses) {
    }

    private record VictimPenalty(String teamCode, double distance) {
    }

    private record MatchStep(Long matchId,
                             String winner,
                             String loser,
                             String resolvedTier,
                             String stage,
                             double preWinnerScore,
                             double preLoserScore,
                             double rawMatchChange,
                             double appliedMatchChange,
                             boolean protectionApplied,
                             double protectedMinimum,
                             boolean shockwave,
                             List<String> softVictims,
                             double argWinner,
                             double argLoser,
                             double rdpTotal,
                             double championPoints,
                             int winnerGames,
                             int totalGames) {
    }

    private record SimulationResult(Map<String, TeamSnapshot> teams, List<MatchStep> steps) {
    }
}
