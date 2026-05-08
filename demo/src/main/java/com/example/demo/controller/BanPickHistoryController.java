package com.example.demo.controller;

import com.example.demo.dto.banpick.BanPickProfileResponse;
import com.example.demo.dto.banpick.DraftHistoryResponse;
import com.example.demo.dto.banpick.PlayerStatsResponse;
import com.example.demo.dto.banpick.RecordDraftWinnerRequest;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.BanPickHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ban-pick")
public class BanPickHistoryController {

    private final BanPickHistoryService banPickHistoryService;

    public BanPickHistoryController(BanPickHistoryService banPickHistoryService) {
        this.banPickHistoryService = banPickHistoryService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<DraftHistoryResponse>> getCurrentUserHistory(Authentication authentication) {
        return ResponseEntity.ok(banPickHistoryService.getCurrentUserHistory(currentUser(authentication)));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<DraftHistoryResponse> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(banPickHistoryService.getHistory(id));
    }

    @PostMapping("/history/{id}/winner")
    public ResponseEntity<DraftHistoryResponse> recordWinner(@PathVariable Long id,
                                                             @RequestBody RecordDraftWinnerRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(banPickHistoryService.recordWinner(id, request, currentUser(authentication)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<PlayerStatsResponse>> getLeaderboard() {
        return ResponseEntity.ok(banPickHistoryService.getLeaderboard());
    }

    @GetMapping("/profile")
    public ResponseEntity<BanPickProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(banPickHistoryService.getProfile(currentUser(authentication)));
    }

    private GoogleUserPrincipal currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return principal;
    }
}
