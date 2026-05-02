package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class HeroSlugBackfillService {

    private final HeroRepository heroRepository;

    public HeroSlugBackfillService(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    /**
     * Manual maintenance utility only. Do not call this from normal application startup.
     */
    @Transactional
    public int backfillMissingSlugs() {
        List<Hero> heroes = heroRepository.findAllByOrderByNameAsc();
        Set<String> usedSlugs = new HashSet<>();
        List<Hero> changedHeroes = new ArrayList<>();

        for (Hero hero : heroes) {
            String normalizedExisting = SlugUtils.toSlug(hero.getSlug());
            String baseSlug = StringUtils.hasText(normalizedExisting)
                    ? normalizedExisting
                    : SlugUtils.toSlug(hero.getName());
            String candidate = uniqueSlug(baseSlug, usedSlugs);

            if (!Objects.equals(hero.getSlug(), candidate)) {
                hero.setSlug(candidate);
                changedHeroes.add(hero);
            }
            usedSlugs.add(candidate);
        }

        if (!changedHeroes.isEmpty()) {
            heroRepository.saveAll(changedHeroes);
        }
        return changedHeroes.size();
    }

    private String uniqueSlug(String baseSlug, Set<String> usedSlugs) {
        String safeBase = StringUtils.hasText(baseSlug)
                ? SlugUtils.truncate(baseSlug, SlugUtils.MAX_SLUG_LENGTH)
                : "hero";
        String candidate = safeBase;

        if (!usedSlugs.contains(candidate)) {
            return candidate;
        }

        candidate = withSuffix(safeBase, 2);
        if (!usedSlugs.contains(candidate)) {
            return candidate;
        }

        int suffix = 3;
        while (usedSlugs.contains(candidate)) {
            candidate = withSuffix(safeBase, suffix);
            suffix++;
        }
        return candidate;
    }

    private String withSuffix(String baseSlug, int suffix) {
        String suffixText = "-" + suffix;
        return SlugUtils.truncate(baseSlug, SlugUtils.MAX_SLUG_LENGTH - suffixText.length()) + suffixText;
    }
}
