package com.example.demo.controller;

import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.TierListCommunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tier-lists")
public class AdminTierListController {

    private final TierListCommunityService communityService;

    public AdminTierListController(TierListCommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping("/official/regenerate-from-hero-scores")
    public ResponseEntity<?> regenerateOfficialTierListFromHeroScores(Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.regenerateOfficialTierListFromHeroScores(currentUser, authentication));
    }

    @PutMapping("/{id}/admin-rating")
    public ResponseEntity<?> setAdminRating(@PathVariable Long id,
                                            Authentication authentication,
                                            @RequestBody Map<String, Object> body) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.setAdminRating(
                id,
                currentUser,
                readDouble(body, "ratingValue", "stars", "adminRating"),
                readText(body, "note")
        ));
    }

    private GoogleUserPrincipal getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        return principal;
    }

    private String readText(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private double readDouble(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String text) {
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
