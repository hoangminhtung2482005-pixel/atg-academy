package com.example.demo.dto.admin;

import com.example.demo.dto.wiki.HeroRoleDto;
import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.entity.HeroClass;
import com.example.demo.entity.HeroRole;
import com.example.demo.util.HeroClassCatalog;
import org.springframework.util.StringUtils;

import java.util.List;

public record AdminHeroResponse(
        Long id,
        String name,
        String slug,
        String avatarUrl,
        String portraitUrl,
        String bannerUrl,
        String heroClass,
        List<String> classes,
        String difficulty,
        String description,
        HeroRoleDto primaryRole,
        List<HeroRoleDto> subRoles,
        List<HeroRoleDto> roles,
        List<String> roleCodes,
        List<String> roleNames,
        List<String> attributes
) {
    public static AdminHeroResponse from(Hero hero) {
        List<HeroRole> laneRoles = HeroSummaryDto.orderedLaneRoles(hero);

        List<HeroRoleDto> roleDtos = laneRoles.stream()
                .map(HeroRoleDto::from)
                .toList();

        List<String> roleCodes = laneRoles.stream()
                .map(HeroRole::getCode)
                .toList();

        List<String> roleNames = laneRoles.stream()
                .map(HeroRole::getName)
                .toList();

        List<String> attributes = hero.getAttributes() == null
                ? List.of()
                : hero.getAttributes().stream()
                    .sorted((left, right) -> {
                        int leftOrder = left.getSortOrder() != null ? left.getSortOrder() : Integer.MAX_VALUE;
                        int rightOrder = right.getSortOrder() != null ? right.getSortOrder() : Integer.MAX_VALUE;
                        if (leftOrder != rightOrder) {
                            return Integer.compare(leftOrder, rightOrder);
                        }
                        return left.getName().compareToIgnoreCase(right.getName());
                    })
                    .map(HeroAttribute::getName)
                    .toList();

        List<String> classes = hero.getClasses() == null
                ? List.of()
                : hero.getClasses().stream()
                .map(HeroClass::resolveDisplayName)
                .toList();
        if (classes.isEmpty() && StringUtils.hasText(hero.getHeroClass())) {
            String legacyClass = HeroClassCatalog.canonicalize(hero.getHeroClass());
            classes = legacyClass == null ? List.of() : List.of(legacyClass);
        } else {
            classes = HeroClassCatalog.orderedUnique(classes, hero.getHeroClass());
        }

        String primaryClass = HeroClassCatalog.canonicalize(hero.getHeroClass());
        if (primaryClass == null && !classes.isEmpty()) {
            primaryClass = classes.get(0);
        }

        return new AdminHeroResponse(
                hero.getId(),
                hero.getName(),
                hero.getSlug(),
                hero.getAvatarUrl(),
                hero.getPortraitUrl(),
                hero.getBannerUrl(),
                primaryClass,
                classes,
                hero.getDifficulty(),
                hero.getDescription(),
                HeroSummaryDto.primaryRole(hero),
                HeroSummaryDto.subRoles(hero),
                roleDtos,
                roleCodes,
                roleNames,
                attributes
        );
    }
}
