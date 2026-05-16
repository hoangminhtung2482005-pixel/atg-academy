package com.example.demo.util;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EsportsStageSupport {

    public static final String DEFAULT_STAGE = "bang";
    private static final Set<String> CANONICAL_STAGES = Set.of("ck", "playoff", "bang", "vongloai");
    private static final String ALLOWED_STAGES_MESSAGE = "Chỉ chấp nhận: ck, playoff, bang, vongloai.";
    private static final Map<String, String> STAGE_ALIAS_TO_CANONICAL = createStageAliasMap();

    private EsportsStageSupport() {
    }

    public static Optional<String> toCanonicalStage(String rawStage) {
        if (!StringUtils.hasText(rawStage)) {
            return Optional.empty();
        }
        String normalizedKey = normalizeStageKey(rawStage);
        if (!StringUtils.hasText(normalizedKey)) {
            return Optional.empty();
        }
        String canonicalStage = STAGE_ALIAS_TO_CANONICAL.get(normalizedKey);
        if (canonicalStage != null) {
            return Optional.of(canonicalStage);
        }
        return CANONICAL_STAGES.contains(normalizedKey)
                ? Optional.of(normalizedKey)
                : Optional.empty();
    }

    public static String requireCanonicalStage(String rawStage) {
        return requireCanonicalStage(rawStage, "Stage");
    }

    public static String requireCanonicalStage(String rawStage, String fieldLabel) {
        return toCanonicalStage(rawStage)
                .orElseThrow(() -> new IllegalArgumentException(buildInvalidStageMessage(rawStage, fieldLabel)));
    }

    public static String normalizeOrDefault(String rawStage) {
        if (!StringUtils.hasText(rawStage)) {
            return DEFAULT_STAGE;
        }
        return requireCanonicalStage(rawStage);
    }

    public static String normalizeStageKey(String rawStage) {
        if (!StringUtils.hasText(rawStage)) {
            return "";
        }
        String normalized = Normalizer.normalize(rawStage.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "");
    }

    public static String buildInvalidStageMessage(String rawStage, String fieldLabel) {
        String displayValue = StringUtils.hasText(rawStage) ? rawStage.trim() : "(blank)";
        return fieldLabel + " không hợp lệ: " + displayValue + ". " + ALLOWED_STAGES_MESSAGE;
    }

    private static Map<String, String> createStageAliasMap() {
        Map<String, String> aliases = new LinkedHashMap<>();
        registerAliases(aliases, "ck", "ck", "final", "finals", "grandfinal", "grandfinals", "chungket", "chung_ket", "chung ket", "chung kết");
        registerAliases(aliases, "playoff", "playoff", "playoffs", "play-off", "po");
        registerAliases(aliases, "bang", "bang", "group", "groups", "groupstage", "vongbang", "vong_bang", "vong bang", "vòng bảng");
        registerAliases(aliases, "vongloai", "vongloai", "qualifier", "qualifiers", "qualifying", "vong_loai", "vong loai", "vòng loại");
        return Map.copyOf(aliases);
    }

    private static void registerAliases(Map<String, String> aliases,
                                        String canonicalStage,
                                        String... rawAliases) {
        for (String rawAlias : rawAliases) {
            String normalizedKey = normalizeStageKey(rawAlias);
            if (StringUtils.hasText(normalizedKey)) {
                aliases.put(normalizedKey, canonicalStage);
            }
        }
    }
}
