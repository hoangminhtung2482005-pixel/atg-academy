package com.example.demo.util;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    public static final int MAX_SLUG_LENGTH = 140;

    private SlugUtils() {
    }

    public static String toSlug(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return truncate(normalized.isBlank() ? "hero" : normalized, MAX_SLUG_LENGTH);
    }

    public static String truncate(String slug, int maxLength) {
        if (!StringUtils.hasText(slug)) {
            return "";
        }

        int safeMaxLength = Math.max(1, maxLength);
        String trimmed = slug.length() <= safeMaxLength ? slug : slug.substring(0, safeMaxLength);
        return trimmed.replaceAll("-+$", "");
    }
}
