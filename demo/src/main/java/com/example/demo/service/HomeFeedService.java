package com.example.demo.service;

import com.example.demo.dto.home.HomeFeedItemResponse;
import com.example.demo.entity.Guide;
import com.example.demo.entity.TierList;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.TierListRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class HomeFeedService {

    private static final int DEFAULT_LIMIT = 6;
    private static final String GUIDE_STATUS_PUBLISHED = "PUBLISHED";

    private final TierListRepository tierListRepository;
    private final GuideRepository guideRepository;
    private final TierListCommunityService tierListCommunityService;

    public HomeFeedService(TierListRepository tierListRepository,
                           GuideRepository guideRepository,
                           TierListCommunityService tierListCommunityService) {
        this.tierListRepository = tierListRepository;
        this.guideRepository = guideRepository;
        this.tierListCommunityService = tierListCommunityService;
    }

    @Transactional(readOnly = true)
    public List<HomeFeedItemResponse> getHomeFeed() {
        PageRequest page = PageRequest.of(0, DEFAULT_LIMIT);
        List<TierList> tiers = tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc(page);
        List<Guide> guides = guideRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc(GUIDE_STATUS_PUBLISHED, page);

        List<HomeFeedItemResponse> items = new ArrayList<>(tiers.size() + guides.size());
        tiers.forEach(tierList -> items.add(toTierItem(tierList)));
        guides.forEach(guide -> items.add(toGuideItem(guide)));

        return items.stream()
                .sorted(Comparator.comparing(HomeFeedItemResponse::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HomeFeedItemResponse::id, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(DEFAULT_LIMIT)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private HomeFeedItemResponse toTierItem(TierList tierList) {
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
                preview
        );
    }

    private HomeFeedItemResponse toGuideItem(Guide guide) {
        String description = StringUtils.hasText(guide.getExcerpt())
                ? guide.getExcerpt().trim()
                : "Giáo án cộng đồng với các ghi chú chiến thuật, lựa chọn tướng và cách vận hành trong trận.";
        String image = StringUtils.hasText(guide.getCoverImageUrl()) ? guide.getCoverImageUrl().trim() : "/images/backgrounds/bg-map.jpg";
        Integer readTime = guide.getReadingTimeMinutes() != null && guide.getReadingTimeMinutes() > 0
                ? guide.getReadingTimeMinutes()
                : 5;
        String category = StringUtils.hasText(guide.getCategory()) ? guide.getCategory().trim() : "Chiến thuật";

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
