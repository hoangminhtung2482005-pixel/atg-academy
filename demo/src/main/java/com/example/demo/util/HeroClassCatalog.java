package com.example.demo.util;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class HeroClassCatalog {

    public static final List<String> DEFAULT_CLASS_NAMES = List.of(
            "Đấu sĩ",
            "Sát thủ",
            "Pháp sư",
            "Xạ thủ",
            "Đỡ đòn",
            "Trợ thủ"
    );

    private static final Map<String, String> DEFAULT_ROLE_SUGGESTIONS = Map.of(
            "Đấu sĩ", "DSL",
            "Sát thủ", "JGL",
            "Pháp sư", "MID",
            "Xạ thủ", "ADL",
            "Đỡ đòn", "SUP",
            "Trợ thủ", "SUP"
    );

    private static final Map<String, Integer> ORDER_INDEX = DEFAULT_CLASS_NAMES.stream()
            .collect(Collectors.toMap(name -> normalize(name), DEFAULT_CLASS_NAMES::indexOf));

    private HeroClassCatalog() {
    }

    public static boolean isAllowed(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return ORDER_INDEX.containsKey(normalize(value));
    }

    public static String canonicalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalize(value);
        return DEFAULT_CLASS_NAMES.stream()
                .filter(candidate -> normalize(candidate).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static List<String> orderedUnique(Collection<String> values) {
        return orderedUnique(values, null);
    }

    public static List<String> orderedUnique(Collection<String> values, String preferredFirst) {
        Set<String> result = new LinkedHashSet<>();
        String canonicalPreferred = canonicalize(preferredFirst);
        if (canonicalPreferred != null) {
            result.add(canonicalPreferred);
        }

        if (values != null) {
            values.stream()
                    .map(HeroClassCatalog::canonicalize)
                    .filter(Objects::nonNull)
                    .sorted(heroClassComparator())
                    .forEach(result::add);
        }

        return List.copyOf(result);
    }

    public static List<String> suggestedRoles(Collection<String> classNames) {
        if (classNames == null) {
            return List.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        orderedUnique(classNames).forEach(className -> {
            String role = DEFAULT_ROLE_SUGGESTIONS.get(className);
            if (StringUtils.hasText(role)) {
                roles.add(role);
            }
        });
        return List.copyOf(roles);
    }

    public static Comparator<String> heroClassComparator() {
        return Comparator
                .comparingInt((String value) -> ORDER_INDEX.getOrDefault(normalize(value), Integer.MAX_VALUE))
                .thenComparing(value -> normalize(value), Comparator.nullsLast(String::compareTo));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
