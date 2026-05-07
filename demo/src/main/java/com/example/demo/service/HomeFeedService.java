package com.example.demo.service;

import com.example.demo.dto.home.HomeFeedItemResponse;
import com.example.demo.entity.Guide;
import com.example.demo.entity.TierList;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HomeFeedService {

    private static final int DEFAULT_LIMIT = 6;
    private static final int COMMUNITY_HIGHLIGHT_LIMIT = 3;
    private static final int RECENT_RATING_WINDOW_DAYS = 30;
    private static final String GUIDE_STATUS_PUBLISHED = "PUBLISHED";
    private static final String LATEST_BADGE = "M\u1edbi nh\u1ea5t";
    private static final String USER_RATING_BADGE = "Top \u0111\u00e1nh gi\u00e1 30 ng\u00e0y";
    private static final String ADMIN_RATING_BADGE = "Admin \u0111\u00e1nh gi\u00e1 cao";

    private final TierListRepository tierListRepository;
    private final GuideRepository guideRepository;
    private final TierListCommunityService tierListCommunityService;
    private final TierListRatingRepository tierListRatingRepository;
    private final TierListAdminRatingRepository tierListAdminRatingRepository;

    public HomeFeedService(TierListRepository tierListRepository,
                           GuideRepository guideRepository,
                           TierListCommunityService tierListCommunityService,
                           TierListRatingRepository tierListRatingRepository,
                           TierListAdminRatingRepository tierListAdminRatingRepository) {
        this.tierListRepository = tierListRepository;
        this.guideRepository = guideRepository;
        this.tierListCommunityService = tierListCommunityService;
        this.tierListRatingRepository = tierListRatingRepository;
        this.tierListAdminRatingRepository = tierListAdminRatingRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeFeedItemResponse> getHomeFeed() {
        PageRequest page = PageRequest.of(0, DEFAULT_LIMIT);
        List<TierList> tiers = tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc(page);
        List<Guide> guides = guideRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc(GUIDE_STATUS_PUBLISHED, page);

        List<HomeFeedItemResponse> items = new ArrayList<>(tiers.size() + guides.size());
        tiers.forEach(tierList -> items.add(toTierItem(tierList, null)));
        guides.forEach(guide -> items.add(toGuideItem(guide)));

        return items.stream()
                .sorted(Comparator.comparing(HomeFeedItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HomeFeedItemResponse::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DEFAULT_LIMIT)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeFeedItemResponse> getHighlightedCommunityTierLists() {
        List<TierList> communityTiers = tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc()
                .stream()
                .sorted(Comparator.comparing(TierList::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TierList::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (communityTiers.isEmpty()) {
            return List.of();
        }

        Map<Long, TierList> tiersById = communityTiers.stream()
                .filter(tierList -> tierList.getId() != null)
                .collect(Collectors.toMap(
                        TierList::getId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        Set<Long> selectedIds = new LinkedHashSet<>();
        List<HomeFeedItemResponse> highlights = new ArrayList<>(COMMUNITY_HIGHLIGHT_LIMIT);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_RATING_WINDOW_DAYS);

        tryAddHighlightedTier(communityTiers.get(0), LATEST_BADGE, selectedIds, highlights);
        addHighestRecentUserRatedTier(cutoff, tiersById, selectedIds, highlights);
        addHighestRecentAdminRatedTier(cutoff, tiersById, selectedIds, highlights);

        return highlights;
    }

    private void addHighestRecentUserRatedTier(LocalDateTime cutoff,
                                               Map<Long, TierList> tiersById,
                                               Set<Long> selectedIds,
                                               List<HomeFeedItemResponse> highlights) {
        if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return;
        }

        for (TierListRatingRepository.RecentCommunityTierListRatingSummary summary
                : tierListRatingRepository.findRecentCommunityTierListRatingSummaries(cutoff)) {
            if (tryAddHighlightedTier(tiersById.get(summary.getTierListId()), USER_RATING_BADGE, selectedIds, highlights)) {
                return;
            }
        }
    }

    private void addHighestRecentAdminRatedTier(LocalDateTime cutoff,
                                                Map<Long, TierList> tiersById,
                                                Set<Long> selectedIds,
                                                List<HomeFeedItemResponse> highlights) {
        if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return;
        }

        for (TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary summary
                : tierListAdminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(cutoff)) {
            if (tryAddHighlightedTier(tiersById.get(summary.getTierListId()), ADMIN_RATING_BADGE, selectedIds, highlights)) {
                return;
            }
        }
    }

    private boolean tryAddHighlightedTier(TierList tierList,
                                          String badgeLabel,
                                          Set<Long> selectedIds,
                                          List<HomeFeedItemResponse> highlights) {
        if (tierList == null || tierList.getId() == null || selectedIds.contains(tierList.getId())) {
            return false;
        }
        if (highlights.size() >= COMMUNITY_HIGHLIGHT_LIMIT) {
            return false;
        }

        selectedIds.add(tierList.getId());
        highlights.add(toTierItem(tierList, badgeLabel));
        return true;
    }

    @SuppressWarnings("unchecked")
    private HomeFeedItemResponse toTierItem(TierList tierList, String badgeLabel) {
        Map<String, Object> payload = tierListCommunityService.buildTierListResponse(tierList, null);
        Map<String, Object> author = payload.get("author") instanceof Map<?, ?> authorMap
                ? (Map<String, Object>) authorMap
                : Map.of();
        List<Map<String, Object>> preview = payload.get("previewTiers") instanceof List<?> rows
                ? rows.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(row -> (Map<String, Object>) row)
                .toList()
                : List.of();
        Number ratingCountValue = payload.get("totalRatings") instanceof Number number ? number : Long.valueOf(0);
        Number ratingValue = payload.get("communityRating") instanceof Number number ? number : Double.valueOf(0);

        return HomeFeedItemResponse.tier(
                tierList.getId(),
                tierList.getTitle(),
                String.valueOf(author.getOrDefault("name", "ATG Member")),
                tierList.getCreatedAt(),
                ratingValue.doubleValue(),
                ratingCountValue.longValue(),
                preview,
                badgeLabel
        );
    }

    private HomeFeedItemResponse toGuideItem(Guide guide) {
        String description = StringUtils.hasText(guide.getExcerpt())
                ? guide.getExcerpt().trim()
                : "GiÃ¡o Ã¡n cá»™ng Ä‘á»“ng vá»›i cÃ¡c ghi chÃº chiáº¿n thuáº­t, lá»±a chá»n tÆ°á»›ng vÃ  cÃ¡ch váº­n hÃ nh trong tráº­n.";
        String image = StringUtils.hasText(guide.getCoverImageUrl()) ? guide.getCoverImageUrl().trim() : "/images/backgrounds/bg-map.jpg";
        Integer readTime = guide.getReadingTimeMinutes() != null && guide.getReadingTimeMinutes() > 0
                ? guide.getReadingTimeMinutes()
                : 5;
        String category = StringUtils.hasText(guide.getCategory()) ? guide.getCategory().trim() : "Chiáº¿n thuáº­t";

        return HomeFeedItemResponse.guide(
                guide.getId(),
                guide.getTitle(),
                description,
                image,
                guide.getCreatedAt(),
                readTime,
                category
        );
    }
}
