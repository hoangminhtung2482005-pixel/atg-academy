package com.example.demo.service;

import com.example.demo.dto.user.UserContentSummaryResponse;
import com.example.demo.dto.user.UserProfileResponse;
import com.example.demo.dto.user.UserProfileUpdateRequest;
import com.example.demo.entity.Guide;
import com.example.demo.entity.TierList;
import com.example.demo.entity.User;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.TierListRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuideRepository guideRepository;

    @Mock
    private TierListRepository tierListRepository;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userRepository, guideRepository, tierListRepository);
    }

    @Test
    void updateCurrentProfilePersistsDisplayNameLevelAndPlayerCard() {
        User user = user(1L, "player@example.com", "Google Name");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest(
                        "  Minh Tùng Hoàng  ",
                        "Vip",
                        "shot-caller",
                        "Shot Caller",
                        "",
                        "  ✦ Ban/Pick Captain ✦  "
                )
        );

        assertThat(user.getName()).isEqualTo("Google Name");
        assertThat(user.getDisplayName()).isEqualTo("Minh Tùng Hoàng");
        assertThat(user.getLevel()).isEqualTo("Vip");
        assertThat(user.getPlayerBadgeCode()).isEqualTo("shot-caller");
        assertThat(user.getPlayerBadgeName()).isEqualTo("Shot Caller");
        assertThat(user.getPlayerBadgeIconUrl()).isNull();
        assertThat(user.getPlayerTitle()).isEqualTo("✦ Ban/Pick Captain ✦");
        assertThat(response.displayName()).isEqualTo("Minh Tùng Hoàng");
        assertThat(response.level()).isEqualTo("Vip");
        assertThat(response.playerBadgeCode()).isEqualTo("shot-caller");
        assertThat(response.playerBadgeName()).isEqualTo("Shot Caller");
        assertThat(response.playerBadgeIconUrl()).isNull();
        assertThat(response.playerTitle()).isEqualTo("✦ Ban/Pick Captain ✦");
    }

    @Test
    void updateCurrentProfileRejectsBlankDisplayName() {
        User user = user(1L, "player@example.com", "Google Name");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest("   ", "Normal", null, null, null, null)
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isNotBlank();
        });
    }

    @Test
    void getCurrentProfileFallsBackToStoredGoogleNameAndDefaultPlayerCard() {
        User user = user(1L, "player@example.com", "Google Name");
        user.setLevel(null);
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = service.getCurrentProfile(principal("player@example.com"));

        assertThat(response.displayName()).isEqualTo("Google Name");
        assertThat(response.level()).isEqualTo("Normal");
        assertThat(response.playerBadgeCode()).isEqualTo("default");
        assertThat(response.playerBadgeName()).isEqualTo("ATG Player");
        assertThat(response.playerBadgeIconUrl()).isNull();
        assertThat(response.playerTitle()).isEqualTo("✦ Tân Binh Ban/Pick ✦");
    }

    @Test
    void updateCurrentProfileKeepsExistingPlayerCardWhenRequestOmitsPlayerCardPatch() {
        User user = user(1L, "player@example.com", "Google Name");
        user.setPlayerBadgeCode("meta-reader");
        user.setPlayerBadgeName("Meta Reader");
        user.setPlayerBadgeIconUrl("https://cdn.example.com/meta-reader.png");
        user.setPlayerTitle("✦ Đọc Meta Như Mở Sách ✦");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest("Người Chơi Mới", "Normal", null, null, null, null)
        );

        assertThat(response.playerBadgeCode()).isEqualTo("meta-reader");
        assertThat(response.playerBadgeName()).isEqualTo("Meta Reader");
        assertThat(response.playerBadgeIconUrl()).isEqualTo("https://cdn.example.com/meta-reader.png");
        assertThat(response.playerTitle()).isEqualTo("✦ Đọc Meta Như Mở Sách ✦");
    }

    @Test
    void updateCurrentProfileRejectsUnsupportedPlayerBadgeCode() {
        User user = user(1L, "player@example.com", "Google Name");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest("Google Name", "Normal", "drop table", "Oops", null, "Title")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isNotBlank();
        });
    }

    @Test
    void getCurrentContentSummaryUsesAuthenticatedUserOwnedPostedContent() {
        User user = user(42L, "player@example.com", "Google Name");
        Guide guide = guide(10L, "Guide cua toi", user);
        TierList tierList = tierList(20L, "Tier List cua toi", user);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(guideRepository.countPublishedByAuthorId(42L)).thenReturn(1L);
        when(guideRepository.findPublishedByAuthorIdOrderByLatest(42L)).thenReturn(List.of(guide));
        when(tierListRepository.countByAuthorIdAndIsOfficialFalse(42L)).thenReturn(1L);
        when(tierListRepository.findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(42L)).thenReturn(List.of(tierList));

        UserContentSummaryResponse response = service.getCurrentContentSummary(principal("player@example.com"));

        assertThat(response.guideCount()).isEqualTo(1);
        assertThat(response.tierListCount()).isEqualTo(1);
        assertThat(response.guides()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(10L);
            assertThat(item.type()).isEqualTo("guide");
            assertThat(item.detailUrl()).isEqualTo("/html/guide-detail.html?id=10");
        });
        assertThat(response.tierLists()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(20L);
            assertThat(item.type()).isEqualTo("tierList");
            assertThat(item.detailUrl()).isEqualTo("/html/tier-list-detail.html?id=20");
        });

        verify(guideRepository).countPublishedByAuthorId(42L);
        verify(guideRepository).findPublishedByAuthorIdOrderByLatest(42L);
        verify(tierListRepository).countByAuthorIdAndIsOfficialFalse(42L);
        verify(tierListRepository).findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(42L);
    }

    private User user(Long id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRole("User");
        user.setLevel("Normal");
        return user;
    }

    private Guide guide(Long id, String title, User author) {
        Guide guide = new Guide();
        guide.setId(id);
        guide.setTitle(title);
        guide.setAuthor(author);
        guide.setStatus("PUBLISHED");
        guide.setCreatedAt(LocalDateTime.parse("2026-05-01T10:00:00"));
        guide.setPublishedAt(LocalDateTime.parse("2026-05-02T10:00:00"));
        return guide;
    }

    private TierList tierList(Long id, String title, User author) {
        TierList tierList = new TierList();
        tierList.setId(id);
        tierList.setTitle(title);
        tierList.setAuthor(author);
        tierList.setOfficial(false);
        tierList.setCreatedAt(LocalDateTime.parse("2026-05-03T10:00:00"));
        return tierList;
    }

    private GoogleUserPrincipal principal(String email) {
        return new GoogleUserPrincipal(email, "Google Name", "", "USER");
    }
}
