package com.example.demo.util;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EsportsTournamentCatalog {

    private static final String DEFAULT_TOURNAMENT_TIER = "1";

    private static final Map<String, String> TOURNAMENT_NAMES = new LinkedHashMap<>();

    static {
        TOURNAMENT_NAMES.put("0", "AER International");
        TOURNAMENT_NAMES.put("1", "AER Pro League");
        TOURNAMENT_NAMES.put("2", "AER Challenger");
    }

    private EsportsTournamentCatalog() {
    }

    public static String resolveTournamentName(String tournamentTier) {
        String normalizedTier = StringUtils.hasText(tournamentTier)
                ? tournamentTier.trim()
                : DEFAULT_TOURNAMENT_TIER;
        return TOURNAMENT_NAMES.getOrDefault(normalizedTier, TOURNAMENT_NAMES.get(DEFAULT_TOURNAMENT_TIER));
    }

    public static String resolveTournamentTier(String tournamentName) {
        if (!StringUtils.hasText(tournamentName)) {
            return null;
        }

        String normalizedName = tournamentName.trim();
        if ("ALL".equalsIgnoreCase(normalizedName)) {
            return null;
        }

        if (TOURNAMENT_NAMES.containsKey(normalizedName)) {
            return normalizedName;
        }

        return TOURNAMENT_NAMES.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(normalizedName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
