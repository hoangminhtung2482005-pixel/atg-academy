package com.example.demo.service;

import com.example.demo.dto.esports.EsportsFranchiseRequest;
import com.example.demo.dto.esports.EsportsFranchiseResponse;
import com.example.demo.entity.EsportsFranchise;
import com.example.demo.repository.EsportsFranchiseRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EsportsFranchiseService {

    private static final List<DefaultFranchiseSeed> DEFAULT_FRANCHISES = List.of(
            new DefaultFranchiseSeed("RPL", "RoV Pro League", "T1", "Thailand", "/images/leagues/RPL_logo.png", 10),
            new DefaultFranchiseSeed("AOG", "Arena Of Glory", "T1", "Vietnam", "/images/leagues/AOG_logo.png", 20),
            new DefaultFranchiseSeed("GCS", "Garena Challenger Series", "T1", "Taiwan/Hong Kong/Macau", "/images/leagues/GCS_logo.png", 30),
            new DefaultFranchiseSeed("APL", "AoV Premier League", "T0", "International", null, 40),
            new DefaultFranchiseSeed("AIC", "AoV International Championship", "T0", "International", null, 50)
    );

    private final EsportsFranchiseRepository esportsFranchiseRepository;
    private final EsportsTournamentRepository esportsTournamentRepository;

    public EsportsFranchiseService(EsportsFranchiseRepository esportsFranchiseRepository,
                                   EsportsTournamentRepository esportsTournamentRepository) {
        this.esportsFranchiseRepository = esportsFranchiseRepository;
        this.esportsTournamentRepository = esportsTournamentRepository;
    }

    public List<EsportsFranchiseResponse> getPublicFranchises() {
        seedDefaultsIfMissing();
        return mapFranchises(esportsFranchiseRepository.findAll().stream()
                .filter(franchise -> Boolean.TRUE.equals(franchise.getActive()))
                .sorted(franchiseComparator())
                .toList());
    }

    public List<EsportsFranchiseResponse> getAdminFranchises() {
        seedDefaultsIfMissing();
        return mapFranchises(esportsFranchiseRepository.findAll().stream()
                .sorted(franchiseComparator())
                .toList());
    }

    public EsportsFranchiseResponse getFranchiseByCode(String code) {
        seedDefaultsIfMissing();
        EsportsFranchise franchise = esportsFranchiseRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy franchise với code: " + code));
        if (!Boolean.TRUE.equals(franchise.getActive())) {
            throw new IllegalArgumentException("Franchise đang không hoạt động.");
        }
        return mapFranchises(List.of(franchise)).get(0);
    }

    public EsportsFranchise findEntityById(Long id) {
        return esportsFranchiseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy franchise với ID: " + id));
    }

    public EsportsFranchise findEntityByCode(String code) {
        return esportsFranchiseRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy franchise với code: " + code));
    }

    @Transactional
    public EsportsFranchiseResponse createFranchise(EsportsFranchiseRequest request) {
        String code = requireCode(request);
        if (esportsFranchiseRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("code franchise đã tồn tại.");
        }

        EsportsFranchise franchise = new EsportsFranchise();
        applyRequest(franchise, request, code);
        return mapFranchises(List.of(esportsFranchiseRepository.save(franchise))).get(0);
    }

    @Transactional
    public EsportsFranchiseResponse updateFranchise(Long id, EsportsFranchiseRequest request) {
        EsportsFranchise franchise = findEntityById(id);
        String code = requireCode(request);
        if (esportsFranchiseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new IllegalArgumentException("code franchise đã tồn tại.");
        }
        applyRequest(franchise, request, code);
        return mapFranchises(List.of(esportsFranchiseRepository.save(franchise))).get(0);
    }

    @Transactional
    public EsportsFranchiseResponse deactivateFranchise(Long id) {
        EsportsFranchise franchise = findEntityById(id);
        franchise.setActive(Boolean.FALSE);
        return mapFranchises(List.of(esportsFranchiseRepository.save(franchise))).get(0);
    }

    @Transactional
    public void seedDefaultsIfMissing() {
        for (DefaultFranchiseSeed seed : DEFAULT_FRANCHISES) {
            esportsFranchiseRepository.findByCodeIgnoreCase(seed.code())
                    .orElseGet(() -> {
                        EsportsFranchise franchise = new EsportsFranchise();
                        franchise.setCode(seed.code());
                        franchise.setName(seed.name());
                        franchise.setTierLevel(seed.tierLevel());
                        franchise.setRegion(seed.region());
                        franchise.setDescription(seed.name());
                        franchise.setLogoUrl(seed.logoUrl());
                        franchise.setDisplayOrder(seed.displayOrder());
                        franchise.setActive(Boolean.TRUE);
                        return esportsFranchiseRepository.save(franchise);
                    });
        }
    }

    private List<EsportsFranchiseResponse> mapFranchises(List<EsportsFranchise> franchises) {
        Map<Long, Long> tournamentCounts = esportsTournamentRepository.findAll().stream()
                .filter(tournament -> tournament.getFranchise() != null && tournament.getFranchise().getId() != null)
                .collect(Collectors.groupingBy(
                        tournament -> tournament.getFranchise().getId(),
                        Collectors.counting()
                ));

        return franchises.stream()
                .map(franchise -> new EsportsFranchiseResponse(
                        franchise.getId(),
                        franchise.getCode(),
                        franchise.getName(),
                        franchise.getTierLevel(),
                        franchise.getRegion(),
                        franchise.getDescription(),
                        franchise.getLogoUrl(),
                        franchise.getDisplayOrder(),
                        franchise.getActive(),
                        tournamentCounts.getOrDefault(franchise.getId(), 0L),
                        franchise.getCreatedAt(),
                        franchise.getUpdatedAt()
                ))
                .toList();
    }

    private void applyRequest(EsportsFranchise franchise, EsportsFranchiseRequest request, String normalizedCode) {
        franchise.setCode(normalizedCode);
        franchise.setName(requireText(request.name(), "name"));
        franchise.setTierLevel(normalizeTierLevel(request.tierLevel()));
        franchise.setRegion(trimToNull(request.region()));
        franchise.setDescription(trimToNull(request.description()));
        franchise.setLogoUrl(trimToNull(request.logoUrl()));
        franchise.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        franchise.setActive(request.active() == null || request.active());
    }

    private String requireCode(EsportsFranchiseRequest request) {
        return normalizeCode(requireText(request.code(), "code"));
    }

    private static Comparator<EsportsFranchise> franchiseComparator() {
        return Comparator
                .comparing((EsportsFranchise franchise) -> Boolean.FALSE.equals(franchise.getActive()))
                .thenComparing(franchise -> franchise.getDisplayOrder() == null ? Integer.MAX_VALUE : franchise.getDisplayOrder())
                .thenComparing(franchise -> safeText(franchise.getCode()));
    }

    private static String normalizeCode(String value) {
        return requireText(value, "code").toUpperCase(Locale.ROOT);
    }

    private static String normalizeTierLevel(String value) {
        return requireText(value, "tierLevel").toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " la bat buoc.");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String safeText(String value) {
        return Objects.toString(value, "");
    }

    private record DefaultFranchiseSeed(
            String code,
            String name,
            String tierLevel,
            String region,
            String logoUrl,
            int displayOrder
    ) {
    }
}
