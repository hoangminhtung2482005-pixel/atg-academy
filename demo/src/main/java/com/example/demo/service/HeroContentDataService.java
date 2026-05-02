package com.example.demo.service;

import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HeroContentDataService {

    private final HeroRepository heroRepository;

    public HeroContentDataService(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Transactional(readOnly = true)
    public Object normalizeForStorage(Object contentData) {
        return transformKnownHeroContent(contentData, loadCatalog(), false);
    }

    @Transactional(readOnly = true)
    public Object enrichForResponse(Object contentData) {
        return transformKnownHeroContent(contentData, loadCatalog(), true);
    }

    private Object transformKnownHeroContent(Object contentData, Catalog catalog, boolean enrich) {
        if (!(contentData instanceof Map<?, ?> map)) {
            return contentData;
        }

        if (map.get("rows") instanceof List<?>) {
            return transformTierListContent(map, catalog, enrich);
        }
        if (map.get("tiers") instanceof List<?>) {
            return transformTierGroupContent(map, catalog, enrich);
        }
        return contentData;
    }

    private Map<String, Object> transformTierListContent(Map<?, ?> source, Catalog catalog, boolean enrich) {
        Map<String, Object> result = copyMap(source);
        Object rowsValue = source.get("rows");
        if (rowsValue instanceof List<?> rows) {
            result.put("rows", rows.stream()
                    .map(row -> transformTierListRow(row, catalog, enrich))
                    .toList());
        }
        return result;
    }

    private Object transformTierListRow(Object rowValue, Catalog catalog, boolean enrich) {
        if (!(rowValue instanceof Map<?, ?> row)) {
            return rowValue;
        }

        Map<String, Object> result = copyMap(row);
        Object cellsValue = row.get("cells");
        if (cellsValue instanceof List<?> cells) {
            result.put("cells", cells.stream()
                    .map(cell -> transformHeroList(cell, catalog, enrich))
                    .toList());
        }
        return result;
    }

    private Map<String, Object> transformTierGroupContent(Map<?, ?> source, Catalog catalog, boolean enrich) {
        Map<String, Object> result = copyMap(source);
        Object tiersValue = source.get("tiers");
        if (tiersValue instanceof List<?> tiers) {
            result.put("tiers", tiers.stream()
                    .map(tier -> transformTierGroup(tier, catalog, enrich))
                    .toList());
        }
        return result;
    }

    private Object transformTierGroup(Object tierValue, Catalog catalog, boolean enrich) {
        if (!(tierValue instanceof Map<?, ?> tier)) {
            return tierValue;
        }

        Map<String, Object> result = copyMap(tier);
        result.put("heroes", transformHeroList(tier.get("heroes"), catalog, enrich));
        return result;
    }

    private List<Object> transformHeroList(Object value, Catalog catalog, boolean enrich) {
        if (!(value instanceof List<?> heroes)) {
            return List.of();
        }
        return heroes.stream()
                .map(heroValue -> enrich ? toHeroResponse(heroValue, catalog) : toHeroStorageRef(heroValue, catalog))
                .filter(Objects::nonNull)
                .toList();
    }

    private Object toHeroStorageRef(Object value, Catalog catalog) {
        Optional<Hero> hero = resolveHero(value, catalog);
        if (hero.isPresent()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("heroId", hero.get().getId());
            return result;
        }

        String fallbackName = readHeroName(value);
        if (StringUtils.hasText(fallbackName)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", fallbackName.trim());
            return result;
        }
        return null;
    }

    private Object toHeroResponse(Object value, Catalog catalog) {
        Optional<Hero> hero = resolveHero(value, catalog);
        if (hero.isPresent()) {
            return HeroSummaryDto.from(hero.get());
        }

        Map<String, Object> fallback = new LinkedHashMap<>();
        Long heroId = readHeroId(value);
        if (heroId != null) {
            fallback.put("heroId", heroId);
        }
        String fallbackName = readHeroName(value);
        if (StringUtils.hasText(fallbackName)) {
            fallback.put("name", fallbackName.trim());
        }
        return fallback.isEmpty() ? null : fallback;
    }

    private Optional<Hero> resolveHero(Object value, Catalog catalog) {
        Long heroId = readHeroId(value);
        if (heroId != null) {
            Hero hero = catalog.byId().get(heroId);
            if (hero != null) {
                return Optional.of(hero);
            }
        }

        String name = readHeroName(value);
        if (StringUtils.hasText(name)) {
            Hero hero = catalog.byName().get(normalize(name));
            if (hero != null) {
                return Optional.of(hero);
            }
        }

        String slug = readHeroSlug(value);
        if (StringUtils.hasText(slug)) {
            Hero hero = catalog.bySlug().get(SlugUtils.toSlug(slug));
            if (hero != null) {
                return Optional.of(hero);
            }
        }
        return Optional.empty();
    }

    private Long readHeroId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Long.parseLong(text);
        }
        if (value instanceof Map<?, ?> map) {
            Object idValue = firstPresent(map, "heroId", "id");
            if (idValue instanceof Number number) {
                return number.longValue();
            }
            if (idValue instanceof String text && text.matches("\\d+")) {
                return Long.parseLong(text);
            }
        }
        return null;
    }

    private String readHeroName(Object value) {
        if (value instanceof String text && !text.matches("\\d+")) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            Object nameValue = firstPresent(map, "name", "heroName");
            return nameValue != null ? String.valueOf(nameValue) : "";
        }
        return "";
    }

    private String readHeroSlug(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object slugValue = firstPresent(map, "slug", "heroSlug");
            return slugValue != null ? String.valueOf(slugValue) : "";
        }
        return "";
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private Catalog loadCatalog() {
        List<Hero> heroes = heroRepository.findAllWithRolesAndAttributes();
        Map<Long, Hero> byId = heroes.stream()
                .filter(hero -> hero.getId() != null)
                .collect(Collectors.toMap(Hero::getId, hero -> hero, (first, second) -> first, LinkedHashMap::new));
        Map<String, Hero> byName = heroes.stream()
                .filter(hero -> StringUtils.hasText(hero.getName()))
                .collect(Collectors.toMap(hero -> normalize(hero.getName()), hero -> hero, (first, second) -> first, LinkedHashMap::new));
        Map<String, Hero> bySlug = heroes.stream()
                .filter(hero -> StringUtils.hasText(hero.getSlug()))
                .collect(Collectors.toMap(hero -> SlugUtils.toSlug(hero.getSlug()), hero -> hero, (first, second) -> first, LinkedHashMap::new));
        return new Catalog(byId, byName, bySlug);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record Catalog(Map<Long, Hero> byId, Map<String, Hero> byName, Map<String, Hero> bySlug) {
    }
}
