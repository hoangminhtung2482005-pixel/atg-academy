package com.example.demo.dto.wiki;

import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.entity.HeroClass;
import com.example.demo.entity.HeroRole;
import com.example.demo.util.HeroClassCatalog;
import org.springframework.util.StringUtils;

import java.util.List;

public record HeroSummaryDto(
        Long id,
        String slug,
        String name,
        String avatarUrl,
        String heroClass,
        List<String> classes,
        List<String> laneRoles,
        List<String> roles,
        List<String> attributes
) {
    public static HeroSummaryDto from(Hero hero) {
        if (hero == null) {
            return null;
        }

        return new HeroSummaryDto(
                hero.getId(),
                hero.getSlug(),
                hero.getName(),
                hero.getAvatarUrl(),
                primaryClass(hero),
                classes(hero),
                laneRoles(hero),
                roleCodes(hero),
                attributes(hero)
        );
    }

    public static String primaryClass(Hero hero) {
        List<String> classNames = classes(hero);
        if (StringUtils.hasText(hero.getHeroClass())) {
            String legacyClass = HeroClassCatalog.canonicalize(hero.getHeroClass());
            if (legacyClass != null && classNames.contains(legacyClass)) {
                return legacyClass;
            }
        }
        return classNames.isEmpty() ? "" : classNames.get(0);
    }

    public static List<String> classes(Hero hero) {
        if (hero == null) {
            return List.of();
        }

        List<String> mappedClasses = hero.getClasses() != null
                ? hero.getClasses().stream()
                .map(HeroClass::resolveDisplayName)
                .toList()
                : List.of();

        if (!mappedClasses.isEmpty()) {
            return HeroClassCatalog.orderedUnique(mappedClasses, hero.getHeroClass());
        }

        String legacyClass = HeroClassCatalog.canonicalize(hero.getHeroClass());
        return legacyClass == null ? List.of() : List.of(legacyClass);
    }

    static List<String> laneRoles(Hero hero) {
        return hero.getRoles() != null
                ? hero.getRoles().stream()
                    .map(HeroRole::getName)
                    .sorted()
                    .toList()
                : List.of();
    }

    static List<String> roleCodes(Hero hero) {
        return hero.getRoles() != null
                ? hero.getRoles().stream()
                    .map(HeroRole::getCode)
                    .sorted()
                    .toList()
                : List.of();
    }

    static List<String> attributes(Hero hero) {
        return hero.getAttributes() != null
                ? hero.getAttributes().stream()
                    .sorted((left, right) -> {
                        int leftOrder = left.getSortOrder() != null ? left.getSortOrder() : Integer.MAX_VALUE;
                        int rightOrder = right.getSortOrder() != null ? right.getSortOrder() : Integer.MAX_VALUE;
                        if (leftOrder != rightOrder) {
                            return Integer.compare(leftOrder, rightOrder);
                        }
                        return left.getName().compareToIgnoreCase(right.getName());
                    })
                    .map(HeroAttribute::getName)
                    .toList()
                : List.of();
    }
}
