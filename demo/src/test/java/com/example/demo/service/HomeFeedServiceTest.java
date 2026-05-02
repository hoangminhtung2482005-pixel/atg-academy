package com.example.demo.service;

import com.example.demo.dto.home.HomeFeedItemResponse;
import com.example.demo.entity.Guide;
import com.example.demo.entity.TierList;
import com.example.demo.repository.GuideRepository;
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

    private HomeFeedService service;

    @BeforeEach
    void setUp() {
        service = new HomeFeedService(tierListRepository, guideRepository, tierListCommunityService);
    }

    @Test
    void getHomeFeedMergesTierListsAndGuidesByNewestCreatedAt() {
        TierList newestTier = tierList(11L, "Tier mới", LocalDateTime.of(2026, 4, 30, 10, 0));
        TierList olderTier = tierList(9L, "Tier cũ", LocalDateTime.of(2026, 4, 28, 8, 0));
        Guide newestGuide = guide(22L, "Guide mới", LocalDateTime.of(2026, 4, 29, 12, 0));
        Guide olderGuide = guide(18L, "Guide cũ", LocalDateTime.of(2026, 4, 27, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc(any())).thenReturn(List.of(newestTier, olderTier));
        when(guideRepository.findByStatusIgnoreCaseOrderByCreatedAtDesc(any(), any())).thenReturn(List.of(newestGuide, olderGuide));
        when(tierListCommunityService.buildTierListResponse(newestTier, null)).thenReturn(Map.of(
                "author", Map.of("name", "Minh"),
                "communityRating", 4.8,
                "totalRatings", 12L,
                "previewTiers", List.of(Map.of("label", "S", "heroes", List.of("Aoi")))
        ));
        when(tierListCommunityService.buildTierListResponse(olderTier, null)).thenReturn(Map.of(
                "author", Map.of("name", "Hoàng"),
                "communityRating", 4.1,
                "totalRatings", 4L,
                "previewTiers", List.of()
        ));

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
        assertThat(items.get(1).category()).isEqualTo("Chiến thuật");
        assertThat(items.get(1).readTime()).isEqualTo(7);
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
        guide.setExcerpt("Mô tả ngắn");
        guide.setCoverImageUrl("/images/backgrounds/sample.jpg");
        guide.setReadingTimeMinutes(7);
        guide.setCategory("Chiến thuật");
        return guide;
    }
}
