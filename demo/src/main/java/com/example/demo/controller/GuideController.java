package com.example.demo.controller;

import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Guide;
import com.example.demo.entity.Hero;
import com.example.demo.entity.User;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.GoogleUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/guides")
public class GuideController {

    private final GuideRepository guideRepository;
    private final HeroRepository heroRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public GuideController(GuideRepository guideRepository,
                           HeroRepository heroRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.guideRepository = guideRepository;
        this.heroRepository = heroRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> getGuides(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long heroId,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String lane,
                                       @RequestParam(required = false) String search,
                                       @RequestParam(defaultValue = "newest") String sort) {
        String normalizedStatus = normalize(status);
        String normalizedCategory = normalize(category);
        String normalizedLane = normalize(lane);
        String normalizedSearch = normalize(search);

        List<Guide> filtered = guideRepository.findAll().stream()
                .filter(guide -> !StringUtils.hasText(normalizedStatus)
                        || normalize(effectiveStatus(guide)).equals(normalizedStatus))
                .filter(guide -> heroId == null
                        || (guide.getHero() != null && Objects.equals(guide.getHero().getId(), heroId)))
                .filter(guide -> !StringUtils.hasText(normalizedCategory)
                        || normalize(effectiveCategory(guide)).equals(normalizedCategory))
                .filter(guide -> !StringUtils.hasText(normalizedLane)
                        || normalize(effectiveLane(guide)).equals(normalizedLane))
                .filter(guide -> !StringUtils.hasText(normalizedSearch) || matchesSearch(guide, normalizedSearch))
                .sorted("views".equalsIgnoreCase(sort)
                        ? Comparator.comparing((Guide guide) -> safeInt(guide.getViewCount())).reversed()
                        : Comparator.comparing((Guide guide) -> safeDate(guide.getPublishedAt(), guide.getCreatedAt())).reversed())
                .toList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Guide guide : filtered) {
            result.add(buildResponse(guide));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGuide(@PathVariable Long id) {
        Guide guide = guideRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay giao an"));

        guide.setViewCount(safeInt(guide.getViewCount()) + 1);
        Guide saved = guideRepository.save(guide);
        return ResponseEntity.ok(buildResponse(saved));
    }

    @PostMapping
    public ResponseEntity<?> createGuide(Authentication authentication,
                                         @RequestBody Map<String, Object> body) throws JacksonException {
        GoogleUserPrincipal currentUser = getCurrentUser(authentication);
        User author = findOrCreateUser(currentUser);

        String title = readString(body, "title");
        if (!StringUtils.hasText(title)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tieu de la bat buoc"));
        }

        Object contentData = body.get("contentData");
        Map<String, Object> contentMap = toStringObjectMap(contentData);

        String heroName = firstText(readString(body, "heroName"), readString(contentMap, "heroName"));
        String lane = firstText(readString(body, "lane"), readString(contentMap, "lane"));
        String category = firstText(readString(body, "category"), readString(contentMap, "category"), "Chiến thuật");
        String excerpt = firstText(readString(body, "excerpt"), readString(contentMap, "excerpt"));
        String coverImageUrl = firstText(readString(body, "coverImageUrl"), readString(contentMap, "coverImageUrl"));
        String status = firstText(readString(body, "status"), "PUBLISHED").trim().toUpperCase(Locale.ROOT);

        Guide guide = new Guide();
        guide.setTitle(title.trim());
        guide.setAuthor(author);
        guide.setStatus(status);
        guide.setCategory(category);
        guide.setLane(lane);
        guide.setCoverImageUrl(coverImageUrl);
        guide.setHero(resolveHero(body, heroName));

        contentMap.putIfAbsent("title", title.trim());
        contentMap.putIfAbsent("authorName", author.resolveDisplayName());
        contentMap.putIfAbsent("heroName", heroName);
        contentMap.putIfAbsent("lane", lane);
        contentMap.putIfAbsent("category", category);
        contentMap.putIfAbsent("coverImageUrl", coverImageUrl);

        String textForExcerpt = buildTextForExcerpt(contentMap);
        guide.setExcerpt(StringUtils.hasText(excerpt) ? excerpt : makeExcerpt(textForExcerpt));
        guide.setReadingTimeMinutes(readInt(body, "readingTimeMinutes", estimateReadingTime(textForExcerpt)));
        guide.setContentData(serializeContentData(contentMap.isEmpty() ? contentData : contentMap));
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            guide.setPublishedAt(LocalDateTime.now());
        }

        Guide saved = guideRepository.save(guide);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse(saved));
    }

