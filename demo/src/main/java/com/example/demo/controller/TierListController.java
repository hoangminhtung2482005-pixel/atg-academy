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

import java.util.ArrayList;
import java.util.List;
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
            return ResponseEntity.ok(Map.of("exists", false));
        }
        return ResponseEntity.ok(communityService.buildTierListResponse(official.get(), authentication));
    }

    @GetMapping("/community")
    public ResponseEntity<?> getCommunityTierLists(Authentication authentication) {
        List<TierList> list = tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (TierList tierList : list) {
            result.add(communityService.buildTierListResponse(tierList, authentication));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTierListDetail(@PathVariable Long id, Authentication authentication) {
        return tierListRepository.findById(id)
                .map(tierList -> ResponseEntity.ok(communityService.buildTierListResponse(tierList, authentication)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        Authentication authentication,
                                        @RequestBody Map<String, Object> body) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.addComment(id, currentUser, readText(body, "content", "comment")));
    }

    @GetMapping("/{id}/ratings")
    public ResponseEntity<?> getRatings(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(communityService.getRatingSummary(id, authentication));
    }

    @GetMapping("/{id}/ratings/summary")
    public ResponseEntity<?> getRatingSummary(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(communityService.getRatingSummary(id, authentication));
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<?> rateTierList(@PathVariable Long id,
                                          Authentication authentication,
                                          @RequestBody Map<String, Object> body) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(communityService.rateTierList(id, currentUser, readInt(body, "ratingValue", "stars")));
    }

    @PostMapping("/{id}/rate")
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

        String defaultTitle = isOfficialRequest ? "Tier List Meta Hien Tai" : "Tier List cua " + author.resolveDisplayName();
        tierList.setTitle(String.valueOf(body.getOrDefault("title", defaultTitle)));
        if (body.containsKey("description") || body.containsKey("note")) {
            tierList.setDescription(readText(body, "description", "note"));
        }
        tierList.setAuthor(author);
        tierList.setContentData(serializeContentData(heroContentDataService.normalizeForStorage(body.get("contentData"))));
        tierList.setOfficial(isOfficialRequest);
        tierListRepository.save(tierList);

        return ResponseEntity.ok(communityService.buildTierListResponse(tierList, authentication));
    }

    @PutMapping("/{id}")
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTierList(@PathVariable Long id, Authentication authentication) {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        communityService.deleteTierList(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/admin-rate")
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
