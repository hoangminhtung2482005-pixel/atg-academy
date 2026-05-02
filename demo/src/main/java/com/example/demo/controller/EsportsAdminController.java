package com.example.demo.controller;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.service.EsportsAdminService;
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

@RestController
@RequestMapping("/api/admin/esports")
public class EsportsAdminController {

    private final EsportsAdminService esportsAdminService;

    public EsportsAdminController(EsportsAdminService esportsAdminService) {
        this.esportsAdminService = esportsAdminService;
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
                    "message", "Đã reset dữ liệu cũ và import " + importedCount + " trận mới.",
                    "importedCount", importedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Import thất bại: " + e.getMessage()));
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
}
