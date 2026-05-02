package com.example.demo.service;

import com.example.demo.dto.admin.AdminUserResponse;
import com.example.demo.dto.admin.AdminUserUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository);
    }

    @Test
    void updateUserStoresDisplayNameWithoutOverwritingProviderName() {
        User user = new User();
        user.setId(7L);
        user.setEmail("player@example.com");
        user.setName("Google Name");
        user.setRole("User");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = service.updateUser(
                7L,
                new AdminUserUpdateRequest("Custom Name", null, null, null, null),
                null
        );

        assertThat(user.getName()).isEqualTo("Google Name");
        assertThat(user.getDisplayName()).isEqualTo("Custom Name");
        assertThat(response.name()).isEqualTo("Custom Name");
    }

    @Test
    void updateUserRejectsDisplayNameLongerThanEightyCharacters() {
        User user = new User();
        user.setId(7L);
        user.setEmail("player@example.com");
        user.setName("Google Name");
        user.setRole("User");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateUser(
                7L,
                new AdminUserUpdateRequest("x".repeat(81), null, null, null, null),
                null
        )).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isEqualTo("Ten hien thi toi da 80 ky tu");
        });
    }
}
