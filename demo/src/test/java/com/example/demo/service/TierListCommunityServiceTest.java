package com.example.demo.service;

import com.example.demo.entity.TierList;
import com.example.demo.entity.TierListAdminRating;
import com.example.demo.entity.User;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListCommentRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TierListCommunityServiceTest {

    @Mock
    private TierListRepository tierListRepository;

    @Mock
    private TierListRatingRepository ratingRepository;

    @Mock
    private TierListCommentRepository commentRepository;

    @Mock
    private TierListAdminRatingRepository adminRatingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HeroContentDataService heroContentDataService;

    private TierListCommunityService service;

    @BeforeEach
    void setUp() {
        service = new TierListCommunityService(
                tierListRepository,
                ratingRepository,
                commentRepository,
                adminRatingRepository,
                userRepository,
                objectMapper,
                heroContentDataService
        );
    }

    @Test
    void setAdminRatingRejectsNonAdminUser() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("user@atg.test", "User", "", "USER");

        assertThatThrownBy(() -> service.setAdminRating(1L, principal, 4.0, ""))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(tierListRepository, userRepository, adminRatingRepository);
    }

    @Test
    void setAdminRatingSavesRatingWithoutRequiredNote() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        TierList tierList = new TierList();
        tierList.setId(7L);
        tierList.setTitle("Community Tier");

        User admin = new User();
        admin.setEmail(principal.email());
        admin.setName(principal.name());
        admin.setAvatarUrl(principal.picture());
        admin.setRole("Admin");

        when(tierListRepository.findById(7L)).thenReturn(Optional.of(tierList));
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(admin));
        when(adminRatingRepository.findByTierListId(7L)).thenReturn(Optional.empty());
        when(tierListRepository.save(tierList)).thenReturn(tierList);
        when(adminRatingRepository.save(any(TierListAdminRating.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = service.setAdminRating(7L, principal, 4.0, "");

        ArgumentCaptor<TierListAdminRating> ratingCaptor = ArgumentCaptor.forClass(TierListAdminRating.class);
        verify(adminRatingRepository).save(ratingCaptor.capture());
        TierListAdminRating savedRating = ratingCaptor.getValue();

        assertThat(savedRating.getTierList()).isSameAs(tierList);
        assertThat(savedRating.getAdminUser()).isSameAs(admin);
        assertThat(savedRating.getRatingValue()).isEqualTo(4.0);
        assertThat(savedRating.getNote()).isNull();
        assertThat(tierList.getAdminRating()).isEqualTo(4);
        assertThat(response.get("adminRating")).isEqualTo(4.0);
        assertThat(response.get("adminRatingDetail")).isInstanceOf(Map.class);
    }

    @Test
    void getHighlightedCommunityTierListsSelectsSixUniqueTiersByPriority() {
        TierList official = tierList(99L, "Official", true, LocalDateTime.of(2026, 5, 11, 9, 0));
        TierList newestOne = tierList(10L, "Newest One", false, LocalDateTime.of(2026, 5, 10, 9, 0));
        TierList newestTwo = tierList(9L, "Newest Two", false, LocalDateTime.of(2026, 5, 9, 9, 0));
        TierList extra = tierList(5L, "Extra", false, LocalDateTime.of(2026, 5, 5, 9, 0));
        TierList adminChoice = tierList(4L, "Admin Choice", false, LocalDateTime.of(2026, 5, 4, 9, 0));
        TierList mostFiveStars = tierList(3L, "Five Stars", false, LocalDateTime.of(2026, 5, 3, 9, 0));
        TierList mostRatings = tierList(2L, "Most Ratings", false, LocalDateTime.of(2026, 5, 2, 9, 0));
        TierList topAverage = tierList(1L, "Top Average", false, LocalDateTime.of(2026, 5, 1, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(official, newestOne, newestTwo, extra, adminChoice, mostFiveStars, mostRatings, topAverage));
        when(ratingRepository.findRecentCommunityTierListRatingSummaries(any())).thenReturn(List.of(
                new UserRatingSummary(99L, 5.0, 100L, 100L),
                new UserRatingSummary(mostRatings.getId(), 4.7, 10L, 2L),
                new UserRatingSummary(topAverage.getId(), 5.0, 3L, 3L),
                new UserRatingSummary(mostFiveStars.getId(), 4.6, 7L, 6L)
        ));
        when(adminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(any())).thenReturn(List.of(
                new AdminRatingSummary(99L, 5.0),
                new AdminRatingSummary(topAverage.getId(), 5.0),
                new AdminRatingSummary(adminChoice.getId(), 4.9)
        ));
        stubCardDependencies();

        List<Map<String, Object>> highlights = service.getHighlightedCommunityTierLists(null);

        assertThat(highlights).hasSize(6);
        assertThat(highlights)
                .extracting(item -> item.get("id"), item -> item.get("highlightBadge"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Top điểm TB 30 ngày"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Nhiều đánh giá 30 ngày"),
                        org.assertj.core.groups.Tuple.tuple(3L, "Nhiều 5 sao 30 ngày"),
                        org.assertj.core.groups.Tuple.tuple(4L, "Admin đánh giá cao"),
                        org.assertj.core.groups.Tuple.tuple(10L, "Mới đăng"),
                        org.assertj.core.groups.Tuple.tuple(9L, "Mới đăng")
                );
        assertThat(highlights).extracting(item -> item.get("id")).doesNotContain(99L);
    }

    @Test
    void getHighlightedCommunityTierListsReturnsOnlyNewestSlotsWhenRecentSignalsAreMissing() {
        TierList newestOne = tierList(10L, "Newest One", false, LocalDateTime.of(2026, 5, 10, 9, 0));
        TierList newestTwo = tierList(9L, "Newest Two", false, LocalDateTime.of(2026, 5, 9, 9, 0));
        TierList olderOne = tierList(8L, "Older One", false, LocalDateTime.of(2026, 5, 8, 9, 0));
        TierList olderTwo = tierList(7L, "Older Two", false, LocalDateTime.of(2026, 5, 7, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(newestOne, newestTwo, olderOne, olderTwo));
        when(ratingRepository.findRecentCommunityTierListRatingSummaries(any())).thenReturn(List.of());
        when(adminRatingRepository.findRecentCommunityTierListAdminRatingSummaries(any())).thenReturn(List.of());
        stubCardDependencies();

        List<Map<String, Object>> highlights = service.getHighlightedCommunityTierLists(null);

        assertThat(highlights)
                .extracting(item -> item.get("id"), item -> item.get("highlightBadge"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Mới đăng"),
                        org.assertj.core.groups.Tuple.tuple(9L, "Mới đăng")
                );
        assertThat(highlights).hasSize(2);
    }

    @Test
    void getAllCommunityTierListsReturnsNewestNonOfficialTierLists() {
        TierList older = tierList(1L, "Older", false, LocalDateTime.of(2026, 5, 1, 9, 0));
        TierList newest = tierList(2L, "Newest", false, LocalDateTime.of(2026, 5, 2, 9, 0));
        TierList official = tierList(99L, "Official", true, LocalDateTime.of(2026, 5, 3, 9, 0));

        when(tierListRepository.findByIsOfficialFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(older, newest, official));
        stubCardDependencies();

        List<Map<String, Object>> results = service.getAllCommunityTierLists(null);

        assertThat(results).extracting(item -> item.get("id")).containsExactly(2L, 1L);
    }

    @Test
    void getCurrentUserCommunityTierListsReturnsOnlyCurrentUsersCommunityTierLists() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("player@atg.test", "Player", "", "USER");
        User user = new User();
        user.setId(42L);
        user.setEmail(principal.email());

        TierList newest = tierList(5L, "Newest Mine", false, LocalDateTime.of(2026, 5, 5, 9, 0));
        TierList older = tierList(4L, "Older Mine", false, LocalDateTime.of(2026, 5, 4, 9, 0));
        TierList official = tierList(99L, "Official", true, LocalDateTime.of(2026, 5, 6, 9, 0));

        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(user));
        when(tierListRepository.findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(42L))
                .thenReturn(List.of(older, official, newest));
        stubCardDependencies();

        List<Map<String, Object>> results = service.getCurrentUserCommunityTierLists(principal, null);

        assertThat(results).extracting(item -> item.get("id")).containsExactly(5L, 4L);
    }

    @Test
    void getCurrentUserCommunityTierListsReturnsEmptyWhenCurrentUserDoesNotExistYet() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("new-user@atg.test", "Player", "", "USER");

        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.empty());

        assertThat(service.getCurrentUserCommunityTierLists(principal, null)).isEmpty();
        verifyNoInteractions(tierListRepository);
    }

    private void stubCardDependencies() {
        when(heroContentDataService.enrichForResponse(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ratingRepository.getAverageRating(any())).thenReturn(0.0);
        when(ratingRepository.countByTierListId(any())).thenReturn(0L);
        when(commentRepository.countByTierListId(any())).thenReturn(0L);
        when(adminRatingRepository.findByTierListId(any())).thenReturn(Optional.empty());
    }

    private TierList tierList(Long id, String title, boolean official, LocalDateTime createdAt) {
        TierList tierList = new TierList();
        tierList.setId(id);
        tierList.setTitle(title);
        tierList.setOfficial(official);
        tierList.setCreatedAt(createdAt);
        return tierList;
    }

    private static final class UserRatingSummary implements TierListRatingRepository.RecentCommunityTierListRatingSummary {
        private final Long tierListId;
        private final Double averageRating;
        private final Long ratingCount;
        private final Long fiveStarCount;

        private UserRatingSummary(Long tierListId, Double averageRating, Long ratingCount, Long fiveStarCount) {
            this.tierListId = tierListId;
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
            this.fiveStarCount = fiveStarCount;
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
