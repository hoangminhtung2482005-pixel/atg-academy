package com.example.demo.controller;

import com.example.demo.entity.TierList;
import com.example.demo.entity.User;
import com.example.demo.repository.TierListRepository;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.HeroContentDataService;
import com.example.demo.service.TierListCommunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tier-lists")
public class TierListController {

    private final TierListRepository tierListRepository;
    private final ObjectMapper objectMapper;
    private final HeroContentDataService heroContentDataService;
    private final TierListCommunityService communityService;

    public TierListController(TierListRepository tierListRepository,
                              ObjectMapper objectMapper,
                              HeroContentDataService heroContentDataService,
                              TierListCommunityService communityService) {
        this.tierListRepository = tierListRepository;
        this.objectMapper = objectMapper;
        this.heroContentDataService = heroContentDataService;
        this.communityService = communityService;
    }

    @GetMapping("/official")
    public ResponseEntity<?> getOfficialTierList(Authentication authentication) {
        Optional<TierList> official = tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc();
        if (official.isEmpty()) {
            return ResponseEntity.ok(communityService.buildGeneratedOfficialTierListPreview(authentication));
        }
        return ResponseEntity.ok(communityService.buildTierListResponse(official.get(), authentication));
    }

    @GetMapping("/community")
    public ResponseEntity<?> getCommunityTierLists(Authentication authentication) {
        return ResponseEntity.ok(communityService.getHighlightedCommunityTierLists(authentication));
    }

    @GetMapping("/community/all")
    public ResponseEntity<?> getAllCommunityTierLists(Authentication authentication) {
        return ResponseEntity.ok(communityService.getAllCommunityTierLists(authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyCommunityTierLists(Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.getCurrentUserCommunityTierLists(currentUser, authentication));
    }

    @GetMapping("/saved")
    public ResponseEntity<?> getSavedTierLists(Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.getCurrentUserSavedTierLists(currentUser, authentication));
    }

    @PostMapping("/{id:\\d+}/save")
    public ResponseEntity<?> saveTierList(@PathVariable Long id, Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.saveTierList(id, currentUser, authentication));
    }

    @DeleteMapping("/{id:\\d+}/save")
    public ResponseEntity<?> unsaveTierList(@PathVariable Long id, Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.unsaveTierList(id, currentUser));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getTierListDetail(@PathVariable Long id, Authentication authentication) {
        return tierListRepository.findById(id)
                .map(tierList -> ResponseEntity.ok(communityService.buildTierListResponse(tierList, authentication)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id:\\d+}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getComments(id));
    }

    @PostMapping("/{id:\\d+}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        Authentication authentication,
                                        @RequestBody Map<String, Object> body) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.addComment(id, currentUser, readText(body, "content", "comment")));
    }

    @GetMapping("/{id:\\d+}/ratings")
    public ResponseEntity<?> getRatings(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(communityService.getRatingSummary(id, authentication));
    }

    @GetMapping("/{id:\\d+}/ratings/summary")
    public ResponseEntity<?> getRatingSummary(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(communityService.getRatingSummary(id, authentication));
    }

    @PostMapping("/{id:\\d+}/ratings")
    public ResponseEntity<?> rateTierList(@PathVariable Long id,
                                          Authentication authentication,
                                          @RequestBody Map<String, Object> body) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.rateTierList(id, currentUser, readInt(body, "ratingValue", "stars")));
    }

    @PostMapping("/{id:\\d+}/rate")
    public ResponseEntity<?> rateTierListLegacy(@PathVariable Long id,
                                                Authentication authentication,
                                                @RequestBody Map<String, Object> body) {
        return rateTierList(id, authentication, body);
    }

    @PostMapping
    public ResponseEntity<?> createTierList(Authentication authentication,
                                            @RequestBody Map<String, Object> body) throws JacksonException {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        User author = communityService.findOrCreateUser(currentUser);

        boolean isOfficialRequest = currentUser.isAdmin() && Boolean.TRUE.equals(body.get("isOfficial"));
        TierList tierList = isOfficialRequest
                ? tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc().orElseGet(TierList::new)
                : new TierList();

        String defaultTitle = isOfficialRequest ? "Tier List Meta" : "Tier List cua " + author.resolveDisplayName();
        tierList.setTitle(String.valueOf(body.getOrDefault("title", defaultTitle)));
        if (body.containsKey("description") || body.containsKey("note")) {
            tierList.setDescription(readText(body, "description", "note"));
        }
        tierList.setAuthor(author);
        tierList.setContentData(serializeContentData(normalizeContentDataForSave(body, isOfficialRequest)));
        tierList.setOfficial(isOfficialRequest);
        tierListRepository.save(tierList);

        return ResponseEntity.ok(communityService.buildTierListResponse(tierList, authentication));
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<?> updateTierList(@PathVariable Long id,
                                            Authentication authentication,
                                            @RequestBody Map<String, Object> body) throws JacksonException {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);

        Optional<TierList> opt = tierListRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TierList tierList = opt.get();
        boolean isAuthor = tierList.getAuthor() != null && currentUser.email().equals(tierList.getAuthor().getEmail());
        if (!isAuthor && !currentUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Khong co quyen"));
        }

        if (body.containsKey("title")) {
            tierList.setTitle((String) body.get("title"));
        }
        if (body.containsKey("description") || body.containsKey("note")) {
            tierList.setDescription(readText(body, "description", "note"));
        }
        if (body.containsKey("contentData")) {
            tierList.setContentData(serializeContentData(heroContentDataService.normalizeForStorage(body.get("contentData"))));
        }
        tierListRepository.save(tierList);
        return ResponseEntity.ok(communityService.buildTierListResponse(tierList, authentication));
    }

    @DeleteMapping({"/{id:\\d+}", "/community/{id:\\d+}"})
    public ResponseEntity<?> deleteTierList(@PathVariable Long id, Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        communityService.deleteTierList(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id:\\d+}/admin-rate")
    public ResponseEntity<?> adminRateLegacy(@PathVariable Long id,
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

    private String serializeContentData(Object contentData) throws JacksonException {
        if (contentData == null) {
            return "{}";
        }
        if (contentData instanceof String text) {
            return StringUtils.hasText(text) ? text : "{}";
        }
        return objectMapper.writeValueAsString(contentData);
    }

    private Object normalizeContentDataForSave(Map<String, Object> body, boolean isOfficialRequest) {
        Object contentData = body.get("contentData");
        if (isOfficialRequest && isPrimaryRoleImport(body)) {
            return heroContentDataService.normalizeOfficialImportForStorage(contentData);
        }
        return heroContentDataService.normalizeForStorage(contentData);
    }

    private boolean isPrimaryRoleImport(Map<String, Object> body) {
        Object importMode = body.get("importMode");
        return importMode != null && "PRIMARY_ROLE".equalsIgnoreCase(String.valueOf(importMode).trim());
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

    private int readInt(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text && text.matches("\\d+")) {
                return Integer.parseInt(text);
            }
        }
        return 0;
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
