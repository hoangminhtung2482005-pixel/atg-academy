package com.example.demo.service;

import com.example.demo.dto.user.UserContentItemResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class UserProfileService {

    private static final int MAX_DISPLAY_NAME_LENGTH = 80;
    private static final Set<String> ALLOWED_LEVELS = Set.of("Normal", "Vip");

    private final UserRepository userRepository;
    private final GuideRepository guideRepository;
    private final TierListRepository tierListRepository;

    public UserProfileService(UserRepository userRepository,
                              GuideRepository guideRepository,
                              TierListRepository tierListRepository) {
        this.userRepository = userRepository;
        this.guideRepository = guideRepository;
        this.tierListRepository = tierListRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(GoogleUserPrincipal principal) {
        return UserProfileResponse.from(findCurrentUser(principal));
    }

    @Transactional(readOnly = true)
    public UserContentSummaryResponse getCurrentContentSummary(GoogleUserPrincipal principal) {
        User user = findCurrentUser(principal);
        Long userId = user.getId();

        long guideCount = guideRepository.countPublishedByAuthorId(userId);
        long tierListCount = tierListRepository.countByAuthorIdAndIsOfficialFalse(userId);
        List<UserContentItemResponse> guides = guideRepository.findPublishedByAuthorIdOrderByLatest(userId)
                .stream()
                .map(this::toGuideItem)
                .toList();
        List<UserContentItemResponse> tierLists = tierListRepository.findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toTierListItem)
                .toList();

        return new UserContentSummaryResponse(guideCount, tierListCount, guides, tierLists);
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

    private UserContentItemResponse toGuideItem(Guide guide) {
        return new UserContentItemResponse(
                guide.getId(),
                guide.getTitle(),
                "guide",
                effectiveGuideStatus(guide),
                "/html/guide-detail.html?id=" + guide.getId(),
                guide.getCreatedAt(),
                guide.getUpdatedAt(),
                guide.getPublishedAt()
        );
    }

    private UserContentItemResponse toTierListItem(TierList tierList) {
        return new UserContentItemResponse(
                tierList.getId(),
                tierList.getTitle(),
                "tierList",
                "PUBLISHED",
                "/html/tier-list-detail.html?id=" + tierList.getId(),
                tierList.getCreatedAt(),
                tierList.getUpdatedAt(),
                null
        );
    }

    private String effectiveGuideStatus(Guide guide) {
        return StringUtils.hasText(guide.getStatus()) ? guide.getStatus() : "PUBLISHED";
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
