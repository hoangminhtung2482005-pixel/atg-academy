package com.example.demo.dto.wiki;

import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.entity.HeroClass;
import com.example.demo.entity.HeroRole;
import com.example.demo.util.HeroClassCatalog;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record HeroSummaryDto(
        Long id,
        String slug,
        String name,
        String avatarUrl,
        HeroRoleDto primaryRole,
        List<HeroRoleDto> subRoles,
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
                primaryRole(hero),
                subRoles(hero),
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

    public static HeroRoleDto primaryRole(Hero hero) {
        return hero == null ? null : HeroRoleDto.from(hero.getPrimaryRole());
    }

    public static List<HeroRoleDto> subRoles(Hero hero) {
        return subRoleEntities(hero).stream()
                .map(HeroRoleDto::from)
                .toList();
    }

    public static List<HeroRole> orderedLaneRoles(Hero hero) {
        if (hero == null) {
            return List.of();
        }

        List<HeroRole> result = new ArrayList<>();
        HeroRole primaryRole = hero.getPrimaryRole();
        if (primaryRole != null) {
            result.add(primaryRole);
        }
        subRoleEntities(hero).forEach(result::add);
        return result;
    }

    public static List<HeroRole> subRoleEntities(Hero hero) {
        if (hero == null || hero.getRoles() == null) {
            return List.of();
        }
        Long primaryRoleId = hero.getPrimaryRole() != null ? hero.getPrimaryRole().getId() : null;
        return hero.getRoles().stream()
                .filter(role -> role != null)
                .filter(role -> primaryRoleId == null || !Objects.equals(primaryRoleId, role.getId()))
                .sorted(Comparator.comparing(HeroRole::getCode, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static List<String> laneRoles(Hero hero) {
        return orderedLaneRoles(hero).stream()
                .map(HeroRole::getName)
                .toList();
    }

    static List<String> roleCodes(Hero hero) {
        return orderedLaneRoles(hero).stream()
                .map(HeroRole::getCode)
                .toList();
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
