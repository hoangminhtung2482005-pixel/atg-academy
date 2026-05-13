package com.example.demo.service;

import com.example.demo.dto.wiki.EnchantmentDto;
import com.example.demo.util.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EnchantmentService {

    private static final Map<String, String> DEFAULT_BRANCH_NAMES = Map.of(
            "thanh-khoi-nguyen", "Thành khởi nguyên",
            "thap-quang-minh", "Tháp quang minh",
            "vuc-hon-mang", "Vực hỗn mang",
            "rung-nguyen-sinh", "Rừng nguyên sinh"
    );

    private final WikiJsonStorageService wikiJsonStorageService;

    public EnchantmentService(WikiJsonStorageService wikiJsonStorageService) {
        this.wikiJsonStorageService = wikiJsonStorageService;
    }

    public List<EnchantmentDto> getAllEnchantments() {
        return wikiJsonStorageService.readEnchantments().stream()
                .map(this::normalizeAndValidate)
                .toList();
    }

    public EnchantmentDto getEnchantmentBySlug(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        return getAllEnchantments().stream()
                .filter(enchantment -> slug.equals(enchantment.slug()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Phù hiệu"));
    }

    public EnchantmentDto createEnchantment(EnchantmentDto request) {
        List<EnchantmentDto> enchantments = new ArrayList<>(getAllEnchantments());
        EnchantmentDto candidate = normalizeAndValidate(request);
        boolean duplicated = enchantments.stream().anyMatch(existing -> existing.slug().equals(candidate.slug()));
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug Phù hiệu đã tồn tại");
        }

        enchantments.add(candidate);
        wikiJsonStorageService.writeEnchantments(enchantments);
        return candidate;
    }

    public EnchantmentDto updateEnchantment(String rawCurrentSlug, EnchantmentDto request) {
        String currentSlug = SlugUtils.toSlug(rawCurrentSlug);
        List<EnchantmentDto> enchantments = new ArrayList<>(getAllEnchantments());
        int existingIndex = findEnchantmentIndex(enchantments, currentSlug);
        if (existingIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Phù hiệu");
        }

        EnchantmentDto candidate = normalizeAndValidate(request);
        boolean duplicated = enchantments.stream()
                .anyMatch(existing -> existing.slug().equals(candidate.slug()) && !existing.slug().equals(currentSlug));
        if (duplicated) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug Phù hiệu đã tồn tại");
        }

        enchantments.set(existingIndex, candidate);
        wikiJsonStorageService.writeEnchantments(enchantments);
        return candidate;
    }

    public void deleteEnchantment(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        List<EnchantmentDto> enchantments = new ArrayList<>(getAllEnchantments());
        int existingIndex = findEnchantmentIndex(enchantments, slug);
        if (existingIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Phù hiệu");
        }

        enchantments.remove(existingIndex);
        wikiJsonStorageService.writeEnchantments(enchantments);
    }

    private int findEnchantmentIndex(List<EnchantmentDto> enchantments, String slug) {
        for (int index = 0; index < enchantments.size(); index++) {
            if (slug.equals(enchantments.get(index).slug())) {
                return index;
            }
        }
        return -1;
    }

    private EnchantmentDto normalizeAndValidate(EnchantmentDto request) {
        String slug = SlugUtils.toSlug(request == null ? null : request.slug());
        String name = trimToNull(request == null ? null : request.name());
        String branch = SlugUtils.toSlug(request == null ? null : request.branch());
        String branchName = trimToNull(request == null ? null : request.branchName());
        Integer level = request == null ? null : request.level();
        String iconUrl = normalizeAssetUrl(request == null ? null : request.iconUrl());
        String description = trimToNull(request == null ? null : request.description());

        if (!StringUtils.hasText(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug Phù hiệu là bắt buộc");
        }
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên Phù hiệu là bắt buộc");
        }
        if (!StringUtils.hasText(branch)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branch Phù hiệu là bắt buộc");
        }
        if (!StringUtils.hasText(iconUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "iconUrl Phù hiệu là bắt buộc");
        }
        if (level != null && level < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level Phù hiệu phải lớn hơn hoặc bằng 1");
        }

        String resolvedBranchName = StringUtils.hasText(branchName)
                ? branchName
                : DEFAULT_BRANCH_NAMES.getOrDefault(branch, humanizeBranch(branch));
        String branchIconUrl = "/images/enchantments/" + branch + "/" + branch + ".webp";

        return new EnchantmentDto(
                slug,
                name,
                branch,
                resolvedBranchName,
                level,
                iconUrl,
                description,
                branchIconUrl
        );
    }

    private String humanizeBranch(String branch) {
        String[] words = branch.split("-");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!StringUtils.hasText(word)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
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
