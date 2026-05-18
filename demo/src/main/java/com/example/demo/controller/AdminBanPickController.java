package com.example.demo.controller;

import com.example.demo.dto.banpick.BanPickSeasonResetExecuteRequest;
import com.example.demo.dto.banpick.BanPickSeasonResetExecuteResponse;
import com.example.demo.dto.banpick.BanPickSeasonResetPreviewResponse;
import com.example.demo.dto.banpick.BanPickRatingSettingsResponse;
import com.example.demo.dto.banpick.BanPickRatingSettingsUpdateRequest;
import com.example.demo.entity.BanPickSeasonResetType;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.BanPickRatingSettingsService;
import com.example.demo.service.BanPickSeasonResetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ban-pick")
public class AdminBanPickController {

    private final BanPickRatingSettingsService banPickRatingSettingsService;
    private final BanPickSeasonResetService banPickSeasonResetService;

    public AdminBanPickController(BanPickRatingSettingsService banPickRatingSettingsService,
                                  BanPickSeasonResetService banPickSeasonResetService) {
        this.banPickRatingSettingsService = banPickRatingSettingsService;
        this.banPickSeasonResetService = banPickSeasonResetService;
    }

    @GetMapping("/rating-settings")
    public ResponseEntity<BanPickRatingSettingsResponse> getRatingSettings() {
        return ResponseEntity.ok(banPickRatingSettingsService.getSettingsView());
    }

    @PutMapping("/rating-settings")
    public ResponseEntity<?> updateRatingSettings(@RequestBody BanPickRatingSettingsUpdateRequest request,
                                                  Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    banPickRatingSettingsService.updateSettings(request, currentAdminEmail(authentication))
            );
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception);
        }
    }

    @PostMapping("/rating-settings/reset-defaults")
    public ResponseEntity<?> resetRatingSettingsToDefaults(Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    banPickRatingSettingsService.resetDefaults(currentAdminEmail(authentication))
            );
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception);
        }
    }

    @GetMapping("/rank-reset/preview")
    public ResponseEntity<?> previewRankReset(@RequestParam String type) {
        try {
            BanPickSeasonResetPreviewResponse preview = banPickSeasonResetService.previewReset(
                    BanPickSeasonResetType.fromValue(type)
            );
            return ResponseEntity.ok(preview);
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception);
        }
    }

    @PostMapping("/rank-reset")
    public ResponseEntity<?> executeRankReset(@RequestBody BanPickSeasonResetExecuteRequest request,
                                              Authentication authentication) {
        try {
            BanPickSeasonResetExecuteResponse response = banPickSeasonResetService.executeReset(
                    BanPickSeasonResetType.fromValue(request != null ? request.type() : null),
                    request != null ? request.confirmationText() : null,
                    currentAdminEmail(authentication),
                    request != null ? request.note() : null
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            return error(HttpStatus.BAD_REQUEST, exception);
        } catch (IllegalStateException exception) {
            return error(HttpStatus.CONFLICT, exception);
        }
    }

    private String currentAdminEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof GoogleUserPrincipal principal) {
            return principal.email();
        }
        return null;
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, Exception exception) {
        return ResponseEntity.status(status).body(Map.of("error", exception.getMessage()));
    }
}
