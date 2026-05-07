package com.example.demo.service;

import com.example.demo.dto.home.HomeFeedItemResponse;
import com.example.demo.entity.Guide;
import com.example.demo.entity.TierList;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeFeedServiceTest {

    @Mock
    private TierListRepository tierListRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private TierListCommunityService tierListCommunityService;

    @Mock
    private TierListRatingRepository tierListRatingRepository;

    @Mock
    private TierListAdminRatingRepository tierListAdminRatingRepository;

    private HomeFeedService service;

    @BeforeEach
    void setUp() {
        service = new HomeFeedService(
                tierListRepository,
                guideRepository,
                tierListCommunityService,
                tierListRatingRepository,
                tierListAdminRatingRepository
        );
    }

    @Test
    void getHomeFeedMergesTierListsAndGuidesByNewestCreatedAt() {
        TierList newestTier = tierList(11L, "Tier moi", LocalDateTime.of(2026, 4, 30, 10, 0));
        TierList olderTier = tierList(9L, "Tier cu", LocalDateTime.of(2026, 4, 28, 8, 0));
        Guide newestGuide = guide(22L, "Guide moi", LocalDateTime.of(2026, 4, 29, 12, 0));
        Guide olderGuide = guide(18L, "Guide cu", LocalDateTime.of(2026, 4, 27, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc(any())).thenReturn(List.of(newestTier, olderTier));
        when(guideRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(newestGuide, olderGuide));
        stubTierPayload(newestTier, "Minh", 4.8, 12L);
        stubTierPayload(olderTier, "Hoang", 4.1, 4L);

        List<HomeFeedItemResponse> items = service.getHomeFeed();

        assertThat(items)
                .extracting(HomeFeedItemResponse::type, HomeFeedItemResponse::id)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("tier", 11L),
                        org.assertj.core.groups.Tuple.tuple("guide", 22L),
                        org.assertj.core.groups.Tuple.tuple("tier", 9L),
                        org.assertj.core.groups.Tuple.tuple("guide", 18L)
                );
        assertThat(items.get(0).author()).isEqualTo("Minh");
        assertThat(items.get(0).rating()).isEqualTo(4.8);
        assertThat(items.get(1).category()).isEqualTo("Chien thuat");
        assertThat(items.get(1).readTime()).isEqualTo(7);
    }

    @Test
    void getHighlightedCommunityTierListsSelectsThreeUniqueTiersByPriority() {
        TierList newestTier = tierList(33L, "Newest", LocalDateTime.of(2026, 5, 3, 9, 0));
        TierList runnerUpTier = tierList(22L, "Runner Up", LocalDateTime.of(2026, 5, 2, 9, 0));
        TierList adminTier = tierList(11L, "Admin Choice", LocalDateTime.of(2026, 5, 1, 9, 0));
        TierList extraTier = tierList(7L, "Extra", LocalDateTime.of(2026, 4, 28, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(newestTier, runnerUpTier, adminTier, extraTier));
        when(tierListRatingRepository.findRecentCommunityTierListRatingSummaries(any()))
                .thenReturn(List.of(
                        new UserRatingSummary(33L, 5.0, 20L),
                        new UserRatingSummary(22L, 4.8, 12L),
                        new UserRatingSummary(11L, 4.7, 8L)
                ));
        when(tierListAdminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(any()))
                .thenReturn(List.of(
                        new AdminRatingSummary(33L, 5.0),
                        new AdminRatingSummary(22L, 4.9),
                        new AdminRatingSummary(11L, 4.8)
                ));
        stubTierPayload(newestTier, "Lan", 4.9, 20L);
        stubTierPayload(runnerUpTier, "Bao", 4.8, 12L);
        stubTierPayload(adminTier, "Nhi", 4.7, 8L);

        List<HomeFeedItemResponse> items = service.getHighlightedCommunityTierLists();

        assertThat(items)
                .extracting(HomeFeedItemResponse::id, HomeFeedItemResponse::badgeLabel)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(33L, "Mới nhất"),
                        org.assertj.core.groups.Tuple.tuple(22L, "Top đánh giá 30 ngày"),
                        org.assertj.core.groups.Tuple.tuple(11L, "Admin đánh giá cao")
                );
        assertThat(items).hasSize(3);
        assertThat(items).extracting(HomeFeedItemResponse::type).containsOnly("tier");
    }

    @Test
    void getHighlightedCommunityTierListsReturnsFewerThanThreeWhenRecentSignalsAreMissing() {
        TierList newestTier = tierList(33L, "Newest", LocalDateTime.of(2026, 5, 3, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc()).thenReturn(List.of(newestTier));
        when(tierListRatingRepository.findRecentCommunityTierListRatingSummaries(any())).thenReturn(List.of());
        when(tierListAdminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(any())).thenReturn(List.of());
        stubTierPayload(newestTier, "Lan", 4.9, 20L);

        List<HomeFeedItemResponse> items = service.getHighlightedCommunityTierLists();

        assertThat(items)
                .extracting(HomeFeedItemResponse::id, HomeFeedItemResponse::badgeLabel)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(33L, "Mới nhất"));
    }

    private void stubTierPayload(TierList tierList, String author, double rating, long ratingCount) {
        when(tierListCommunityService.buildTierListResponse(tierList, null)).thenReturn(Map.of(
                "author", Map.of("name", author),
                "communityRating", rating,
                "totalRatings", ratingCount,
                "previewTiers", List.of(Map.of("label", "S", "heroes", List.of("Aoi")))
        ));
    }

    private TierList tierList(Long id, String title, LocalDateTime createdAt) {
        TierList tierList = new TierList();
        tierList.setId(id);
        tierList.setTitle(title);
        tierList.setCreatedAt(createdAt);
        return tierList;
    }

    private Guide guide(Long id, String title, LocalDateTime createdAt) {
        Guide guide = new Guide();
        guide.setId(id);
        guide.setTitle(title);
        guide.setCreatedAt(createdAt);
        guide.setExcerpt("Mo ta ngan");
        guide.setCoverImageUrl("/images/backgrounds/sample.jpg");
        guide.setReadingTimeMinutes(7);
        guide.setCategory("Chien thuat");
        return guide;
    }

    private static final class UserRatingSummary implements TierListRatingRepository.RecentCommunityTierListRatingSummary {
        private final Long tierListId;
        private final Double averageRating;
        private final Long ratingCount;
        private final Long fiveStarCount;

        private UserRatingSummary(Long tierListId, Double averageRating, Long ratingCount) {
            this.tierListId = tierListId;
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
            this.fiveStarCount = 0L;
        }

        @Override
        public Long getTierListId() {
            return tierListId;
        }

        @Override
        public Double getAverageRating() {
            return averageRating;
        }

        @Override
        public Long getRatingCount() {
            return ratingCount;
        }

        @Override
        public Long getFiveStarCount() {
            return fiveStarCount;
        }
    }

    private static final class AdminRatingSummary implements TierListAdminRatingRepository.RecentCommunityTierListAdminRatingSummary {
        private final Long tierListId;
        private final Double adminRating;

        private AdminRatingSummary(Long tierListId, Double adminRating) {
            this.tierListId = tierListId;
            this.adminRating = adminRating;
        }

        @Override
        public Long getTierListId() {
            return tierListId;
        }

        @Override
        public Double getAdminRating() {
            return adminRating;
        }
    }
}
