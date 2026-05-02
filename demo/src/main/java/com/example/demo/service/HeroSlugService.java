package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
public class HeroSlugService {

    private final HeroRepository heroRepository;

    public HeroSlugService(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    public String generateUniqueSlug(String source) {
        return generateUniqueSlug(source, null);
    }

    public String generateUniqueSlug(String source, Long currentHeroId) {
        String baseSlug = SlugUtils.toSlug(source);
        if (!StringUtils.hasText(baseSlug)) {
            baseSlug = "hero";
        }

        String candidate = SlugUtils.truncate(baseSlug, SlugUtils.MAX_SLUG_LENGTH);
        if (isAvailableForHero(candidate, currentHeroId)) {
            return candidate;
        }

        int suffix = 2;
        while (true) {
            String suffixText = "-" + suffix;
            String truncatedBase = SlugUtils.truncate(baseSlug, SlugUtils.MAX_SLUG_LENGTH - suffixText.length());
            candidate = truncatedBase + suffixText;
            if (isAvailableForHero(candidate, currentHeroId)) {
                return candidate;
            }
            suffix++;
        }
    }

    private boolean isAvailableForHero(String slug, Long currentHeroId) {
        Optional<Hero> existing = heroRepository.findBySlug(slug);
        return existing.isEmpty()
                || (currentHeroId != null && Objects.equals(existing.get().getId(), currentHeroId));
    }
}
