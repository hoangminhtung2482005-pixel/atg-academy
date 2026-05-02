package com.example.demo.service;

import com.example.demo.dto.user.UserProfileResponse;
import com.example.demo.dto.user.UserProfileUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userRepository);
    }

    @Test
    void updateCurrentProfilePersistsDisplayNameAndLevel() {
        User user = user(1L, "player@example.com", "Google Name");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest("  Minh Tùng Hoàng  ", "Vip")
        );

        assertThat(user.getName()).isEqualTo("Google Name");
        assertThat(user.getDisplayName()).isEqualTo("Minh Tùng Hoàng");
        assertThat(user.getLevel()).isEqualTo("Vip");
        assertThat(response.displayName()).isEqualTo("Minh Tùng Hoàng");
        assertThat(response.level()).isEqualTo("Vip");
    }

    @Test
    void updateCurrentProfileRejectsBlankDisplayName() {
        User user = user(1L, "player@example.com", "Google Name");
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateCurrentProfile(
                principal("player@example.com"),
                new UserProfileUpdateRequest("   ", "Normal")
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isEqualTo("Ten hien thi khong duoc de trong");
        });
    }

    @Test
    void getCurrentProfileFallsBackToStoredGoogleName() {
        User user = user(1L, "player@example.com", "Google Name");
        user.setLevel(null);
        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = service.getCurrentProfile(principal("player@example.com"));

        assertThat(response.displayName()).isEqualTo("Google Name");
        assertThat(response.level()).isEqualTo("Normal");
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

    private GoogleUserPrincipal principal(String email) {
        return new GoogleUserPrincipal(email, "Google Name", "", "USER");
    }
}
