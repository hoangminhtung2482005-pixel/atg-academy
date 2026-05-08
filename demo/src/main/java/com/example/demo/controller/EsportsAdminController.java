package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsDraftActionRequest;
import com.example.demo.dto.esports.EsportsDraftActionUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameLineupUpsertRequest;
import com.example.demo.dto.esports.EsportsMatchGameRequest;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.service.EsportsAdminService;
import com.example.demo.service.EsportsDraftService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/esports")
public class EsportsAdminController {

    private final EsportsAdminService esportsAdminService;
    private final EsportsDraftService esportsDraftService;

    public EsportsAdminController(EsportsAdminService esportsAdminService,
                                  EsportsDraftService esportsDraftService) {
        this.esportsAdminService = esportsAdminService;
        this.esportsDraftService = esportsDraftService;
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
            return ResponseEntity.ok(Map.of("message", "Đã xóa đội tuyển với ID: " + id));
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
                "message", "Đã xóa toàn bộ Match History và reset Elo.",
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
            return ResponseEntity.ok(Map.of("message", "Đã xóa trận đấu với ID: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/matches/{matchId}/games")
    public ResponseEntity<?> getGamesByMatchId(@PathVariable Long matchId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getGamesByMatchId(matchId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PostMapping("/matches/{matchId}/games")
    public ResponseEntity<?> createGame(@PathVariable Long matchId,
                                        @RequestBody EsportsMatchGameRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(esportsDraftService.createGame(matchId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<?> getGame(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getGame(gameId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<?> updateGame(@PathVariable Long gameId,
                                        @RequestBody EsportsMatchGameRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.updateGame(gameId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable Long gameId) {
        try {
            esportsDraftService.deleteGame(gameId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa ván đấu với ID: " + gameId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @GetMapping("/games/{gameId}/draft-actions")
    public ResponseEntity<?> getDraftActionsByGameId(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getDraftActionsByGameId(gameId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PutMapping("/games/{gameId}/draft-actions")
    public ResponseEntity<?> replaceDraftActions(@PathVariable Long gameId,
                                                 @RequestBody EsportsDraftActionUpsertRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.replaceDraftActions(gameId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @GetMapping("/games/{gameId}/lineups")
    public ResponseEntity<?> getLineupsByGameId(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(esportsDraftService.getLineupsByGameId(gameId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    @PutMapping("/games/{gameId}/lineups")
    public ResponseEntity<?> upsertLineups(@PathVariable Long gameId,
                                           @RequestBody EsportsMatchGameLineupUpsertRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.upsertLineups(gameId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @PostMapping("/games/{gameId}/draft-actions")
    public ResponseEntity<?> createDraftAction(@PathVariable Long gameId,
                                               @RequestBody EsportsDraftActionRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(esportsDraftService.createDraftAction(gameId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @PutMapping("/draft-actions/{actionId}")
    public ResponseEntity<?> updateDraftAction(@PathVariable Long actionId,
                                               @RequestBody EsportsDraftActionRequest request) {
        try {
            return ResponseEntity.ok(esportsDraftService.updateDraftAction(actionId, request));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e);
        }
    }

    @DeleteMapping("/draft-actions/{actionId}")
    public ResponseEntity<?> deleteDraftAction(@PathVariable Long actionId) {
        try {
            esportsDraftService.deleteDraftAction(actionId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa draft action với ID: " + actionId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e);
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(Map.of("error", exception.getMessage()));
    }
}
