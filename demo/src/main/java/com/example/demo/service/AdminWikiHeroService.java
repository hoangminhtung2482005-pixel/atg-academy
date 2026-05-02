package com.example.demo.service;

import com.example.demo.dto.admin.AdminHeroAttributeResponse;
import com.example.demo.dto.admin.AdminHeroAttributesUpdateRequest;
import com.example.demo.dto.admin.AdminHeroAttributeUpsertRequest;
import com.example.demo.dto.admin.AdminHeroBasicUpdateRequest;
import com.example.demo.dto.admin.AdminHeroDetailResponse;
import com.example.demo.dto.admin.AdminHeroResponse;
import com.example.demo.dto.admin.AdminHeroRoleOption;
import com.example.demo.dto.admin.AdminHeroRolesUpdateRequest;
import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.entity.HeroClass;
import com.example.demo.entity.HeroRole;
import com.example.demo.repository.HeroAttributeRepository;
import com.example.demo.repository.HeroClassRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.HeroRoleRepository;
import com.example.demo.util.HeroClassCatalog;
import com.example.demo.util.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminWikiHeroService {

    private static final Set<String> ALLOWED_ROLE_CODES = Set.of("DSL", "JGL", "MID", "ADL", "SUP");
    private static final List<String> DIFFICULTIES = List.of("Dễ", "Trung bình", "Khó");
    private static final String ATTRIBUTE_IN_USE_MESSAGE = "Không thể xóa đặc điểm đang được sử dụng bởi tướng.";

    private final HeroRepository heroRepository;
    private final HeroRoleRepository heroRoleRepository;
    private final HeroAttributeRepository heroAttributeRepository;
    private final HeroClassRepository heroClassRepository;

    public AdminWikiHeroService(HeroRepository heroRepository,
                                HeroRoleRepository heroRoleRepository,
                                HeroAttributeRepository heroAttributeRepository,
                                HeroClassRepository heroClassRepository) {
        this.heroRepository = heroRepository;
        this.heroRoleRepository = heroRoleRepository;
        this.heroAttributeRepository = heroAttributeRepository;
        this.heroClassRepository = heroClassRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminHeroResponse> listHeroes() {
        return heroRepository.findAllWithRolesAndAttributes().stream()
                .map(AdminHeroResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminHeroDetailResponse getHero(Long id) {
        return detailResponse(findHero(id));
    }

    @Transactional(readOnly = true)
    public List<AdminHeroAttributeResponse> listAttributes() {
        return listAttributeResponses();
    }

    @Transactional
    public AdminHeroDetailResponse updateHeroRoles(Long heroId, AdminHeroRolesUpdateRequest request) {
        Hero hero = findHero(heroId);
        Set<String> requestedCodes = normalizeRoleCodes(request != null ? request.roles() : List.of());
        List<HeroRole> roles = findRoles(requestedCodes);
        hero.getRoles().clear();
        hero.getRoles().addAll(roles);
        return detailResponse(hero);
    }

    @Transactional
    public AdminHeroDetailResponse updateHeroAttributes(Long heroId, AdminHeroAttributesUpdateRequest request) {
        Hero hero = findHero(heroId);
        Set<String> requestedNames = normalizeAttributeNames(request != null ? request.attributes() : List.of());
        List<HeroAttribute> attributes = findAttributes(requestedNames);
        hero.getAttributes().clear();
        hero.getAttributes().addAll(attributes);
        return detailResponse(hero);
    }

    @Transactional
    public AdminHeroDetailResponse updateHeroBasicInfo(Long heroId, AdminHeroBasicUpdateRequest request) {
        if (request == null) {
            throw badRequest("Dữ liệu cập nhật không hợp lệ.");
        }

        Hero hero = findHero(heroId);

        if (request.name() != null) {
            String name = request.name().trim();
            if (!StringUtils.hasText(name)) {
                throw badRequest("Tên tướng không được để trống.");
            }
            hero.setName(name);
        }

        if (request.slug() != null) {
            String slug = SlugUtils.toSlug(request.slug());
            if (!StringUtils.hasText(slug)) {
                throw badRequest("Slug không được để trống.");
            }
            validateUniqueSlug(slug, hero.getId());
            hero.setSlug(slug);
        } else if (!StringUtils.hasText(hero.getSlug()) && StringUtils.hasText(hero.getName())) {
            String slug = SlugUtils.toSlug(hero.getName());
            validateUniqueSlug(slug, hero.getId());
            hero.setSlug(slug);
        }

        if (request.classes() != null || request.heroClass() != null) {
            updateHeroClasses(hero, request);
        }
        if (request.description() != null) {
            hero.setDescription(blankToNull(request.description()));
        }
        if (request.avatarUrl() != null) {
            hero.setAvatarUrl(blankToNull(request.avatarUrl()));
        }
        if (request.portraitUrl() != null) {
            hero.setPortraitUrl(blankToNull(request.portraitUrl()));
        }
        if (request.bannerUrl() != null) {
            hero.setBannerUrl(blankToNull(request.bannerUrl()));
        }
        if (request.difficulty() != null) {
            hero.setDifficulty(blankToNull(request.difficulty()));
        }

        if (HeroSummaryDto.classes(hero).isEmpty()) {
            throw badRequest("Tướng phải có ít nhất một class.");
        }

        return detailResponse(hero);
    }

    @Transactional
    public AdminHeroAttributeResponse createAttribute(AdminHeroAttributeUpsertRequest request) {
        HeroAttribute attribute = new HeroAttribute();
        applyAttributeChanges(attribute, request, null);
        HeroAttribute saved = heroAttributeRepository.save(attribute);
        return AdminHeroAttributeResponse.from(saved, 0);
    }

    @Transactional
    public AdminHeroAttributeResponse updateAttribute(Long id, AdminHeroAttributeUpsertRequest request) {
        HeroAttribute attribute = heroAttributeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc điểm."));
        applyAttributeChanges(attribute, request, id);
        return AdminHeroAttributeResponse.from(attribute, heroAttributeRepository.countHeroUsage(id));
    }

    @Transactional
    public void deleteAttribute(Long id) {
        HeroAttribute attribute = heroAttributeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc điểm."));
        if (heroAttributeRepository.countHeroUsage(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ATTRIBUTE_IN_USE_MESSAGE);
        }
        heroAttributeRepository.delete(attribute);
    }

    private Hero findHero(Long id) {
        return heroRepository.findByIdWithRolesAndAttributes(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tướng."));
    }

    private AdminHeroDetailResponse detailResponse(Hero hero) {
        AdminHeroResponse heroResponse = AdminHeroResponse.from(hero);
        List<String> suggestedRoles = heroResponse.roles().isEmpty()
                ? HeroClassCatalog.suggestedRoles(heroResponse.classes())
                : List.of();

        return new AdminHeroDetailResponse(
                heroResponse,
                heroRoleRepository.findAllByOrderByCodeAsc().stream()
                        .map(AdminHeroRoleOption::from)
                        .toList(),
                listAttributeResponses(),
                HeroClassCatalog.DEFAULT_CLASS_NAMES,
                DIFFICULTIES,
                suggestedRoles
        );
    }

    private List<AdminHeroAttributeResponse> listAttributeResponses() {
        List<HeroAttribute> attributes = new ArrayList<>(heroAttributeRepository.findAll());
        attributes.sort(Comparator
                .comparing((HeroAttribute attribute) -> attribute.getSortOrder() != null ? attribute.getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(attribute -> attribute.getName() != null ? attribute.getName().toLowerCase(Locale.ROOT) : ""));

        return attributes.stream()
                .map(attribute -> AdminHeroAttributeResponse.from(
                        attribute,
                        attribute.getId() != null ? heroAttributeRepository.countHeroUsage(attribute.getId()) : 0
                ))
                .toList();
    }

    private void updateHeroClasses(Hero hero, AdminHeroBasicUpdateRequest request) {
        Set<String> requestedNames = normalizeClassNames(resolveRequestedClassNames(request));
        if (requestedNames.isEmpty()) {
            throw badRequest("Tướng phải có ít nhất một class.");
        }

        ensureDefaultHeroClasses();
        List<HeroClass> classes = findHeroClasses(requestedNames);
        hero.getClasses().clear();
        hero.getClasses().addAll(classes);
        hero.setHeroClass(requestedNames.iterator().next());
    }

    private Collection<String> resolveRequestedClassNames(AdminHeroBasicUpdateRequest request) {
        if (request.classes() != null) {
            return request.classes();
        }
        if (StringUtils.hasText(request.heroClass())) {
            return List.of(request.heroClass());
        }
        return List.of();
    }

    private void applyAttributeChanges(HeroAttribute attribute,
                                       AdminHeroAttributeUpsertRequest request,
                                       Long currentId) {
        if (request == null) {
            throw badRequest("Dữ liệu đặc điểm không hợp lệ.");
        }

        String name = StringUtils.hasText(request.name()) ? request.name().trim() : "";
        if (!StringUtils.hasText(name)) {
            throw badRequest("Tên đặc điểm không được để trống.");
        }

        heroAttributeRepository.findByNameIgnoreCase(name)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đặc điểm đã tồn tại.");
                });

        if (request.sortOrder() != null && request.sortOrder() < 0) {
            throw badRequest("Thứ tự đặc điểm không hợp lệ.");
        }

        attribute.setName(name);
        attribute.setDescription(blankToNull(request.description()));
        attribute.setIconUrl(blankToNull(request.iconUrl()));
        attribute.setSortOrder(request.sortOrder());
    }

    private void ensureDefaultHeroClasses() {
        for (String className : HeroClassCatalog.DEFAULT_CLASS_NAMES) {
            if (heroClassRepository.findByNameIgnoreCase(className).isPresent()) {
                continue;
            }
            heroClassRepository.save(new HeroClass(null, className, className));
        }
    }

    private Set<String> normalizeRoleCodes(Collection<String> roles) {
        Set<String> result = new LinkedHashSet<>();
        if (roles == null) {
            return result;
        }
        for (String role : roles) {
            if (!StringUtils.hasText(role)) {
                continue;
            }
            String code = role.trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_ROLE_CODES.contains(code)) {
                throw badRequest("Vai trò đi đường không hợp lệ: " + role);
            }
            result.add(code);
        }
        return result;
    }

    private Set<String> normalizeClassNames(Collection<String> classes) {
        Set<String> result = new LinkedHashSet<>();
        if (classes == null) {
            return result;
        }
        for (String heroClass : classes) {
            if (!StringUtils.hasText(heroClass)) {
                continue;
            }
            String canonical = HeroClassCatalog.canonicalize(heroClass);
            if (canonical == null) {
                throw badRequest("Class không hợp lệ: " + heroClass);
            }
            result.add(canonical);
        }
        return result;
    }

    private Set<String> normalizeAttributeNames(Collection<String> attributes) {
        Set<String> result = new LinkedHashSet<>();
        if (attributes == null) {
            return result;
        }
        for (String attribute : attributes) {
            if (StringUtils.hasText(attribute)) {
                result.add(attribute.trim());
            }
        }
        return result;
    }

    private List<HeroRole> findRoles(Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, HeroRole> rolesByCode = heroRoleRepository.findByCodeIn(roleCodes).stream()
                .collect(Collectors.toMap(HeroRole::getCode, Function.identity()));
        List<String> missing = roleCodes.stream()
                .filter(code -> !rolesByCode.containsKey(code))
                .toList();
        if (!missing.isEmpty()) {
            throw badRequest("Vai trò đi đường không tồn tại: " + String.join(", ", missing));
        }
        return roleCodes.stream().map(rolesByCode::get).toList();
    }

    private List<HeroClass> findHeroClasses(Set<String> classNames) {
        if (classNames.isEmpty()) {
            return List.of();
        }
        Map<String, HeroClass> classesByName = heroClassRepository.findByNameIn(classNames).stream()
                .collect(Collectors.toMap(HeroClass::getName, Function.identity()));
        List<String> missing = classNames.stream()
                .filter(name -> !classesByName.containsKey(name))
                .toList();
        if (!missing.isEmpty()) {
            throw badRequest("Class không tồn tại: " + String.join(", ", missing));
        }
        return classNames.stream().map(classesByName::get).toList();
    }

    private List<HeroAttribute> findAttributes(Set<String> attributeNames) {
        if (attributeNames.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, HeroAttribute> attributesByName = heroAttributeRepository.findByNameIn(attributeNames).stream()
                .collect(Collectors.toMap(HeroAttribute::getName, Function.identity()));
        List<String> missing = attributeNames.stream()
                .filter(name -> !attributesByName.containsKey(name))
                .toList();
        if (!missing.isEmpty()) {
            throw badRequest("Đặc điểm không tồn tại: " + String.join(", ", missing));
        }
        return attributeNames.stream().map(attributesByName::get).toList();
    }

    private void validateUniqueSlug(String slug, Long heroId) {
        heroRepository.findBySlug(slug)
                .filter(existing -> !Objects.equals(existing.getId(), heroId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug đã tồn tại.");
                });
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
