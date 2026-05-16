package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsFranchiseResponse;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentResponse;
import com.example.demo.dto.esports.EsportsTournamentTeamResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.service.EsportsDataService;
import com.example.demo.service.EsportsFranchiseService;
import com.example.demo.service.EsportsTournamentService;
import com.example.demo.util.EsportsTierSupport;
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
    private final EsportsDataService esportsDataService;
    private final EsportsFranchiseService esportsFranchiseService;
    private final EsportsTournamentService esportsTournamentService;

    public EsportsController(EsportsTeamRepository esportsTeamRepository,
                             EsportsMatchRepository esportsMatchRepository,
                             EsportsDataService esportsDataService,
                             EsportsFranchiseService esportsFranchiseService,
                             EsportsTournamentService esportsTournamentService) {
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsDataService = esportsDataService;
        this.esportsFranchiseService = esportsFranchiseService;
        this.esportsTournamentService = esportsTournamentService;
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

    @GetMapping("/data/tournaments")
    public ResponseEntity<List<EsportsTournamentOptionResponse>> getDraftTournaments() {
        return ResponseEntity.ok(esportsDataService.getAvailableTournaments());
    }

    @GetMapping("/franchises")
    public ResponseEntity<List<EsportsFranchiseResponse>> getFranchises() {
        return ResponseEntity.ok(esportsFranchiseService.getPublicFranchises());
    }

    @GetMapping("/franchises/{code}")
    public ResponseEntity<?> getFranchiseDetail(@PathVariable String code) {
        try {
            return ResponseEntity.ok(esportsFranchiseService.getFranchiseByCode(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tournaments")
    public ResponseEntity<List<EsportsTournamentResponse>> getTournaments(
            @RequestParam(required = false) Long franchiseId,
            @RequestParam(required = false) String franchiseCode) {
        return ResponseEntity.ok(esportsTournamentService.getPublicTournaments(franchiseId, franchiseCode));
    }

    @GetMapping("/tournaments/{id}")
    public ResponseEntity<?> getTournamentDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(esportsTournamentService.getTournamentDetail(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tournaments/{id}/teams")
    public ResponseEntity<?> getTournamentTeams(@PathVariable Long id) {
        try {
            List<EsportsTournamentTeamResponse> payload = esportsTournamentService.listTournamentTeams(id);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/top-banned-heroes")
    public ResponseEntity<?> getTopBannedHeroes(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<EsportsHeroBanStatResponse> payload = esportsDataService.getTopBannedHeroes(tournamentId, tournamentName, limit);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/top-blue-banned-heroes")
    public ResponseEntity<?> getTopBlueBannedHeroes(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<EsportsHeroBanStatResponse> payload = esportsDataService.getTopBlueBannedHeroes(tournamentId, tournamentName, limit);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/top-red-banned-heroes")
    public ResponseEntity<?> getTopRedBannedHeroes(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<EsportsHeroBanStatResponse> payload = esportsDataService.getTopRedBannedHeroes(tournamentId, tournamentName, limit);
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/data/hero-stats")
    public ResponseEntity<?> getHeroStats(@RequestParam(required = false) Long tournamentId,
                                          @RequestParam(required = false) String tournamentName,
                                          @RequestParam(required = false) String teamCode,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        try {
            List<EsportsHeroStatResponse> payload = esportsDataService.getHeroStats(
                    tournamentId,
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

    @GetMapping("/data/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(required = false) String teamCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        try {
            EsportsDashboardResponse payload = esportsDataService.getDashboard(
                    tournamentId,
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
                resolveMatchTournamentName(match),
                match.getTournamentId(),
                EsportsTierSupport.resolveEffectiveTier(match),
                match.getStage()
        );
    }

    private String resolveMatchTournamentName(EsportsMatch match) {
        if (match != null && match.getTournament() != null && match.getTournament().getName() != null && !match.getTournament().getName().isBlank()) {
            return match.getTournament().getName();
        }
        return EsportsTournamentCatalog.resolveTournamentName(EsportsTierSupport.resolveEffectiveTier(match));
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
            Long tournamentId,
            String tournamentTier,
            String stage
    ) {
    }
}
