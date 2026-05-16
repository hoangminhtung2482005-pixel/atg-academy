package com.example.demo.support;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class PlayerCardDefaults {

    public static final String DEFAULT_BADGE_CODE = "default";
    public static final String DEFAULT_BADGE_NAME = "ATG Player";
    public static final String DEFAULT_TITLE = "✦ Tân Binh Ban/Pick ✦";
    public static final int MAX_BADGE_CODE_LENGTH = 40;
    public static final int MAX_BADGE_NAME_LENGTH = 80;
    public static final int MAX_BADGE_ICON_URL_LENGTH = 500;
    public static final int MAX_TITLE_LENGTH = 60;
    public static final Pattern SAFE_BADGE_CODE = Pattern.compile("^[a-z0-9_-]{1,40}$");

    private static final Map<String, PlayerBadgePreset> BADGE_PRESETS = createBadgePresets();

    private PlayerCardDefaults() {
    }

    public static PlayerBadgePreset defaultPreset() {
        return BADGE_PRESETS.get(DEFAULT_BADGE_CODE);
    }

    public static PlayerBadgePreset resolvePreset(String badgeCode) {
        String normalizedCode = normalizeBadgeCode(badgeCode);
        return BADGE_PRESETS.getOrDefault(normalizedCode, defaultPreset());
    }

    public static boolean isSupportedBadgeCode(String badgeCode) {
        String normalizedCode = normalizeBadgeCode(badgeCode);
        return SAFE_BADGE_CODE.matcher(normalizedCode).matches() && BADGE_PRESETS.containsKey(normalizedCode);
    }

    public static List<PlayerBadgePreset> presets() {
        return List.copyOf(BADGE_PRESETS.values());
    }

    public static String normalizeBadgeCode(String badgeCode) {
        if (!StringUtils.hasText(badgeCode)) {
            return DEFAULT_BADGE_CODE;
        }
        return badgeCode.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, PlayerBadgePreset> createBadgePresets() {
        Map<String, PlayerBadgePreset> presets = new LinkedHashMap<>();
        presets.put(DEFAULT_BADGE_CODE, new PlayerBadgePreset(DEFAULT_BADGE_CODE, DEFAULT_BADGE_NAME, null));
        presets.put("draft-scout", new PlayerBadgePreset("draft-scout", "Draft Scout", null));
        presets.put("shot-caller", new PlayerBadgePreset("shot-caller", "Shot Caller", null));
        presets.put("meta-reader", new PlayerBadgePreset("meta-reader", "Meta Reader", null));
        return Map.copyOf(presets);
    }

    public record PlayerBadgePreset(
            String code,
            String name,
            String iconUrl
    ) {
    }
}
