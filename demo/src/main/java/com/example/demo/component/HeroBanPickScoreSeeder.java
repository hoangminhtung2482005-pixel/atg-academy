package com.example.demo.component;

import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class HeroBanPickScoreSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HeroBanPickScoreSeeder.class);
    private static final String SCORE_RESOURCE = "seed/hero-ban-pick-scores.txt";

    private final HeroRepository heroRepository;

    public HeroBanPickScoreSeeder(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Hero> heroes = heroRepository.findAllByOrderByNameAsc();
        if (heroes.isEmpty()) {
            log.warn("[Hero BanPick Score Seeder] Hero database is empty. Skip score backfill.");
            return;
        }

        List<HeroBanPickScoreSeedEntry> seedEntries = loadSeedEntries();
        Map<String, Hero> heroLookup = buildHeroLookup(heroes);
        List<Hero> heroesToUpdate = new ArrayList<>();
        int seededCount = 0;
        int missingCount = 0;

        for (HeroBanPickScoreSeedEntry entry : seedEntries) {
            Hero hero = resolveHero(heroLookup, entry);
            if (hero == null) {
                missingCount += 1;
                log.warn("[Hero BanPick Score Seeder] Hero not found for score seed '{}'. Keys={}", entry.label(), entry.lookupKeys());
                continue;
            }
            if (hero.getBanPickScore() != null) {
                continue;
            }
            hero.setBanPickScore(entry.score());
            heroesToUpdate.add(hero);
            seededCount += 1;
        }

        if (!heroesToUpdate.isEmpty()) {
            heroRepository.saveAll(heroesToUpdate);
        }

        log.info("[Hero BanPick Score Seeder] Seeded {} hero scores from {}. Missing matches: {}.",
                seededCount,
                SCORE_RESOURCE,
                missingCount);
    }

    private List<HeroBanPickScoreSeedEntry> loadSeedEntries() throws Exception {
        ClassPathResource resource = new ClassPathResource(SCORE_RESOURCE);
        List<HeroBanPickScoreSeedEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber += 1;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 2) {
                    throw new IllegalStateException("Invalid hero ban/pick score seed at line " + lineNumber + ": " + trimmed);
                }

                String label = parts[0].trim();
                BigDecimal score = new BigDecimal(parts[1].trim());
                List<String> lookupKeys = new ArrayList<>();
                lookupKeys.add(label);
                if (parts.length >= 3 && StringUtils.hasText(parts[2])) {
                    Arrays.stream(parts[2].split(","))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .forEach(lookupKeys::add);
                }

                entries.add(new HeroBanPickScoreSeedEntry(label, score, lookupKeys));
            }
        }

        return entries;
    }

    private Map<String, Hero> buildHeroLookup(List<Hero> heroes) {
        Map<String, Hero> lookup = new LinkedHashMap<>();
        for (Hero hero : heroes) {
            registerLookupKey(lookup, hero.getName(), hero);
            registerLookupKey(lookup, hero.getSlug(), hero);
        }
        return lookup;
    }

    private void registerLookupKey(Map<String, Hero> lookup, String rawValue, Hero hero) {
        String key = normalizeLookupKey(rawValue);
        if (!StringUtils.hasText(key)) {
            return;
        }
        lookup.putIfAbsent(key, hero);
    }

    private Hero resolveHero(Map<String, Hero> heroLookup, HeroBanPickScoreSeedEntry entry) {
        for (String lookupKey : entry.lookupKeys()) {
            Hero hero = heroLookup.get(normalizeLookupKey(lookupKey));
            if (hero != null) {
                return hero;
            }
        }
        return null;
    }

    private String normalizeLookupKey(String value) {
        return SlugUtils.toSlug(value);
    }

    private record HeroBanPickScoreSeedEntry(String label, BigDecimal score, List<String> lookupKeys) {
        private HeroBanPickScoreSeedEntry {
            lookupKeys = lookupKeys == null ? List.of() : lookupKeys.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}
