package com.example.demo.service;

import com.example.demo.dto.admin.AdminHeroAttributeResponse;
import com.example.demo.dto.admin.AdminHeroAttributeUpsertRequest;
import com.example.demo.entity.HeroAttribute;
import com.example.demo.repository.HeroAttributeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class HeroAttributeService {

    private final HeroAttributeRepository heroAttributeRepository;

    public HeroAttributeService(HeroAttributeRepository heroAttributeRepository) {
        this.heroAttributeRepository = heroAttributeRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminHeroAttributeResponse> findAll() {
        return heroAttributeRepository.findAllWithUsageCount().stream()
                .map(attribute -> new AdminHeroAttributeResponse(
                        attribute.getId(),
                        attribute.getName(),
                        attribute.getDescription(),
                        attribute.getIconUrl(),
                        attribute.getSortOrder(),
                        attribute.getUsageCount()
                ))
                .toList();
    }

    @Transactional
    public AdminHeroAttributeResponse create(AdminHeroAttributeUpsertRequest request) {
        HeroAttribute attribute = new HeroAttribute();
        applyChanges(attribute, request, null);
        HeroAttribute saved = heroAttributeRepository.save(attribute);
        return AdminHeroAttributeResponse.from(saved, 0);
    }

    @Transactional
    public AdminHeroAttributeResponse update(Long id, AdminHeroAttributeUpsertRequest request) {
        HeroAttribute attribute = findById(id);
        applyChanges(attribute, request, id);
        return AdminHeroAttributeResponse.from(attribute, heroAttributeRepository.countHeroUsage(id));
    }

    @Transactional
    public void delete(Long id) {
        HeroAttribute attribute = findById(id);
        heroAttributeRepository.deleteMappingsByAttributeId(id);
        heroAttributeRepository.delete(attribute);
    }

    private HeroAttribute findById(Long id) {
        return heroAttributeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc điểm."));
    }

    private void applyChanges(HeroAttribute attribute, AdminHeroAttributeUpsertRequest request, Long currentId) {
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
        if (request.description() != null) {
            attribute.setDescription(blankToNull(request.description()));
        }
        if (request.iconUrl() != null) {
            attribute.setIconUrl(blankToNull(request.iconUrl()));
        }
        if (request.sortOrder() != null) {
            attribute.setSortOrder(request.sortOrder());
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
