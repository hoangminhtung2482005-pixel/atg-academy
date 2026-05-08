package com.example.demo.service;

import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroRole;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HeroContentDataService {

    private static final List<String> OFFICIAL_IMPORT_ROLE_ORDER = List.of("DSL", "JGL", "MID", "ADL", "SUP");
    private static final List<String> OFFICIAL_SCORE_TIER_ORDER = List.of("S", "A", "B", "C", "D");
    private static final BigDecimal SCORE_THRESHOLD_S = new BigDecimal("9");
    private static final BigDecimal SCORE_THRESHOLD_A = new BigDecimal("7.5");
    private static final BigDecimal SCORE_THRESHOLD_B = new BigDecimal("5");
    private static final BigDecimal SCORE_THRESHOLD_C = new BigDecimal("2.5");
    private static final Map<String, String> OFFICIAL_SCORE_TIER_COLORS = Map.of(
            "S", "#e74c3c",
            "A", "#9b59b6",
            "B", "#3498db",
            "C", "#2ecc71",
            "D", "#95a5a6"
    );

    private final HeroRepository heroRepository;

    public HeroContentDataService(HeroRepository heroRepository) {
        this.heroRepository = heroRepository;
    }

    @Transactional(readOnly = true)
    public Object normalizeForStorage(Object contentData) {
        return transformKnownHeroContent(contentData, loadCatalog(), false);
    }

    @Transactional(readOnly = true)
    public Object normalizeOfficialImportForStorage(Object contentData) {
        Catalog catalog = loadCatalog();
        Object normalized = transformKnownHeroContent(contentData, catalog, false);
        return rebuildOfficialTierListByPrimaryRole(normalized, catalog);
    }

    @Transactional(readOnly = true)
    public Object enrichForResponse(Object contentData) {
        return transformKnownHeroContent(contentData, loadCatalog(), true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateOfficialTierListFromHeroScores() {
        Map<String, List<List<Map<String, Object>>>> cellsByTier = new LinkedHashMap<>();
        for (String tier : OFFICIAL_SCORE_TIER_ORDER) {
            List<List<Map<String, Object>>> roleCells = new ArrayList<>();
            for (int index = 0; index < OFFICIAL_IMPORT_ROLE_ORDER.size(); index++) {
                roleCells.add(new ArrayList<>());
            }
            cellsByTier.put(tier, roleCells);
        }

        heroRepository.findAllWithRolesAndAttributes().stream()
                .sorted(Comparator
                        .comparing((Hero hero) -> normalizeBanPickScore(hero.getBanPickScore()))
                        .reversed()
                        .thenComparing(hero -> StringUtils.hasText(hero.getName()) ? hero.getName() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(hero -> hero.getId() != null ? hero.getId() : Long.MAX_VALUE))
                .forEach(hero -> {
                    String tier = resolveTierFromBanPickScore(hero.getBanPickScore());
                    int roleIndex = resolveOfficialTierListRoleIndex(hero);
                    if (roleIndex < 0) {
                        return;
                    }
                    cellsByTier.get(tier).get(roleIndex).add(buildHeroStorageRef(hero));
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", buildOfficialImportColumns());
        result.put("rows", OFFICIAL_SCORE_TIER_ORDER.stream()
                .map(tier -> buildOfficialScoreTierRow(tier, cellsByTier.get(tier)))
                .toList());
        result.put("source", "HERO_BAN_PICK_SCORE");
        return result;
    }

    public String resolveTierFromBanPickScore(BigDecimal score) {
        BigDecimal normalizedScore = normalizeBanPickScore(score);
        if (normalizedScore.compareTo(SCORE_THRESHOLD_S) > 0) {
            return "S";
        }
        if (normalizedScore.compareTo(SCORE_THRESHOLD_A) > 0) {
            return "A";
        }
        if (normalizedScore.compareTo(SCORE_THRESHOLD_B) > 0) {
            return "B";
        }
        if (normalizedScore.compareTo(SCORE_THRESHOLD_C) > 0) {
            return "C";
        }
        return "D";
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

    private Map<String, Object> buildHeroStorageRef(Hero hero) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("heroId", hero.getId());
        return result;
    }

    private Map<String, Object> buildOfficialScoreTierRow(String label, List<List<Map<String, Object>>> cells) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("color", OFFICIAL_SCORE_TIER_COLORS.getOrDefault(label, "#95a5a6"));
        row.put("cells", cells != null ? cells : List.of());
        return row;
    }

    private Object rebuildOfficialTierListByPrimaryRole(Object contentData, Catalog catalog) {
        if (!(contentData instanceof Map<?, ?> map) || !(map.get("rows") instanceof List<?> rows)) {
            return contentData;
        }

        Map<String, Object> result = copyMap(map);
        result.put("columns", buildOfficialImportColumns());
        result.put("rows", rows.stream()
                .map(row -> rebuildOfficialTierListRow(row, catalog))
                .toList());
        return result;
    }

    private Object rebuildOfficialTierListRow(Object rowValue, Catalog catalog) {
        if (!(rowValue instanceof Map<?, ?> row)) {
            return rowValue;
        }

        Map<String, Object> result = copyMap(row);
        List<List<Object>> cells = new ArrayList<>();
        OFFICIAL_IMPORT_ROLE_ORDER.forEach(role -> cells.add(new ArrayList<>()));

        Object cellsValue = row.get("cells");
        if (cellsValue instanceof List<?> sourceCells) {
            for (int sourceCellIndex = 0; sourceCellIndex < sourceCells.size(); sourceCellIndex++) {
                Object cellValue = sourceCells.get(sourceCellIndex);
                if (!(cellValue instanceof List<?> heroes)) {
                    continue;
                }

                for (Object heroValue : heroes) {
                    Object storageRef = toHeroStorageRef(heroValue, catalog);
                    if (storageRef == null) {
                        continue;
                    }
                    int targetIndex = resolveOfficialImportRoleIndex(storageRef, catalog);
                    if (targetIndex < 0) {
                        targetIndex = Math.min(sourceCellIndex, OFFICIAL_IMPORT_ROLE_ORDER.size() - 1);
                    }
                    cells.get(targetIndex).add(storageRef);
                }
            }
        }

        result.put("cells", cells);
        return result;
    }

    private int resolveOfficialImportRoleIndex(Object heroValue, Catalog catalog) {
        return resolveHero(heroValue, catalog)
                .map(hero -> normalizeRoleCode(hero.getPrimaryRole() != null ? hero.getPrimaryRole().getCode() : null))
                .filter(StringUtils::hasText)
                .map(OFFICIAL_IMPORT_ROLE_ORDER::indexOf)
                .filter(index -> index >= 0)
                .orElse(-1);
    }

    private int resolveOfficialTierListRoleIndex(Hero hero) {
        String roleCode = resolveOfficialTierListRoleCode(hero);
        return StringUtils.hasText(roleCode) ? OFFICIAL_IMPORT_ROLE_ORDER.indexOf(roleCode) : -1;
    }

    private String resolveOfficialTierListRoleCode(Hero hero) {
        if (hero == null) {
            return "";
        }

        String primaryRoleCode = normalizeRoleCode(hero.getPrimaryRole() != null ? hero.getPrimaryRole().getCode() : null);
        if (StringUtils.hasText(primaryRoleCode)) {
            return primaryRoleCode;
        }

        return HeroSummaryDto.subRoleEntities(hero).stream()
                .map(HeroRole::getCode)
                .map(this::normalizeRoleCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }

    private String normalizeRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return "";
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        return OFFICIAL_IMPORT_ROLE_ORDER.contains(normalized) ? normalized : "";
    }

    private List<Map<String, Object>> buildOfficialImportColumns() {
        return List.of(
                buildOfficialImportColumn("DSL", "/images/ui/top.png"),
                buildOfficialImportColumn("JGL", "/images/ui/jungle.png"),
                buildOfficialImportColumn("MID", "/images/ui/mid.png"),
                buildOfficialImportColumn("ADL", "/images/ui/adc.png"),
                buildOfficialImportColumn("SUP", "/images/ui/support.png")
        );
    }

    private Map<String, Object> buildOfficialImportColumn(String label, String icon) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("label", label);
        column.put("icon", icon);
        column.put("alt", label);
        return column;
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

    private BigDecimal normalizeBanPickScore(BigDecimal score) {
        if (score == null || score.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return score;
    }

    private record Catalog(Map<Long, Hero> byId, Map<String, Hero> byName, Map<String, Hero> bySlug) {
    }
}
