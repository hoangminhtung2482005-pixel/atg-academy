package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsFranchiseRequest;
import com.example.demo.dto.esports.EsportsTournamentRequest;
import com.example.demo.dto.esports.EsportsTournamentTeamRequest;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest;
import com.example.demo.dto.esports.EsportsGameDraftRequest;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.service.EsportsAdminService;
import com.example.demo.service.EsportsDraftService;
import com.example.demo.service.EsportsFranchiseService;
import com.example.demo.service.EsportsTournamentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/esports")
public class EsportsAdminController {

    private final EsportsAdminService esportsAdminService;
    private final EsportsDraftService esportsDraftService;
    private final EsportsFranchiseService esportsFranchiseService;
    private final EsportsTournamentService esportsTournamentService;

    public EsportsAdminController(EsportsAdminService esportsAdminService,
                                  EsportsDraftService esportsDraftService,
                                  EsportsFranchiseService esportsFranchiseService,
                                  EsportsTournamentService esportsTournamentService) {
        this.esportsAdminService = esportsAdminService;
        this.esportsDraftService = esportsDraftService;
        this.esportsFranchiseService = esportsFranchiseService;
        this.esportsTournamentService = esportsTournamentService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<EsportsTeam>> getAllTeams() {
        return ResponseEntity.ok(esportsAdminService.getAllTeams());
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(esportsAdminService.getTeamById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams")
    public ResponseEntity<?> addNewTeam(@RequestBody EsportsTeam team) {
        try {
            EsportsTeam created = esportsAdminService.addNewTeam(team);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/franchises")
    public ResponseEntity<?> getFranchises() {
        return ResponseEntity.ok(esportsFranchiseService.getAdminFranchises());
    }

    @PostMapping("/franchises")
    public ResponseEntity<?> createFranchise(@RequestBody EsportsFranchiseRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(esportsFranchiseService.createFranchise(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/franchises/{id}")
    public ResponseEntity<?> updateFranchise(@PathVariable Long id,
                                             @RequestBody EsportsFranchiseRequest request) {
        try {
            return ResponseEntity.ok(esportsFranchiseService.updateFranchise(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/franchises/{id}")
    public ResponseEntity<?> deleteFranchise(@PathVariable Long id) {
        try {
            esportsFranchiseService.deactivateFranchise(id);
            return ResponseEntity.ok(Map.of("message", "Da deactivate franchise voi ID: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tournaments")
    public ResponseEntity<?> getTournaments(@RequestParam(required = false) Long franchiseId,
                                            @RequestParam(required = false) String franchiseCode) {
        return ResponseEntity.ok(esportsTournamentService.getAdminTournaments(franchiseId, franchiseCode));
    }

    @PostMapping("/tournaments")
    public ResponseEntity<?> createTournament(@RequestBody EsportsTournamentRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(esportsTournamentService.createTournament(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tournaments/{id}")
    public ResponseEntity<?> updateTournament(@PathVariable Long id,
                                              @RequestBody EsportsTournamentRequest request) {
        try {
            return ResponseEntity.ok(esportsTournamentService.updateTournament(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tournaments/{id}")
    public ResponseEntity<?> deleteTournament(@PathVariable Long id) {
        try {
            esportsTournamentService.deleteTournament(id);
            return ResponseEntity.ok(Map.of("message", "Da xoa tournament voi ID: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/tournaments/{id}/teams")
    public ResponseEntity<?> getTournamentTeams(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(esportsTournamentService.listTournamentTeams(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/tournaments/{id}/teams")
    public ResponseEntity<?> addTeamToTournament(@PathVariable Long id,
                                                 @RequestBody EsportsTournamentTeamRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(esportsTournamentService.addTeamToTournament(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tournaments/{id}/teams/{teamId}")
    public ResponseEntity<?> removeTeamFromTournament(@PathVariable Long id,
                                                      @PathVariable Long teamId) {
        try {
            esportsTournamentService.removeTeamFromTournament(id, teamId);
            return ResponseEntity.ok(Map.of("message", "Da xoa team khoi tournament."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/teams/{id}")
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @RequestBody EsportsTeam team) {
        try {
            return ResponseEntity.ok(esportsAdminService.updateTeam(id, team));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/teams/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        try {
            esportsAdminService.deleteTeam(id);
            return ResponseEntity.ok(Map.of("message", "Da xoa doi tuyen voi ID: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teams/matches/bulk-import")
    public ResponseEntity<?> bulkImportMatches(@RequestBody String rawData) {
        try {
            int importedCount = esportsAdminService.importMatchesFromText(rawData);
            return ResponseEntity.ok(Map.of(
                    "message", "Da reset du lieu cu va import " + importedCount + " tran moi.",
                    "importedCount", importedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Import that bai: " + e.getMessage()));
        }
    }

    @GetMapping("/matches")
    public ResponseEntity<List<EsportsMatch>> getAllMatches() {
        return ResponseEntity.ok(esportsAdminService.getAllMatches());
    }

    @DeleteMapping("/matches")
    public ResponseEntity<?> resetMatchHistory() {
        long deletedCount = esportsAdminService.resetMatchHistory();
        return ResponseEntity.ok(Map.of(
                "message", "Da xoa toan bo Match History va reset Elo.",
                "deletedCount", deletedCount
        ));
    }

    @GetMapping("/matches/{id}")
    public ResponseEntity<?> getMatchById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(esportsAdminService.getMatchById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/matches")
    public ResponseEntity<?> addMatch(@RequestBody EsportsMatch match) {
        try {
            EsportsMatch created = esportsAdminService.addMatch(match);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/matches/{id}")
    public ResponseEntity<?> updateMatch(@PathVariable Long id, @RequestBody EsportsMatch match) {
        try {
            return ResponseEntity.ok(esportsAdminService.updateMatch(id, match));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/matches/{id}")
    public ResponseEntity<?> deleteMatch(@PathVariable Long id) {
        try {
            esportsAdminService.deleteMatch(id);
            return ResponseEntity.ok(Map.of("message", "Da xoa tran dau voi ID: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/matches/{matchId}/game-drafts")
    public ResponseEntity<?> getGameDraftsByMatchId(@PathVariable Long matchId) {
        try {
            return ResponseEntity.ok(esportsDraftService.listGameDraftsByMatch(matchId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PostMapping("/matches/{matchId}/game-drafts")
    public ResponseEntity<?> createGameDraft(@PathVariable Long matchId,
                                             @RequestBody EsportsGameDraftRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(esportsDraftService.createGameDraft(matchId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping("/game-drafts/{gameDraftId}")
    public ResponseEntity<?> getGameDraft(@PathVariable Long gameDraftId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getGameDraft(gameDraftId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PostMapping(value = "/game-drafts/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewGameDraftImport(@RequestPart("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        try {
            return ResponseEntity.ok(esportsDraftService.previewGameDraftImport(file, overwriteExisting));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @PostMapping("/game-drafts/import/confirm")
    public ResponseEntity<?> confirmGameDraftImport(@RequestBody EsportsGameDraftImportConfirmRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.confirmGameDraftImport(request));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping("/game-drafts/export")
    public ResponseEntity<?> exportGameDraftsCsv(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String tournamentName,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        try {
            byte[] csvBytes = esportsDraftService.exportGameDraftsCsv(tournamentId, tournamentName, matchId, dateFrom, dateTo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"esports-game-drafts.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvBytes);
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @PutMapping("/game-drafts/{gameDraftId}")
    public ResponseEntity<?> updateGameDraft(@PathVariable Long gameDraftId,
                                             @RequestBody EsportsGameDraftRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.updateGameDraft(gameDraftId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @DeleteMapping("/game-drafts/{gameDraftId}")
    public ResponseEntity<?> deleteGameDraft(@PathVariable Long gameDraftId) {
        try {
            esportsDraftService.deleteGameDraft(gameDraftId);
            return ResponseEntity.ok(Map.of("message", "Da xoa game draft voi ID: " + gameDraftId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(Map.of("error", exception.getMessage()));
    }
}