    private Hero resolveHero(Map<String, Object> body, String heroName) {
        Object heroIdValue = body.get("heroId");
        if (heroIdValue instanceof Number number) {
            return heroRepository.findById(number.longValue()).orElse(null);
        }
        if (StringUtils.hasText(heroName)) {
            return heroRepository.findFirstByNameIgnoreCase(heroName.trim()).orElse(null);
        }
        return null;
    }

    private boolean matchesSearch(Guide guide, String normalizedSearch) {
        Map<String, Object> content = parseContentDataAsMap(guide.getContentData());
        String haystack = String.join(" ",
                guide.getTitle(),
                guide.getExcerpt(),
                guide.getCategory(),
                guide.getLane(),
                guide.getAuthor() != null ? guide.getAuthor().resolveDisplayName() : "",
                guide.getHero() != null ? guide.getHero().getName() : "",
                buildTextForExcerpt(content)
        );
        return normalize(haystack).contains(normalizedSearch);
    }

    private Map<String, Object> buildResponse(Guide guide) {
        Map<String, Object> contentData = parseContentDataAsMap(guide.getContentData());
        String fallbackHeroName = readString(contentData, "heroName");
        String heroName = guide.getHero() != null ? guide.getHero().getName() : fallbackHeroName;
        String authorName = guide.getAuthor() != null ? guide.getAuthor().resolveDisplayName() : readString(contentData, "authorName");
        String coverImageUrl = firstText(guide.getCoverImageUrl(), readString(contentData, "coverImageUrl"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", guide.getId());
        response.put("title", guide.getTitle());
        response.put("coverImageUrl", coverImageUrl);
        response.put("category", effectiveCategory(guide, contentData));
        response.put("lane", effectiveLane(guide, contentData));
        response.put("excerpt", firstText(guide.getExcerpt(), readString(contentData, "excerpt")));
        response.put("status", effectiveStatus(guide));
        response.put("viewCount", safeInt(guide.getViewCount()));
        response.put("readingTime", safeInt(guide.getReadingTimeMinutes()));
        response.put("readingTimeMinutes", safeInt(guide.getReadingTimeMinutes()));
        response.put("contentData", contentData);
        response.put("createdAt", guide.getCreatedAt());
        response.put("updatedAt", guide.getUpdatedAt());
        response.put("publishedAt", guide.getPublishedAt());
        response.put("heroName", heroName);

        if (guide.getHero() != null) {
            response.put("hero", HeroSummaryDto.from(guide.getHero()));
        } else if (StringUtils.hasText(heroName)) {
            response.put("hero", Map.of("id", "", "name", heroName, "avatarUrl", "", "heroClass", "", "classes", List.of(), "laneRoles", List.of(), "attributes", List.of()));
        }

        response.put("author", Map.of(
                "name", StringUtils.hasText(authorName) ? authorName : "ATG Member",
                "avatar", guide.getAuthor() != null && guide.getAuthor().getAvatarUrl() != null ? guide.getAuthor().getAvatarUrl() : ""
        ));
        return response;
    }

    private GoogleUserPrincipal getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof GoogleUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        return principal;
    }

    private User findOrCreateUser(GoogleUserPrincipal principal) {
        return userRepository.findByEmail(principal.email()).map(existing -> {
            boolean changed = false;
            if (!Objects.equals(existing.getName(), principal.name())) {
                existing.setName(principal.name());
                changed = true;
            }
            if (!Objects.equals(existing.getAvatarUrl(), principal.picture())) {
                existing.setAvatarUrl(principal.picture());
                changed = true;
            }
            String normalizedRole = principal.isAdmin() ? "Admin" : principal.isStaff() ? "Staff" : "User";
            if (!Objects.equals(existing.getRole(), normalizedRole)) {
                existing.setRole(normalizedRole);
                changed = true;
            }
            return changed ? userRepository.save(existing) : existing;
        }).orElseGet(() -> {
            User user = new User();
            user.setEmail(principal.email());
            user.setName(StringUtils.hasText(principal.name()) ? principal.name() : "User");
            user.setAvatarUrl(principal.picture());
            user.setRole(principal.isAdmin() ? "Admin" : principal.isStaff() ? "Staff" : "User");
            return userRepository.save(user);
        });
    }

    private String serializeContentData(Object contentData) throws JacksonException {
        if (contentData == null) {
            return "{}";
        }
        if (contentData instanceof String text) {
            return StringUtils.hasText(text) ? text : "{}";
        }
        return objectMapper.writeValueAsString(contentData);
    }

    private Map<String, Object> parseContentDataAsMap(String contentData) {
        if (!StringUtils.hasText(contentData)) {
            return new LinkedHashMap<>();
        }
        try {
            Object parsed = objectMapper.readValue(contentData, Object.class);
            if (parsed instanceof Map<?, ?> parsedMap) {
                return toStringObjectMap(parsedMap);
            }
        } catch (JacksonException ignored) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>();
    }

    private Map<String, Object> toStringObjectMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        }
        return result;
    }

    private String buildTextForExcerpt(Map<String, Object> contentData) {
        List<String> parts = new ArrayList<>();
        for (Object value : contentData.values()) {
            collectText(value, parts);
        }
        return String.join(" ", parts).replaceAll("\\s+", " ").trim();
    }

    private String effectiveStatus(Guide guide) {
        return StringUtils.hasText(guide.getStatus()) ? guide.getStatus() : "PUBLISHED";
    }

    private String effectiveCategory(Guide guide) {
        return effectiveCategory(guide, parseContentDataAsMap(guide.getContentData()));
    }

    private String effectiveCategory(Guide guide, Map<String, Object> contentData) {
        return firstText(guide.getCategory(), readString(contentData, "category"), "Chiến thuật");
    }

    private String effectiveLane(Guide guide) {
        return effectiveLane(guide, parseContentDataAsMap(guide.getContentData()));
    }

    private String effectiveLane(Guide guide, Map<String, Object> contentData) {
        return firstText(guide.getLane(), readString(contentData, "lane"));
    }

    private void collectText(Object value, List<String> parts) {
        if (value == null) return;
        if (value instanceof String text) {
            parts.add(text.replaceAll("<[^>]+>", " "));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(child -> collectText(child, parts));
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(child -> collectText(child, parts));
        }
    }

    private String makeExcerpt(String text) {
        if (!StringUtils.hasText(text)) {
            return "Giáo án cộng đồng ATG với các ghi chú chiến thuật, lựa chọn tướng và cách vận hành trong trận.";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > 180 ? clean.substring(0, 177) + "..." : clean;
    }

    private int estimateReadingTime(String text) {
        if (!StringUtils.hasText(text)) {
            return 5;
        }
        int words = text.trim().split("\\s+").length;
        return Math.max(3, (int) Math.ceil(words / 220.0));
    }

    private int readInt(Map<String, Object> source, String key, int fallback) {
        Object value = source.get(key);
        if (value instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (value instanceof String text) {
            try {
                return Math.max(1, Integer.parseInt(text));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String readString(Map<String, Object> source, String key) {
        Object value = source != null ? source.get(key) : null;
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private LocalDateTime safeDate(LocalDateTime primary, LocalDateTime fallback) {
        if (primary != null) return primary;
        if (fallback != null) return fallback;
        return LocalDateTime.MIN;
    }
}
