package com.example.demo.service;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.repository.GuideRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpellService {

    private final WikiJsonStorageService wikiJsonStorageService;
    private final GuideRepository guideRepository;
    private final ObjectMapper objectMapper;

    public SpellService(WikiJsonStorageService wikiJsonStorageService,
                        GuideRepository guideRepository,
                        ObjectMapper objectMapper) {
        this.wikiJsonStorageService = wikiJsonStorageService;
        this.guideRepository = guideRepository;
        this.objectMapper = objectMapper;
    }

    public List<SpellDto> getAllSpells() {
        return wikiJsonStorageService.readSpells().stream()
                .map(this::normalizeExistingSpell)
                .toList();
    }

    public SpellDto getSpellBySlug(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        return getAllSpells().stream()
                .filter(spell -> slug.equals(spell.slug()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Bổ trợ"));
    }

    public SpellDto createSpell(SpellDto request) {
        List<SpellDto> spells = new ArrayList<>(getAllSpells());
        SpellDto candidate = normalizeAndValidate(request);
        boolean duplicated = spells.stream().anyMatch(existing -> existing.slug().equals(candidate.slug()));
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug Bổ trợ đã tồn tại");
        }

        spells.add(candidate);
        wikiJsonStorageService.writeSpells(spells);
        return candidate;
    }

    public SpellDto updateSpell(String rawCurrentSlug, SpellDto request) {
        String currentSlug = SlugUtils.toSlug(rawCurrentSlug);
        List<SpellDto> spells = new ArrayList<>(getAllSpells());
        int existingIndex = findSpellIndex(spells, currentSlug);
        if (existingIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Bổ trợ");
        }

        SpellDto candidate = normalizeAndValidate(request);
        boolean duplicated = spells.stream()
                .anyMatch(existing -> existing.slug().equals(candidate.slug()) && !existing.slug().equals(currentSlug));
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug Bổ trợ đã tồn tại");
        }

        spells.set(existingIndex, candidate);
        wikiJsonStorageService.writeSpells(spells);
        return candidate;
    }

    public void deleteSpell(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        List<SpellDto> spells = new ArrayList<>(getAllSpells());
        int existingIndex = findSpellIndex(spells, slug);
        if (existingIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Bổ trợ");
        }

        long guideReferenceCount = countGuideSpellReferences(slug);
        if (guideReferenceCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa Bổ trợ này vì còn " + guideReferenceCount + " guide đang tham chiếu"
            );
        }

        spells.remove(existingIndex);
        wikiJsonStorageService.writeSpells(spells);
    }

    private int findSpellIndex(List<SpellDto> spells, String slug) {
        for (int index = 0; index < spells.size(); index++) {
            if (slug.equals(spells.get(index).slug())) {
                return index;
            }
        }
        return -1;
    }

    private SpellDto normalizeExistingSpell(SpellDto spell) {
        return normalizeAndValidate(spell);
    }

    private SpellDto normalizeAndValidate(SpellDto request) {
        String slug = SlugUtils.toSlug(request == null ? null : request.slug());
        String name = trimToNull(request == null ? null : request.name());
        String iconUrl = normalizeAssetUrl(request == null ? null : request.iconUrl());
        String description = trimToNull(request == null ? null : request.description());

        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug Bổ trợ là bắt buộc");
        }
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên Bổ trợ là bắt buộc");
        }
        if (!StringUtils.hasText(iconUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "iconUrl Bổ trợ là bắt buộc");
        }

        return new SpellDto(slug, name, iconUrl, description);
    }

    private long countGuideSpellReferences(String slug) {
        return guideRepository.findAll().stream()
                .filter(guide -> guideReferencesSpell(guide.getContentData(), slug))
                .count();
    }

    private boolean guideReferencesSpell(String contentData, String slug) {
        if (!StringUtils.hasText(contentData)) {
            return false;
        }

        try {
            Object payload = objectMapper.readValue(contentData, Object.class);
            return containsSpellSlug(payload, slug);
        } catch (JacksonException exception) {
            return contentData.contains("\"spellSlug\":\"" + slug + "\"")
                    || contentData.contains("\"spellSlug\": \"" + slug + "\"");
        }
    }

    private boolean containsSpellSlug(Object value, String slug) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object child = entry.getValue();
                if ("spellSlug".equals(key) && slug.equals(SlugUtils.toSlug(String.valueOf(child)))) {
                    return true;
                }
                if (containsSpellSlug(child, slug)) {
                    return true;
                }
            }
            return false;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object child : iterable) {
                if (containsSpellSlug(child, slug)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalizeAssetUrl(String value) {
        String trimmed = trimToNull(value);
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        if (trimmed.startsWith("/") || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:")) {
            return trimmed;
        }
        return "/" + trimmed.replaceFirst("^\\./?", "");
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
