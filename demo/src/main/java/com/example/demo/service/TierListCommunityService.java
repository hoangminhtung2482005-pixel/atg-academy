package com.example.demo.service;

import com.example.demo.entity.TierList;
import com.example.demo.entity.TierListAdminRating;
import com.example.demo.entity.TierListComment;
import com.example.demo.entity.TierListRating;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSavedTierList;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListCommentRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import com.example.demo.repository.UserSavedTierListRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TierListCommunityService {

    private static final int COMMUNITY_HIGHLIGHT_LIMIT = 6;
    private static final int COMMUNITY_NEWEST_SLOT_COUNT = 2;
    private static final int RECENT_RATING_WINDOW_DAYS = 30;
    private static final String OFFICIAL_TIER_LIST_TITLE = "Tier List Meta";
    private static final String OFFICIAL_TIER_LIST_CREATOR = "ATG Academy";
    private static final String TOP_AVERAGE_BADGE = "Top \u0111i\u1ec3m TB 30 ng\u00e0y";
    private static final String MOST_RATINGS_BADGE = "Nhi\u1ec1u \u0111\u00e1nh gi\u00e1 30 ng\u00e0y";
    private static final String MOST_FIVE_STAR_BADGE = "Nhi\u1ec1u 5 sao 30 ng\u00e0y";
    private static final String ADMIN_RATING_BADGE = "Admin \u0111\u00e1nh gi\u00e1 cao";
    private static final String NEWEST_BADGE = "M\u1edbi \u0111\u0103ng";

    private final TierListRepository tierListRepository;
    private final TierListRatingRepository ratingRepository;
    private final TierListCommentRepository commentRepository;
    private final TierListAdminRatingRepository adminRatingRepository;
    private final UserRepository userRepository;
    private final UserSavedTierListRepository userSavedTierListRepository;
    private final ObjectMapper objectMapper;
    private final HeroContentDataService heroContentDataService;

    public TierListCommunityService(TierListRepository tierListRepository,
                                    TierListRatingRepository ratingRepository,
                                    TierListCommentRepository commentRepository,
                                    TierListAdminRatingRepository adminRatingRepository,
                                    UserRepository userRepository,
                                    UserSavedTierListRepository userSavedTierListRepository,
                                    ObjectMapper objectMapper,
                                    HeroContentDataService heroContentDataService) {
        this.tierListRepository = tierListRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.adminRatingRepository = adminRatingRepository;
        this.userRepository = userRepository;
        this.userSavedTierListRepository = userSavedTierListRepository;
        this.objectMapper = objectMapper;
        this.heroContentDataService = heroContentDataService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHighlightedCommunityTierLists(Authentication authentication) {
        List<TierList> communityTiers = tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc()
                .stream()
                .filter(tierList -> tierList != null && !tierList.isOfficial())
                .sorted(this::compareTierNewest)
                .toList();

        if (communityTiers.isEmpty()) {
            return List.of();
        }

        Map<Long, UserSavedTierList> savedTierListsById = resolveSavedTierListMap(authentication, communityTiers);

        Map<Long, TierList> tiersById = communityTiers.stream()
                .filter(tierList -> tierList.getId() != null)
                .collect(Collectors.toMap(
                        TierList::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_RATING_WINDOW_DAYS);
        List<TierListRatingRepository.RecentCommunityTierListRatingSummary> ratingSummaries =
                Optional.ofNullable(ratingRepository.findRecentCommunityTierListRatingSummaries(cutoff)).orElse(List.of());
        List<TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary> adminRatingSummaries =
                Optional.ofNullable(adminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(cutoff)).orElse(List.of());

        Set<Long> selectedIds = new LinkedHashSet<>();
        List<Map<String, Object>> highlights = new ArrayList<>(COMMUNITY_HIGHLIGHT_LIMIT);

        addRecentUserRatingHighlight(ratingSummaries, tiersById, selectedIds, highlights, authentication,
                savedTierListsById, TOP_AVERAGE_BADGE, this::compareByRecentAverageRating);
        addRecentUserRatingHighlight(ratingSummaries, tiersById, selectedIds, highlights, authentication,
                savedTierListsById, MOST_RATINGS_BADGE, this::compareByRecentRatingCount);
        addRecentUserRatingHighlight(
                ratingSummaries.stream().filter(summary -> safeLong(summary.getFiveStarCount()) > 0).toList(),
                tiersById,
                selectedIds,
                highlights,
                authentication,
                savedTierListsById,
                MOST_FIVE_STAR_BADGE,
                this::compareByRecentFiveStarCount
        );
        addRecentAdminRatingHighlight(adminRatingSummaries, tiersById, selectedIds, highlights, authentication, savedTierListsById);
        addNewestCommunityHighlights(communityTiers, selectedIds, highlights, authentication, savedTierListsById);

        return highlights;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCommunityTierLists(Authentication authentication) {
        return mapCommunityTierLists(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc(), authentication);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCurrentUserCommunityTierLists(GoogleUserPrincipal principal,
                                                                      Authentication authentication) {
        if (principal == null) {
            return List.of();
        }

        Optional<User> currentUser = userRepository.findByEmail(principal.email());
        if (currentUser.isEmpty() || currentUser.get().getId() == null) {
            return List.of();
        }

        return mapCommunityTierLists(
                tierListRepository.findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(currentUser.get().getId()),
                authentication
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCurrentUserSavedTierLists(GoogleUserPrincipal principal,
                                                                  Authentication authentication) {
        if (principal == null) {
            return List.of();
        }

        Optional<User> currentUser = userRepository.findByEmail(principal.email());
        if (currentUser.isEmpty() || currentUser.get().getId() == null) {
            return List.of();
        }

        return userSavedTierListRepository.findByUserIdOrderBySavedAtDesc(currentUser.get().getId())
                .stream()
                .filter(savedTierList -> savedTierList.getTierList() != null && !savedTierList.getTierList().isOfficial())
                .map(savedTierList -> buildTierListResponse(savedTierList.getTierList(), authentication, savedTierList))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildGeneratedOfficialTierListPreview(Authentication authentication) {
        return buildOfficialTierListPreviewResponse(
                heroContentDataService.generateOfficialTierListFromHeroScores(),
                authentication
        );
    }

    @Transactional
    public Map<String, Object> regenerateOfficialTierListFromHeroScores(GoogleUserPrincipal principal,
                                                                        Authentication authentication) {
        if (principal == null || !principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi Admin");
        }

        User author = findOrCreateUser(principal);
        TierList tierList = tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc().orElseGet(TierList::new);
        tierList.setTitle(OFFICIAL_TIER_LIST_TITLE);
        tierList.setAuthor(author);
        tierList.setOfficial(true);
        tierList.setContentData(serializeContentData(heroContentDataService.generateOfficialTierListFromHeroScores()));
        tierListRepository.save(tierList);
        return buildTierListResponse(tierList, authentication);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildTierListResponse(TierList tierList, Authentication authentication) {
        UserSavedTierList savedTierList = resolveSavedTierList(authentication, tierList);
        return buildTierListResponse(tierList, authentication, savedTierList);
    }

    private Map<String, Object> buildTierListResponse(TierList tierList,
                                                      Authentication authentication,
                                                      UserSavedTierList savedTierList) {
        Object contentData = heroContentDataService.enrichForResponse(parseContentData(tierList.getContentData()));
        RatingAggregateSummary ratingSummary = buildCombinedRatingSummary(tierList);
        Map<String, Object> adminRating = buildAdminRatingDetail(tierList);
        boolean isSavedByCurrentUser = savedTierList != null;

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
        String creatorName = tierList.getAuthor() != null ? tierList.getAuthor().resolveDisplayName() : "ATG Academy";
        response.put("creatorName", creatorName);
        response.put("creator_name", creatorName);
        response.put("isOwner", isOwner(tierList, authentication));
        response.put("canEdit", canEdit(tierList, authentication));
        response.put("canDelete", canDelete(tierList, authentication));
        response.put("communityRating", ratingSummary.combinedAverage());
        response.put("totalRatings", ratingSummary.totalRatingCount());
        response.put("averageUserRating", ratingSummary.combinedAverage());
        response.put("userRatingCount", ratingSummary.totalRatingCount());
        response.put("userOnlyAverageRating", ratingSummary.userAverage());
        response.put("userOnlyRatingCount", ratingSummary.userRatingCount());
        response.put("adminRatingCount", ratingSummary.adminRatingCount());
        response.put("currentUserRating", currentUserRating(tierList.getId(), authentication));
        response.put("commentCount", commentRepository.countByTierListId(tierList.getId()));
        response.put("saved", isSavedByCurrentUser);
        response.put("isSavedByCurrentUser", isSavedByCurrentUser);
        response.put("savedAt", savedTierList != null ? savedTierList.getSavedAt() : null);
        return response;
    }

    private List<Map<String, Object>> mapCommunityTierLists(List<TierList> tierLists, Authentication authentication) {
        List<TierList> communityTierLists = Optional.ofNullable(tierLists).orElse(List.of())
                .stream()
                .filter(tierList -> tierList != null && !tierList.isOfficial())
                .sorted(this::compareTierNewest)
                .toList();
        Map<Long, UserSavedTierList> savedTierListsById = resolveSavedTierListMap(authentication, communityTierLists);
        return communityTierLists.stream()
                .map(tierList -> buildTierListResponse(tierList, authentication, savedTierListsById.get(tierList.getId())))
                .toList();
    }

    private void addRecentUserRatingHighlight(
            List<TierListRatingRepository.RecentCommunityTierListRatingSummary> summaries,
            Map<Long, TierList> tiersById,
            Set<Long> selectedIds,
            List<Map<String, Object>> highlights,
            Authentication authentication,
            Map<Long, UserSavedTierList> savedTierListsById,
            String badgeLabel,
            RecentUserRatingComparator comparator) {
        if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return;
        }

        summaries.stream()
                .sorted((left, right) -> comparator.compare(left, right, tiersById))
                .map(summary -> tiersById.get(summary.getTierListId()))
                .filter(Objects::nonNull)
                .filter(tierList -> tryAddHighlightedTier(
                        tierList,
                        badgeLabel,
                        selectedIds,
                        highlights,
                        authentication,
                        savedTierListsById
                ))
                .findFirst();
    }

    private void addRecentAdminRatingHighlight(
            List<TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary> summaries,
            Map<Long, TierList> tiersById,
            Set<Long> selectedIds,
            List<Map<String, Object>> highlights,
            Authentication authentication,
            Map<Long, UserSavedTierList> savedTierListsById) {
        if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return;
        }

        summaries.stream()
                .sorted((left, right) -> compareByRecentAdminRating(left, right, tiersById))
                .map(summary -> tiersById.get(summary.getTierListId()))
                .filter(Objects::nonNull)
                .filter(tierList -> tryAddHighlightedTier(
                        tierList,
                        ADMIN_RATING_BADGE,
                        selectedIds,
                        highlights,
                        authentication,
                        savedTierListsById
                ))
                .findFirst();
    }

    private void addNewestCommunityHighlights(List<TierList> communityTiers,
                                              Set<Long> selectedIds,
                                              List<Map<String, Object>> highlights,
                                              Authentication authentication,
                                              Map<Long, UserSavedTierList> savedTierListsById) {
        int added = 0;
        for (TierList tierList : communityTiers) {
            if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT || added >= COMMUNITY_NEWEST_SLOT_COUNT) {
                return;
            }
            if (tryAddHighlightedTier(tierList, NEWEST_BADGE, selectedIds, highlights, authentication, savedTierListsById)) {
                added++;
            }
        }
    }

    private boolean tryAddHighlightedTier(TierList tierList,
                                          String badgeLabel,
                                          Set<Long> selectedIds,
                                          List<Map<String, Object>> highlights,
                                          Authentication authentication,
                                          Map<Long, UserSavedTierList> savedTierListsById) {
        if (tierList == null || tierList.getId() == null || tierList.isOfficial()) {
            return false;
        }
        if (selectedIds.contains(tierList.getId()) || highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return false;
        }

        selectedIds.add(tierList.getId());
        Map<String, Object> response = new LinkedHashMap<>(buildTierListResponse(
                tierList,
                authentication,
                savedTierListsById.get(tierList.getId())
        ));
        response.put("highlightBadge", badgeLabel);
        response.put("badgeLabel", badgeLabel);
        highlights.add(response);
        return true;
    }

    private int compareByRecentAverageRating(
            TierListRatingRepository.RecentCommunityTierListRatingSummary left,
            TierListRatingRepository.RecentCommunityTierListRatingSummary right,
            Map<Long, TierList> tiersById) {
        int averageCompare = Double.compare(safeDouble(right.getAverageRating()), safeDouble(left.getAverageRating()));
        if (averageCompare != 0) {
            return averageCompare;
        }
        int countCompare = Long.compare(safeLong(right.getRatingCount()), safeLong(left.getRatingCount()));
        if (countCompare != 0) {
            return countCompare;
        }
        return compareTierNewest(tiersById.get(left.getTierListId()), tiersById.get(right.getTierListId()));
    }

    private int compareByRecentRatingCount(
            TierListRatingRepository.RecentCommunityTierListRatingSummary left,
            TierListRatingRepository.RecentCommunityTierListRatingSummary right,
            Map<Long, TierList> tiersById) {
        int countCompare = Long.compare(safeLong(right.getRatingCount()), safeLong(left.getRatingCount()));
        if (countCompare != 0) {
            return countCompare;
        }
        int averageCompare = Double.compare(safeDouble(right.getAverageRating()), safeDouble(left.getAverageRating()));
        if (averageCompare != 0) {
            return averageCompare;
        }
        return compareTierNewest(tiersById.get(left.getTierListId()), tiersById.get(right.getTierListId()));
    }

    private int compareByRecentFiveStarCount(
            TierListRatingRepository.RecentCommunityTierListRatingSummary left,
            TierListRatingRepository.RecentCommunityTierListRatingSummary right,
            Map<Long, TierList> tiersById) {
        int fiveStarCompare = Long.compare(safeLong(right.getFiveStarCount()), safeLong(left.getFiveStarCount()));
        if (fiveStarCompare != 0) {
            return fiveStarCompare;
        }
        int averageCompare = Double.compare(safeDouble(right.getAverageRating()), safeDouble(left.getAverageRating()));
        if (averageCompare != 0) {
            return averageCompare;
        }
        int countCompare = Long.compare(safeLong(right.getRatingCount()), safeLong(left.getRatingCount()));
        if (countCompare != 0) {
            return countCompare;
        }
        return compareTierNewest(tiersById.get(left.getTierListId()), tiersById.get(right.getTierListId()));
    }

    private int compareByRecentAdminRating(
            TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary left,
            TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary right,
            Map<Long, TierList> tiersById) {
        int ratingCompare = Double.compare(safeDouble(right.getAdminRating()), safeDouble(left.getAdminRating()));
        if (ratingCompare != 0) {
            return ratingCompare;
        }
        return compareTierNewest(tiersById.get(left.getTierListId()), tiersById.get(right.getTierListId()));
    }

    private int compareTierNewest(TierList left, TierList right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        int createdAtCompare = compareDateDesc(left.getCreatedAt(), right.getCreatedAt());
        if (createdAtCompare != 0) {
            return createdAtCompare;
        }
        return Long.compare(safeId(right.getId()), safeId(left.getId()));
    }

    private int compareDateDesc(LocalDateTime left, LocalDateTime right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0;
    }

    private long safeId(Long value) {
        return value != null ? value : 0;
    }

    private Map<String, Object> buildOfficialTierListPreviewResponse(Object contentData, Authentication authentication) {
        Object enrichedContentData = heroContentDataService.enrichForResponse(contentData);
        boolean canEdit = currentPrincipal(authentication).map(GoogleUserPrincipal::isAdmin).orElse(false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("exists", false);
        response.put("id", null);
        response.put("title", OFFICIAL_TIER_LIST_TITLE);
        response.put("description", "");
        response.put("note", "");
        response.put("isOfficial", true);
        response.put("adminRating", null);
        response.put("adminRatingDetail", null);
        response.put("contentData", enrichedContentData);
        response.put("previewTiers", buildPreviewTiers(enrichedContentData));
        response.put("createdAt", null);
        response.put("updatedAt", null);
        response.put("author", null);
        response.put("creatorName", OFFICIAL_TIER_LIST_CREATOR);
        response.put("creator_name", OFFICIAL_TIER_LIST_CREATOR);
        response.put("isOwner", false);
        response.put("canEdit", canEdit);
        response.put("canDelete", false);
        response.put("communityRating", 0.0);
        response.put("totalRatings", 0L);
        response.put("averageUserRating", 0.0);
        response.put("userRatingCount", 0L);
        response.put("userOnlyAverageRating", 0.0);
        response.put("userOnlyRatingCount", 0L);
        response.put("adminRatingCount", 0L);
        response.put("currentUserRating", null);
        response.put("commentCount", 0L);
        response.put("saved", false);
        response.put("isSavedByCurrentUser", false);
        response.put("savedAt", null);
        return response;
    }

    private String serializeContentData(Object contentData) {
        try {
            return objectMapper.writeValueAsString(contentData);
        } catch (JacksonException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong the luu du lieu Tier List chinh");
        }
    }

    @FunctionalInterface
    private interface RecentUserRatingComparator {
        int compare(TierListRatingRepository.RecentCommunityTierListRatingSummary left,
                    TierListRatingRepository.RecentCommunityTierListRatingSummary right,
                    Map<Long, TierList> tiersById);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRatingSummary(Long tierListId, Authentication authentication) {
        TierList tierList = findTierList(tierListId);
        return buildRatingSummary(tierList, currentPrincipal(authentication).orElse(null));
    }

    private Map<String, Object> buildRatingSummary(TierList tierList, GoogleUserPrincipal principal) {
        RatingAggregateSummary ratingSummary = buildCombinedRatingSummary(tierList);
        Map<String, Object> adminRating = buildAdminRatingDetail(tierList);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("average", ratingSummary.combinedAverage());
        summary.put("count", ratingSummary.totalRatingCount());
        summary.put("totalRatings", ratingSummary.totalRatingCount());
        summary.put("averageUserRating", ratingSummary.combinedAverage());
        summary.put("userRatingCount", ratingSummary.totalRatingCount());
        summary.put("userOnlyAverageRating", ratingSummary.userAverage());
        summary.put("userOnlyRatingCount", ratingSummary.userRatingCount());
        summary.put("adminRatingCount", ratingSummary.adminRatingCount());
        summary.put("userRating", currentUserRating(tierList.getId(), principal));
        summary.put("adminRating", adminRating != null ? adminRating.get("ratingValue") : null);
        summary.put("adminRatingDetail", adminRating);
        return summary;
    }

    @Transactional
    public Map<String, Object> saveTierList(Long tierListId,
                                            GoogleUserPrincipal principal,
                                            Authentication authentication) {
        TierList tierList = findTierList(tierListId);
        if (tierList.isOfficial()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi luu Community Tier List");
        }

        User user = findOrCreateUser(principal);
        UserSavedTierList savedTierList = userSavedTierListRepository.findByUserIdAndTierListId(user.getId(), tierListId)
                .orElseGet(() -> {
                    UserSavedTierList relation = new UserSavedTierList();
                    relation.setUser(user);
                    relation.setTierList(tierList);
                    return userSavedTierListRepository.save(relation);
                });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("saved", true);
        response.put("isSavedByCurrentUser", true);
        response.put("savedAt", savedTierList.getSavedAt());
        response.put("tierListId", tierList.getId());
        response.put("item", buildTierListResponse(tierList, authentication, savedTierList));
        return response;
    }

    @Transactional
    public Map<String, Object> unsaveTierList(Long tierListId, GoogleUserPrincipal principal) {
        TierList tierList = findTierList(tierListId);
        if (tierList.isOfficial()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chi bo luu Community Tier List");
        }

        User user = findOrCreateUser(principal);
        userSavedTierListRepository.deleteByUserIdAndTierListId(user.getId(), tierListId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("saved", false);
        response.put("isSavedByCurrentUser", false);
        response.put("savedAt", null);
        response.put("tierListId", tierList.getId());
        return response;
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

        Map<String, Object> response = new LinkedHashMap<>(buildRatingSummary(tierList, principal));
        response.put("adminRating", rounded(saved.getRatingValue()));
        response.put("adminRatingDetail", buildAdminRatingDetail(saved));
        return response;
    }

    @Transactional
    public void deleteTierList(Long tierListId, GoogleUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }

        TierList tierList = findTierList(tierListId);
        if (tierList.isOfficial()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khong xoa Tier List chinh thuc bang endpoint nay");
        }
        if (!isOwner(tierList, principal) && !principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khong co quyen xoa Tier List nay");
        }

        userSavedTierListRepository.deleteByTierListId(tierListId);
        adminRatingRepository.deleteByTierListId(tierListId);
        commentRepository.deleteByTierListId(tierListId);
        ratingRepository.deleteByTierListId(tierListId);
        tierListRepository.delete(tierList);
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

    private UserSavedTierList resolveSavedTierList(Authentication authentication, TierList tierList) {
        if (tierList == null || tierList.getId() == null) {
            return null;
        }
        return currentPrincipal(authentication)
                .flatMap(this::findExistingUser)
                .flatMap(user -> userSavedTierListRepository.findByUserIdAndTierListId(user.getId(), tierList.getId()))
                .orElse(null);
    }

    private Map<Long, UserSavedTierList> resolveSavedTierListMap(Authentication authentication, List<TierList> tierLists) {
        if (tierLists == null || tierLists.isEmpty()) {
            return Map.of();
        }

        List<Long> tierListIds = tierLists.stream()
                .map(TierList::getId)
                .filter(Objects::nonNull)
                .toList();
        if (tierListIds.isEmpty()) {
            return Map.of();
        }

        Optional<User> currentUser = currentPrincipal(authentication).flatMap(this::findExistingUser);
        if (currentUser.isEmpty() || currentUser.get().getId() == null) {
            return Map.of();
        }

        return userSavedTierListRepository.findByUserIdAndTierListIdIn(currentUser.get().getId(), tierListIds)
                .stream()
                .filter(savedTierList -> savedTierList.getTierList() != null && savedTierList.getTierList().getId() != null)
                .collect(Collectors.toMap(
                        savedTierList -> savedTierList.getTierList().getId(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private Optional<User> findExistingUser(GoogleUserPrincipal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(principal.email());
    }

    private boolean canEdit(TierList tierList, Authentication authentication) {
        return currentPrincipal(authentication)
                .map(principal -> isOwner(tierList, principal) || principal.isAdmin())
                .orElse(false);
    }

    private boolean canDelete(TierList tierList, Authentication authentication) {
        return !tierList.isOfficial() && canEdit(tierList, authentication);
    }

    private boolean isOwner(TierList tierList, Authentication authentication) {
        return currentPrincipal(authentication)
                .map(principal -> isOwner(tierList, principal))
                .orElse(false);
    }

    private boolean isOwner(TierList tierList, GoogleUserPrincipal principal) {
        return principal != null
                && tierList.getAuthor() != null
                && principal.email().equalsIgnoreCase(tierList.getAuthor().getEmail());
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

    private RatingAggregateSummary buildCombinedRatingSummary(TierList tierList) {
        Long tierListId = tierList.getId();
        long userRatingCount = ratingRepository.countByTierListId(tierListId);
        Double rawUserAverage = ratingRepository.getAverageRating(tierListId);

        long adminRatingCount = adminRatingRepository.countByTierListId(tierListId);
        Double rawAdminAverage = adminRatingRepository.getAverageRating(tierListId);

        if (adminRatingCount == 0 && tierList.getAdminRating() != null) {
            adminRatingCount = 1;
            rawAdminAverage = tierList.getAdminRating().doubleValue();
        }

        double userAverage = rawUserAverage != null ? rawUserAverage : 0;
        double adminAverage = rawAdminAverage != null ? rawAdminAverage : 0;
        long totalRatingCount = userRatingCount + adminRatingCount;
        double combinedAverage = totalRatingCount > 0
                ? ((userAverage * userRatingCount) + (adminAverage * adminRatingCount)) / totalRatingCount
                : 0;

        return new RatingAggregateSummary(
                rounded(combinedAverage),
                totalRatingCount,
                rawUserAverage != null ? rounded(rawUserAverage) : 0,
                userRatingCount,
                adminRatingCount
        );
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

    private record RatingAggregateSummary(double combinedAverage,
                                          long totalRatingCount,
                                          double userAverage,
                                          long userRatingCount,
                                          long adminRatingCount) {
    }
}
