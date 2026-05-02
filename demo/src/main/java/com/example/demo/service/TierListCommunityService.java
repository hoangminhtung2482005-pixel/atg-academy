package com.example.demo.service;

import com.example.demo.entity.TierList;
import com.example.demo.entity.TierListAdminRating;
import com.example.demo.entity.TierListComment;
import com.example.demo.entity.TierListRating;
import com.example.demo.entity.User;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListCommentRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class TierListCommunityService {

    private final TierListRepository tierListRepository;
    private final TierListRatingRepository ratingRepository;
    private final TierListCommentRepository commentRepository;
    private final TierListAdminRatingRepository adminRatingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HeroContentDataService heroContentDataService;

    public TierListCommunityService(TierListRepository tierListRepository,
                                    TierListRatingRepository ratingRepository,
                                    TierListCommentRepository commentRepository,
                                    TierListAdminRatingRepository adminRatingRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper,
                                    HeroContentDataService heroContentDataService) {
        this.tierListRepository = tierListRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.adminRatingRepository = adminRatingRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.heroContentDataService = heroContentDataService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildTierListResponse(TierList tierList, Authentication authentication) {
        Object contentData = heroContentDataService.enrichForResponse(parseContentData(tierList.getContentData()));
        Double average = roundedAverage(tierList.getId());
        long ratingCount = ratingRepository.countByTierListId(tierList.getId());
        Map<String, Object> adminRating = buildAdminRatingDetail(tierList);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", tierList.getId());
        response.put("title", tierList.getTitle());
        response.put("description", firstText(tierList.getDescription(), readText(contentData, "description"), readText(contentData, "note")));
        response.put("note", firstText(tierList.getDescription(), readText(contentData, "note"), readText(contentData, "description")));
        response.put("isOfficial", tierList.isOfficial());
        response.put("adminRating", adminRating != null ? adminRating.get("ratingValue") : null);
        response.put("adminRatingDetail", adminRating);
        response.put("contentData", contentData);
        response.put("previewTiers", buildPreviewTiers(contentData));
        response.put("createdAt", tierList.getCreatedAt());
        response.put("updatedAt", tierList.getUpdatedAt());
        response.put("author", buildUserResponse(tierList.getAuthor()));
        response.put("communityRating", average);
        response.put("totalRatings", ratingCount);
        response.put("averageUserRating", average);
        response.put("userRatingCount", ratingCount);
        response.put("currentUserRating", currentUserRating(tierList.getId(), authentication));
        response.put("commentCount", commentRepository.countByTierListId(tierList.getId()));
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRatingSummary(Long tierListId, Authentication authentication) {
        TierList tierList = findTierList(tierListId);
        return buildRatingSummary(tierList, currentPrincipal(authentication).orElse(null));
    }

    private Map<String, Object> buildRatingSummary(TierList tierList, GoogleUserPrincipal principal) {
        Map<String, Object> adminRating = buildAdminRatingDetail(tierList);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("average", roundedAverage(tierList.getId()));
        summary.put("count", ratingRepository.countByTierListId(tierList.getId()));
        summary.put("averageUserRating", roundedAverage(tierList.getId()));
        summary.put("userRatingCount", ratingRepository.countByTierListId(tierList.getId()));
        summary.put("userRating", currentUserRating(tierList.getId(), principal));
        summary.put("adminRating", adminRating != null ? adminRating.get("ratingValue") : null);
        summary.put("adminRatingDetail", adminRating);
        return summary;
    }

    @Transactional
    public Map<String, Object> rateTierList(Long tierListId, GoogleUserPrincipal principal, int ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So sao phai tu 1 den 5");
        }

        TierList tierList = findTierList(tierListId);
        findOrCreateUser(principal);

        TierListRating rating = ratingRepository.findByTierListIdAndUserId(tierListId, principal.email())
                .orElseGet(TierListRating::new);
        rating.setTierList(tierList);
        rating.setUserId(principal.email());
        rating.setRatingValue(ratingValue);
        ratingRepository.save(rating);

        Map<String, Object> summary = buildRatingSummary(tierList, principal);
        summary.put("userRating", ratingValue);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getComments(Long tierListId) {
        findTierList(tierListId);
        return commentRepository.findByTierListIdOrderByCreatedAtAsc(tierListId)
                .stream()
                .map(this::buildCommentResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> addComment(Long tierListId, GoogleUserPrincipal principal, String content) {
        String normalizedContent = content != null ? content.trim() : "";
        if (!StringUtils.hasText(normalizedContent)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Noi dung binh luan khong duoc de trong");
        }
        if (normalizedContent.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Binh luan toi da 2000 ky tu");
        }

        TierList tierList = findTierList(tierListId);
        User user = findOrCreateUser(principal);

        TierListComment comment = new TierListComment();
        comment.setTierList(tierList);
        comment.setUser(user);
        comment.setContent(normalizedContent);
        return buildCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public Map<String, Object> setAdminRating(Long tierListId,
                                              GoogleUserPrincipal principal,
                                              double ratingValue,
                                              String note) {
        if (principal == null || !principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi Admin");
        }
        if (Double.isNaN(ratingValue) || Double.isInfinite(ratingValue) || ratingValue < 1 || ratingValue > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diem Admin phai tu 1 den 5");
        }

        TierList tierList = findTierList(tierListId);
        User admin = findOrCreateUser(principal);
        double normalizedRating = Math.round(ratingValue * 10.0) / 10.0;

        TierListAdminRating adminRating = adminRatingRepository.findByTierListId(tierListId)
                .orElseGet(TierListAdminRating::new);
        adminRating.setTierList(tierList);
        adminRating.setAdminUser(admin);
        adminRating.setRatingValue(normalizedRating);
        adminRating.setNote(StringUtils.hasText(note) ? note.trim() : null);

        tierList.setAdminRating((int) Math.round(normalizedRating));
        tierListRepository.save(tierList);
        TierListAdminRating saved = adminRatingRepository.save(adminRating);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminRating", rounded(saved.getRatingValue()));
        response.put("adminRatingDetail", buildAdminRatingDetail(saved));
        return response;
    }

    @Transactional
    public User findOrCreateUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email()).map(existing -> {
            boolean changed = false;

            if (!Objects.equals(existing.getName(), principal.name())) {
                existing.setName(principal.name());
                changed = true;
            }
            if (!Objects.equals(existing.getAvatarUrl(), principal.picture())) {
                existing.setAvatarUrl(principal.picture());
                changed = true;
            }

            String normalizedRole = principal.isAdmin() ? "Admin" : principal.isStaff() ? "Staff" : "User";
            if (!Objects.equals(existing.getRole(), normalizedRole)) {
                existing.setRole(normalizedRole);
                changed = true;
            }

            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            User user = new User();
            user.setEmail(principal.email());
            user.setName(principal.name() != null ? principal.name() : "User");
            user.setAvatarUrl(principal.picture());
            user.setRole(principal.isAdmin() ? "Admin" : principal.isStaff() ? "Staff" : "User");
            return userRepository.save(user);
        });
    }

    private TierList findTierList(Long tierListId) {
        return tierListRepository.findById(tierListId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay Tier List"));
    }

    private Map<String, Object> buildCommentResponse(TierListComment comment) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", comment.getId());
        response.put("content", comment.getContent());
        response.put("createdAt", comment.getCreatedAt());
        response.put("updatedAt", comment.getUpdatedAt());
        response.put("user", buildUserResponse(comment.getUser()));
        return response;
    }

    private Map<String, Object> buildUserResponse(User user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.resolveDisplayName());
        response.put("email", user.getEmail());
        response.put("avatar", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        return response;
    }

    private Map<String, Object> buildAdminRatingDetail(TierList tierList) {
        Optional<TierListAdminRating> rating = adminRatingRepository.findByTierListId(tierList.getId());
        if (rating.isPresent()) {
            return buildAdminRatingDetail(rating.get());
        }
        if (tierList.getAdminRating() == null) {
            return null;
        }

        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("ratingValue", rounded(tierList.getAdminRating().doubleValue()));
        legacy.put("note", null);
        legacy.put("admin", null);
        legacy.put("createdAt", tierList.getUpdatedAt());
        legacy.put("updatedAt", tierList.getUpdatedAt());
        return legacy;
    }

    private Map<String, Object> buildAdminRatingDetail(TierListAdminRating rating) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", rating.getId());
        response.put("ratingValue", rounded(rating.getRatingValue()));
        response.put("note", rating.getNote());
        response.put("admin", buildUserResponse(rating.getAdminUser()));
        response.put("createdAt", rating.getCreatedAt());
        response.put("updatedAt", rating.getUpdatedAt());
        return response;
    }

    private Double roundedAverage(Long tierListId) {
        Double avg = ratingRepository.getAverageRating(tierListId);
        return avg != null ? rounded(avg) : 0;
    }

    private double rounded(Double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Integer currentUserRating(Long tierListId, Authentication authentication) {
        return currentPrincipal(authentication)
                .map(principal -> currentUserRating(tierListId, principal))
                .orElse(null);
    }

    private Integer currentUserRating(Long tierListId, GoogleUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        return ratingRepository.findByTierListIdAndUserId(tierListId, principal.email())
                .map(TierListRating::getRatingValue)
                .orElse(null);
    }

    private Optional<GoogleUserPrincipal> currentPrincipal(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof GoogleUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    private Object parseContentData(String contentData) {
        if (!StringUtils.hasText(contentData)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(contentData, Object.class);
        } catch (JacksonException ex) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> buildPreviewTiers(Object contentData) {
        if (!(contentData instanceof Map<?, ?> map)) {
            return List.of();
        }

        Object rowsValue = map.get("rows");
        if (rowsValue instanceof List<?> rows) {
            return rows.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::previewFromRow)
                    .toList();
        }

        Object tiersValue = map.get("tiers");
        if (tiersValue instanceof List<?> tiers) {
            return tiers.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(this::previewFromTier)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> previewFromRow(Map<?, ?> row) {
        List<Object> heroes = new ArrayList<>();
        Object cellsValue = row.get("cells");
        if (cellsValue instanceof List<?> cells) {
            for (Object cell : cells) {
                if (cell instanceof List<?> cellHeroes) {
                    heroes.addAll(cellHeroes);
                }
            }
        }
        return previewTier(row, heroes);
    }

    private Map<String, Object> previewFromTier(Map<?, ?> tier) {
        Object heroesValue = tier.get("heroes");
        List<?> heroes = heroesValue instanceof List<?> list ? list : List.of();
        return previewTier(tier, heroes);
    }

    private Map<String, Object> previewTier(Map<?, ?> source, List<?> heroes) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("label", source.get("label"));
        response.put("color", source.get("color"));
        response.put("heroes", heroes.stream().limit(8).toList());
        return response;
    }

    private String readText(Object contentData, String key) {
        if (contentData instanceof Map<?, ?> map && map.get(key) != null) {
            return String.valueOf(map.get(key));
        }
        return "";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
