package com.example.demo.service;

import com.example.demo.entity.TierList;
import com.example.demo.entity.TierListAdminRating;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSavedTierList;
import com.example.demo.repository.TierListAdminRatingRepository;
import com.example.demo.repository.TierListCommentRepository;
import com.example.demo.repository.TierListRatingRepository;
import com.example.demo.repository.TierListRepository;
import com.example.demo.repository.UserSavedTierListRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    private UserSavedTierListRepository userSavedTierListRepository;

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
                userSavedTierListRepository,
                objectMapper,
                heroContentDataService
        );
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void buildGeneratedOfficialTierListPreviewReturnsGeneratedBoardWithoutPersisting() {
        Map<String, Object> generatedContent = Map.of("rows", List.of(), "columns", List.of());
        when(heroContentDataService.generateOfficialTierListFromHeroScores()).thenReturn(generatedContent);
        when(heroContentDataService.enrichForResponse(generatedContent)).thenReturn(generatedContent);

        Map<String, Object> response = service.buildGeneratedOfficialTierListPreview(null);

        assertThat(response.get("exists")).isEqualTo(false);
        assertThat(response.get("isOfficial")).isEqualTo(true);
        assertThat(response.get("title")).isEqualTo("Tier List Meta");
        assertThat(response.get("creatorName")).isEqualTo("ATG Academy");
        assertThat(response.get("contentData")).isEqualTo(generatedContent);
        verify(heroContentDataService).generateOfficialTierListFromHeroScores();
        verify(tierListRepository, never()).save(any(TierList.class));
    }

    @Test
    void regenerateOfficialTierListFromHeroScoresPersistsOfficialContentData() throws Exception {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        User admin = user(1L, principal.email(), "ATG Admin");
        admin.setRole("Admin");
        Map<String, Object> generatedContent = Map.of("rows", List.of(), "columns", List.of());
        String serializedContent = "{\"rows\":[],\"columns\":[]}";

        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(admin));
        when(tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(heroContentDataService.generateOfficialTierListFromHeroScores()).thenReturn(generatedContent);
        when(objectMapper.writeValueAsString(generatedContent)).thenReturn(serializedContent);
        when(objectMapper.readValue(serializedContent, Object.class)).thenReturn(generatedContent);
        when(tierListRepository.save(any(TierList.class))).thenAnswer(invocation -> {
            TierList tierList = invocation.getArgument(0);
            if (tierList.getId() == null) {
                tierList.setId(99L);
            }
            return tierList;
        });
        stubCardDependencies();

        Map<String, Object> response = service.regenerateOfficialTierListFromHeroScores(principal, null);

        ArgumentCaptor<TierList> tierListCaptor = ArgumentCaptor.forClass(TierList.class);
        verify(tierListRepository).save(tierListCaptor.capture());

        TierList savedTierList = tierListCaptor.getValue();
        assertThat(savedTierList.isOfficial()).isTrue();
        assertThat(savedTierList.getTitle()).isEqualTo("Tier List Meta");
        assertThat(savedTierList.getAuthor()).isSameAs(admin);
        assertThat(savedTierList.getContentData()).isEqualTo(serializedContent);
        assertThat(response.get("isOfficial")).isEqualTo(true);
        assertThat(response.get("contentData")).isEqualTo(generatedContent);
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
        when(ratingRepository.getAverageRating(7L)).thenReturn(4.0);
        when(ratingRepository.countByTierListId(7L)).thenReturn(1L);
        when(adminRatingRepository.getAverageRating(7L)).thenReturn(4.0);
        when(adminRatingRepository.countByTierListId(7L)).thenReturn(1L);

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
        assertThat(response.get("average")).isEqualTo(4.0);
        assertThat(response.get("count")).isEqualTo(2L);
        assertThat(response.get("averageUserRating")).isEqualTo(4.0);
        assertThat(response.get("userRatingCount")).isEqualTo(2L);
    }

    @Test
    void buildTierListResponseIncludesAdminRatingInCombinedAverageAndCount() {
        TierList tierList = tierList(7L, "Community Tier", false, LocalDateTime.of(2026, 5, 7, 9, 0));
        TierListAdminRating adminRating = new TierListAdminRating();
        adminRating.setId(77L);
        adminRating.setTierList(tierList);
        adminRating.setRatingValue(3.0);

        when(heroContentDataService.enrichForResponse(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ratingRepository.getAverageRating(7L)).thenReturn(4.0);
        when(ratingRepository.countByTierListId(7L)).thenReturn(1L);
        when(commentRepository.countByTierListId(7L)).thenReturn(0L);
        when(adminRatingRepository.getAverageRating(7L)).thenReturn(3.0);
        when(adminRatingRepository.countByTierListId(7L)).thenReturn(1L);
        when(adminRatingRepository.findByTierListId(7L)).thenReturn(Optional.of(adminRating));

        Map<String, Object> response = service.buildTierListResponse(tierList, null);

        assertThat(response.get("communityRating")).isEqualTo(3.5);
        assertThat(response.get("totalRatings")).isEqualTo(2L);
        assertThat(response.get("averageUserRating")).isEqualTo(3.5);
        assertThat(response.get("userRatingCount")).isEqualTo(2L);
        assertThat(response.get("userOnlyAverageRating")).isEqualTo(4.0);
        assertThat(response.get("userOnlyRatingCount")).isEqualTo(1L);
        assertThat(response.get("adminRatingCount")).isEqualTo(1L);
        assertThat(response.get("adminRating")).isEqualTo(3.0);
    }

    @Test
    void getRatingSummaryUsesLegacyAdminRatingWhenAdminTableIsEmpty() {
        TierList tierList = tierList(8L, "Legacy Admin Rating", false, LocalDateTime.of(2026, 5, 7, 9, 0));
        tierList.setAdminRating(5);

        when(tierListRepository.findById(8L)).thenReturn(Optional.of(tierList));
        when(ratingRepository.getAverageRating(8L)).thenReturn(null);
        when(ratingRepository.countByTierListId(8L)).thenReturn(0L);
        when(adminRatingRepository.getAverageRating(8L)).thenReturn(null);
        when(adminRatingRepository.countByTierListId(8L)).thenReturn(0L);
        when(adminRatingRepository.findByTierListId(8L)).thenReturn(Optional.empty());

        Map<String, Object> summary = service.getRatingSummary(8L, null);

        assertThat(summary.get("average")).isEqualTo(5.0);
        assertThat(summary.get("count")).isEqualTo(1L);
        assertThat(summary.get("averageUserRating")).isEqualTo(5.0);
        assertThat(summary.get("userRatingCount")).isEqualTo(1L);
        assertThat(summary.get("adminRating")).isEqualTo(5.0);
        assertThat(summary.get("userOnlyAverageRating")).isEqualTo(0.0);
        assertThat(summary.get("userOnlyRatingCount")).isEqualTo(0L);
        assertThat(summary.get("adminRatingCount")).isEqualTo(1L);
    }

    @Test
    void getRatingSummaryDoesNotDoubleCountLegacyAdminRatingWhenAdminRecordExists() {
        TierList tierList = tierList(9L, "No Double Count", false, LocalDateTime.of(2026, 5, 7, 9, 0));
        tierList.setAdminRating(5);

        TierListAdminRating adminRating = new TierListAdminRating();
        adminRating.setId(91L);
        adminRating.setTierList(tierList);
        adminRating.setRatingValue(3.0);

        when(tierListRepository.findById(9L)).thenReturn(Optional.of(tierList));
        when(ratingRepository.getAverageRating(9L)).thenReturn(4.0);
        when(ratingRepository.countByTierListId(9L)).thenReturn(1L);
        when(adminRatingRepository.getAverageRating(9L)).thenReturn(3.0);
        when(adminRatingRepository.countByTierListId(9L)).thenReturn(1L);
        when(adminRatingRepository.findByTierListId(9L)).thenReturn(Optional.of(adminRating));

        Map<String, Object> summary = service.getRatingSummary(9L, null);

        assertThat(summary.get("average")).isEqualTo(3.5);
        assertThat(summary.get("count")).isEqualTo(2L);
        assertThat(summary.get("averageUserRating")).isEqualTo(3.5);
        assertThat(summary.get("userRatingCount")).isEqualTo(2L);
        assertThat(summary.get("adminRating")).isEqualTo(3.0);
        assertThat(summary.get("adminRatingCount")).isEqualTo(1L);
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

    @Test
    void saveTierListCreatesReferenceWithoutCloningTierList() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("xibi@atg.test", "Xibi", "https://avatar/xibi", "USER");
        User currentUser = user(42L, principal.email(), "Xibi");
        TierList tierList = tierListWithAuthor(7L, "ADL Meta tier 1H2026", false,
                LocalDateTime.of(2026, 5, 1, 8, 30), user(9L, "phuc@atg.test", "Phuc Pham"));
        UserSavedTierList savedRelation = new UserSavedTierList();
        savedRelation.setId(100L);
        savedRelation.setUser(currentUser);
        savedRelation.setTierList(tierList);
        savedRelation.setSavedAt(LocalDateTime.of(2026, 5, 7, 9, 15));

        when(tierListRepository.findById(7L)).thenReturn(Optional.of(tierList));
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(currentUser));
        when(userSavedTierListRepository.findByUserIdAndTierListId(42L, 7L)).thenReturn(Optional.empty());
        when(userSavedTierListRepository.save(any(UserSavedTierList.class))).thenReturn(savedRelation);
        stubCardDependencies();

        Map<String, Object> response = service.saveTierList(7L, principal, null);

        assertThat(response.get("saved")).isEqualTo(true);
        assertThat(response.get("savedAt")).isEqualTo(savedRelation.getSavedAt());
        assertThat(response.get("tierListId")).isEqualTo(7L);

        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) response.get("item");
        assertThat(item.get("id")).isEqualTo(7L);
        assertThat(item.get("title")).isEqualTo("ADL Meta tier 1H2026");
        assertThat(item.get("createdAt")).isEqualTo(tierList.getCreatedAt());
        assertThat(item.get("savedAt")).isEqualTo(savedRelation.getSavedAt());
        assertThat(item.get("isSavedByCurrentUser")).isEqualTo(true);
        assertThat(item.get("creatorName")).isEqualTo("Phuc Pham");

        verify(userSavedTierListRepository).save(any(UserSavedTierList.class));
        verify(tierListRepository, never()).save(any(TierList.class));
    }

    @Test
    void saveTierListDoesNotCreateDuplicateWhenAlreadySaved() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("xibi@atg.test", "Xibi", "", "USER");
        User currentUser = user(42L, principal.email(), "Xibi");
        TierList tierList = tierListWithAuthor(7L, "Saved Tier", false,
                LocalDateTime.of(2026, 5, 1, 8, 30), user(9L, "phuc@atg.test", "Phuc Pham"));
        UserSavedTierList savedRelation = new UserSavedTierList();
        savedRelation.setId(100L);
        savedRelation.setUser(currentUser);
        savedRelation.setTierList(tierList);
        savedRelation.setSavedAt(LocalDateTime.of(2026, 5, 7, 9, 15));

        when(tierListRepository.findById(7L)).thenReturn(Optional.of(tierList));
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(currentUser));
        when(userSavedTierListRepository.findByUserIdAndTierListId(42L, 7L)).thenReturn(Optional.of(savedRelation));
        stubCardDependencies();

        Map<String, Object> response = service.saveTierList(7L, principal, null);

        assertThat(response.get("saved")).isEqualTo(true);
        assertThat(response.get("savedAt")).isEqualTo(savedRelation.getSavedAt());
        verify(userSavedTierListRepository, never()).save(any(UserSavedTierList.class));
        verify(tierListRepository, never()).save(any(TierList.class));
    }

    @Test
    void saveTierListRejectsOfficialTierList() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("xibi@atg.test", "Xibi", "", "USER");
        TierList official = tierList(99L, "Tier List Meta", true, LocalDateTime.of(2026, 5, 1, 8, 30));

        when(tierListRepository.findById(99L)).thenReturn(Optional.of(official));

        assertThatThrownBy(() -> service.saveTierList(99L, principal, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userSavedTierListRepository, never()).save(any(UserSavedTierList.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unsaveTierListDeletesOnlyCurrentUsersSavedRelation() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("xibi@atg.test", "Xibi", "", "USER");
        User currentUser = user(42L, principal.email(), "Xibi");
        TierList tierList = tierListWithAuthor(7L, "Saved Tier", false,
                LocalDateTime.of(2026, 5, 1, 8, 30), user(9L, "phuc@atg.test", "Phuc Pham"));

        when(tierListRepository.findById(7L)).thenReturn(Optional.of(tierList));
        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(currentUser));

        Map<String, Object> response = service.unsaveTierList(7L, principal);

        assertThat(response.get("saved")).isEqualTo(false);
        assertThat(response.get("tierListId")).isEqualTo(7L);
        verify(userSavedTierListRepository).deleteByUserIdAndTierListId(42L, 7L);
        verify(tierListRepository, never()).save(any(TierList.class));
    }

    @Test
    void getCurrentUserSavedTierListsReturnsLiveOriginalTierMetadata() {
        GoogleUserPrincipal principal = new GoogleUserPrincipal("xibi@atg.test", "Xibi", "", "USER");
        User currentUser = user(42L, principal.email(), "Xibi");
        User owner = user(9L, "phuc@atg.test", "Phuc Pham");
        TierList tierList = tierListWithAuthor(7L, "ADL Meta tier 1H2026", false,
                LocalDateTime.of(2026, 5, 1, 8, 30), owner);
        tierList.setAdminRating(5);

        UserSavedTierList savedRelation = new UserSavedTierList();
        savedRelation.setId(100L);
        savedRelation.setUser(currentUser);
        savedRelation.setTierList(tierList);
        savedRelation.setSavedAt(LocalDateTime.of(2026, 5, 7, 9, 15));

        when(userRepository.findByEmail(principal.email())).thenReturn(Optional.of(currentUser));
        when(userSavedTierListRepository.findByUserIdOrderBySavedAtDesc(42L)).thenReturn(List.of(savedRelation));
        stubCardDependencies();
        when(ratingRepository.getAverageRating(7L)).thenReturn(4.6);
        when(ratingRepository.countByTierListId(7L)).thenReturn(23L);
        when(commentRepository.countByTierListId(7L)).thenReturn(4L);

        List<Map<String, Object>> results = service.getCurrentUserSavedTierLists(principal, null);

        assertThat(results).hasSize(1);
        Map<String, Object> item = results.get(0);
        assertThat(item.get("id")).isEqualTo(7L);
        assertThat(item.get("title")).isEqualTo("ADL Meta tier 1H2026");
        assertThat(item.get("creatorName")).isEqualTo("Phuc Pham");
        assertThat(item.get("createdAt")).isEqualTo(tierList.getCreatedAt());
        assertThat(item.get("averageUserRating")).isEqualTo(4.6);
        assertThat(item.get("userRatingCount")).isEqualTo(24L);
        assertThat(item.get("adminRating")).isEqualTo(5.0);
        assertThat(item.get("savedAt")).isEqualTo(savedRelation.getSavedAt());
        assertThat(item.get("isSavedByCurrentUser")).isEqualTo(true);
        verify(userSavedTierListRepository).findByUserIdOrderBySavedAtDesc(42L);
    }

    private void stubCardDependencies() {
        when(heroContentDataService.enrichForResponse(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ratingRepository.getAverageRating(anyLong())).thenReturn(0.0);
        when(ratingRepository.countByTierListId(anyLong())).thenReturn(0L);
        when(commentRepository.countByTierListId(anyLong())).thenReturn(0L);
        when(adminRatingRepository.getAverageRating(anyLong())).thenReturn(null);
        when(adminRatingRepository.countByTierListId(anyLong())).thenReturn(0L);
        when(adminRatingRepository.findByTierListId(anyLong())).thenReturn(Optional.empty());
        lenient().when(userSavedTierListRepository.findByUserIdAndTierListIdIn(anyLong(), any())).thenReturn(List.of());
    }

    private TierList tierList(Long id, String title, boolean official, LocalDateTime createdAt) {
        TierList tierList = new TierList();
        tierList.setId(id);
        tierList.setTitle(title);
        tierList.setOfficial(official);
        tierList.setCreatedAt(createdAt);
        return tierList;
    }

    private TierList tierListWithAuthor(Long id, String title, boolean official, LocalDateTime createdAt, User author) {
        TierList tierList = tierList(id, title, official, createdAt);
        tierList.setAuthor(author);
        return tierList;
    }

    private User user(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setDisplayName(name);
        user.setRole("User");
        user.setAvatarUrl("");
        return user;
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
