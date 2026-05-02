package com.example.demo.controller;

import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public REST Controller cho dữ liệu Esports — không yêu cầu đăng nhập.
 * Endpoint: /api/esports
 */
@RestController
@RequestMapping("/api/esports")
public class EsportsController {

    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsMatchRepository esportsMatchRepository;

    public EsportsController(EsportsTeamRepository esportsTeamRepository,
                             EsportsMatchRepository esportsMatchRepository) {
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
    }

    /**
     * GET /api/esports/teams
     * Trả về danh sách các đội tuyển, sắp xếp theo điểm Elo giảm dần.
     */
    @GetMapping("/teams")
    public ResponseEntity<List<EsportsTeam>> getAllTeamsRanked() {
        List<EsportsTeam> teams = esportsTeamRepository.findAllByOrderByScoreDesc();
        return ResponseEntity.ok(teams);
    }

    /**
     * GET /api/esports/matches/recent?limit=10
     * Public recent match feed for the full leaderboard page.
     */
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
                tournamentName(match.getTier()),
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

    private static String tournamentName(String tournamentTier) {
        return switch (String.valueOf(tournamentTier)) {
            case "0" -> "AER International";
            case "2" -> "AER Challenger";
            default -> "AER Pro League";
        };
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
