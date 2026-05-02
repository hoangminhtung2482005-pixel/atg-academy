package com.example.demo.service;

import com.example.demo.dto.user.UserProfileResponse;
import com.example.demo.dto.user.UserProfileUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
public class UserProfileService {

    private static final int MAX_DISPLAY_NAME_LENGTH = 80;
    private static final Set<String> ALLOWED_LEVELS = Set.of("Normal", "Vip");

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(GoogleUserPrincipal principal) {
        return UserProfileResponse.from(findCurrentUser(principal));
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(GoogleUserPrincipal principal,
                                                   UserProfileUpdateRequest request) {
        User user = findCurrentUser(principal);
        String displayName = normalizeDisplayName(request != null ? request.displayName() : null);

        user.setDisplayName(displayName);

        if (request != null && request.level() != null) {
            user.setLevel(normalizeLevel(request.level()));
        } else if (!StringUtils.hasText(user.getLevel())) {
            user.setLevel("Normal");
        }

        return UserProfileResponse.from(userRepository.save(user));
    }

    private User findCurrentUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Khong tim thay tai khoan hien tai"));
    }

    private String normalizeDisplayName(String value) {
        if (value == null) {
            throw badRequest("Ten hien thi khong duoc de trong");
        }

        String displayName = value.trim();
        if (!StringUtils.hasText(displayName)) {
            throw badRequest("Ten hien thi khong duoc de trong");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw badRequest("Ten hien thi toi da 80 ky tu");
        }
        return displayName;
    }

    private String normalizeLevel(String value) {
        String level = StringUtils.hasText(value) ? value.trim() : "Normal";
        for (String allowedLevel : ALLOWED_LEVELS) {
            if (allowedLevel.equalsIgnoreCase(level)) {
                return allowedLevel;
            }
        }
        throw badRequest("Cap do khong hop le");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
