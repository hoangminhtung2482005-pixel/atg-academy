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
import com.example.demo.support.PlayerCardDefaults;
import com.example.demo.support.PlayerCardDefaults.PlayerBadgePreset;
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

        applyPlayerCardConfig(user, request);
        return UserProfileResponse.from(userRepository.save(user));
    }

    private User findCurrentUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản hiện tại"));
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
            throw badRequest("Tên hiển thị không được để trống");
        }

        String displayName = value.trim();
        if (!StringUtils.hasText(displayName)) {
            throw badRequest("Tên hiển thị không được để trống");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw badRequest("Tên hiển thị tối đa 80 ký tự");
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
        throw badRequest("Cấp độ không hợp lệ");
    }

    private void applyPlayerCardConfig(User user, UserProfileUpdateRequest request) {
        if (user == null) {
            return;
        }

        if (request == null) {
            ensurePlayerCardDefaults(user);
            return;
        }

        boolean hasPlayerCardPatch = request.playerBadgeCode() != null
                || request.playerBadgeName() != null
                || request.playerBadgeIconUrl() != null
                || request.playerTitle() != null;

        if (!hasPlayerCardPatch) {
            ensurePlayerCardDefaults(user);
            return;
        }

        PlayerBadgePreset preset = resolveBadgePreset(request.playerBadgeCode(), user);
        user.setPlayerBadgeCode(preset.code());
        user.setPlayerBadgeName(resolveBadgeName(request.playerBadgeName(), user, preset));
        user.setPlayerBadgeIconUrl(resolveBadgeIconUrl(request.playerBadgeIconUrl(), user, preset));
        user.setPlayerTitle(resolvePlayerTitle(request.playerTitle(), user));
    }

    private void ensurePlayerCardDefaults(User user) {
        if (!StringUtils.hasText(user.getPlayerBadgeCode())) {
            user.setPlayerBadgeCode(PlayerCardDefaults.DEFAULT_BADGE_CODE);
        }
        if (!StringUtils.hasText(user.getPlayerBadgeName())) {
            user.setPlayerBadgeName(PlayerCardDefaults.resolvePreset(user.getPlayerBadgeCode()).name());
        }
        if (!StringUtils.hasText(user.getPlayerTitle())) {
            user.setPlayerTitle(PlayerCardDefaults.DEFAULT_TITLE);
        }
        if (user.getPlayerBadgeIconUrl() != null && user.getPlayerBadgeIconUrl().isBlank()) {
            user.setPlayerBadgeIconUrl(null);
        }
    }

    private PlayerBadgePreset resolveBadgePreset(String requestedCode, User user) {
        String candidateCode = requestedCode != null
                ? normalizeBadgeCode(requestedCode)
                : user.resolvePlayerBadgeCode();
        if (!PlayerCardDefaults.isSupportedBadgeCode(candidateCode)) {
            throw badRequest("Badge Player Card khong hop le");
        }
        return PlayerCardDefaults.resolvePreset(candidateCode);
    }

    private String resolveBadgeName(String requestedBadgeName, User user, PlayerBadgePreset preset) {
        if (requestedBadgeName == null) {
            return StringUtils.hasText(user.getPlayerBadgeName()) ? user.resolvePlayerBadgeName() : preset.name();
        }
        String badgeName = normalizeOptionalText(
                requestedBadgeName,
                PlayerCardDefaults.MAX_BADGE_NAME_LENGTH,
                "Ten badge toi da 80 ky tu"
        );
        return StringUtils.hasText(badgeName) ? badgeName : preset.name();
    }

    private String resolveBadgeIconUrl(String requestedIconUrl, User user, PlayerBadgePreset preset) {
        if (requestedIconUrl == null) {
            if (StringUtils.hasText(user.getPlayerBadgeIconUrl())) {
                return user.getPlayerBadgeIconUrl().trim();
            }
            return preset.iconUrl();
        }

        String iconUrl = normalizeOptionalText(
                requestedIconUrl,
                PlayerCardDefaults.MAX_BADGE_ICON_URL_LENGTH,
                "Badge icon URL toi da 500 ky tu"
        );
        if (!StringUtils.hasText(iconUrl)) {
            return preset.iconUrl();
        }
        if (iconUrl.startsWith("http://") || iconUrl.startsWith("https://") || iconUrl.startsWith("/")) {
            return iconUrl;
        }
        throw badRequest("Badge icon URL khong hop le");
    }

    private String resolvePlayerTitle(String requestedTitle, User user) {
        if (requestedTitle == null) {
            return user.resolvePlayerTitle();
        }
        String title = normalizeOptionalText(
                requestedTitle,
                PlayerCardDefaults.MAX_TITLE_LENGTH,
                "Title Player Card toi da 60 ky tu"
        );
        return StringUtils.hasText(title) ? title : PlayerCardDefaults.DEFAULT_TITLE;
    }

    private String normalizeBadgeCode(String value) {
        String badgeCode = PlayerCardDefaults.normalizeBadgeCode(value);
        if (!PlayerCardDefaults.SAFE_BADGE_CODE.matcher(badgeCode).matches()) {
            throw badRequest("Ma badge chi duoc gom chu thuong, so, gach ngang hoac gach duoi");
        }
        return badgeCode;
    }

    private String normalizeOptionalText(String value, int maxLength, String tooLongMessage) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > maxLength) {
            throw badRequest(tooLongMessage);
        }
        return normalized;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
