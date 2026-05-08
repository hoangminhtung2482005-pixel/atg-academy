package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.service.EsportsDataService;
import com.example.demo.service.EsportsDraftService;
import com.example.demo.util.EsportsTournamentCatalog;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Public REST Controller cho du lieu Esports.
 * Endpoint: /api/esports
 */
@RestController
@RequestMapping("/api/esports")
public class EsportsController {

    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsDraftService esportsDraftService;
    private final EsportsDataService esportsDataService;

    public EsportsController(EsportsTeamRepository esportsTeamRepository,
                             EsportsMatchRepository esportsMatchRepository,
                             EsportsDraftService esportsDraftService,
                             EsportsDataService esportsDataService) {
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsDraftService = esportsDraftService;
        this.esportsDataService = esportsDataService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<EsportsTeam>> getAllTeamsRanked() {
        List<EsportsTeam> teams = esportsTeamRepository.findAllByOrderByScoreDesc();
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/matches/recent")
    public ResponseEntity<List<RecentMatchDto>> getRecentMatches(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        Map<String, EsportsTeam> teamsByCode = esportsTeamRepository.findAll().stream()
                .collect(Collectors.toMap(
                        team -> normalizeCode(team.getTeamCode()),
                        team -> team,
                        (first, second) -> first
                ));

        List<RecentMatchDto> matches = esportsMatchRepository
                .findAllByOrderByMatchDateDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(match -> toRecentMatchDto(match, teamsByCode))
                .toList();

        return ResponseEntity.ok(matches);
    }

    @GetMapping("/matches/{matchId}/games")
    public ResponseEntity<?> getMatchGames(@PathVariable Long matchId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getGamesByMatchId(matchId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameId}/draft-actions")
    public ResponseEntity<?> getGameDraftActions(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getDraftActionsByGameId(gameId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameId}/lineups")
    public ResponseEntity<?> getGameLineups(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getLineupsByGameId(gameId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/tournaments")
    public ResponseEntity<List<EsportsTournamentOptionResponse>> getDraftTournaments() {
        return ResponseEntity.ok(esportsDataService.getAvailableTournaments());
    }

    @GetMapping("/data/top-banned-heroes")
    public ResponseEntity<?> getTopBannedHeroes(
            @RequestParam(required = false) String tournamentName,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<EsportsHeroBanStatResponse> payload = esportsDataService.getTopBannedHeroes(tournamentName, limit);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/top-blue-banned-heroes")
    public ResponseEntity<?> getTopBlueBannedHeroes(
            @RequestParam(required = false) String tournamentName,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<EsportsHeroBanStatResponse> payload = esportsDataService.getTopBlueBannedHeroes(tournamentName, limit);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/hero-stats")
    public ResponseEntity<?> getHeroStats(@RequestParam(required = false) String tournamentName) {
        try {
            List<EsportsHeroStatResponse> payload = esportsDataService.getHeroStats(tournamentName);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestParam(required = false) String tournamentName,
            @RequestParam(required = false) String teamCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        try {
            EsportsDashboardResponse payload = esportsDataService.getDashboard(
                    tournamentName,
                    teamCode,
                    dateFrom,
                    dateTo
            );
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private RecentMatchDto toRecentMatchDto(EsportsMatch match, Map<String, EsportsTeam> teamsByCode) {
        EsportsTeam team1 = teamsByCode.get(normalizeCode(match.getTeam1Code()));
        EsportsTeam team2 = teamsByCode.get(normalizeCode(match.getTeam2Code()));

        return new RecentMatchDto(
                match.getMatchDate(),
                displayTeamName(match.getTeam1Code(), team1),
                teamLogo(match.getTeam1Code(), team1),
                match.getScore1(),
                displayTeamName(match.getTeam2Code(), team2),
                teamLogo(match.getTeam2Code(), team2),
                match.getScore2(),
                EsportsTournamentCatalog.resolveTournamentName(match.getTier()),
                match.getTier(),
                match.getStage()
        );
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private static String displayTeamName(String fallbackCode, EsportsTeam team) {
        if (team == null) return fallbackCode;
        if (team.getTeamName() != null && !team.getTeamName().isBlank()) {
            return team.getTeamName();
        }
        return team.getTeamCode();
    }

    private static String teamLogo(String fallbackCode, EsportsTeam team) {
        if (team != null && team.getLogoUrl() != null && !team.getLogoUrl().isBlank()) {
            return team.getLogoUrl();
        }
        return "/images/teams/" + fallbackCode + ".png";
    }

    public record RecentMatchDto(
            LocalDateTime matchDate,
            String team1Name,
            String team1Logo,
            Integer team1Score,
            String team2Name,
            String team2Logo,
            Integer team2Score,
            String tournamentName,
            String tournamentTier,
            String stage
    ) {
    }
}
