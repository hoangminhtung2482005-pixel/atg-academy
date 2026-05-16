package com.example.demo.util;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTournament;
import org.springframework.util.StringUtils;

import java.util.Set;

public final class EsportsTierSupport {

    public static final String DEFAULT_TIER = "1";

    private static final Set<String> VALID_TIER_VALUES = Set.of("0", "1", "2");

    private EsportsTierSupport() {
    }

    public static boolean isValidAerTier(Integer aerTier) {
        return aerTier != null && aerTier >= 0 && aerTier <= 2;
    }

    public static boolean isValidTierValue(String tier) {
        return StringUtils.hasText(tier) && VALID_TIER_VALUES.contains(tier.trim());
    }

    public static String normalizeLegacyTierOrDefault(String tier) {
        return isValidTierValue(tier) ? tier.trim() : DEFAULT_TIER;
    }

    public static String resolveTournamentSnapshotTier(EsportsTournament tournament) {
        return tournament != null && isValidAerTier(tournament.getAerTier())
                ? String.valueOf(tournament.getAerTier())
                : DEFAULT_TIER;
    }

    public static String resolveEffectiveTier(EsportsMatch match) {
        if (match != null && match.getTournament() != null && isValidAerTier(match.getTournament().getAerTier())) {
            return String.valueOf(match.getTournament().getAerTier());
        }
        return match != null ? normalizeLegacyTierOrDefault(match.getTier()) : DEFAULT_TIER;
    }
}
