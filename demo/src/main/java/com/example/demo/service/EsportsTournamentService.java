package com.example.demo.service;

import com.example.demo.dto.esports.EsportsTournamentRequest;
import com.example.demo.dto.esports.EsportsTournamentResponse;
import com.example.demo.dto.esports.EsportsTournamentTeamRequest;
import com.example.demo.dto.esports.EsportsTournamentTeamResponse;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.entity.EsportsTournamentTeam;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.EsportsTournamentTeamRepository;
import com.example.demo.util.EsportsTierSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EsportsTournamentService {

    private static final int DEFAULT_AER_TIER = 1;

    private static final List<DefaultTournamentSeed> DEFAULT_TOURNAMENTS = List.of(
            new DefaultTournamentSeed("AOG", "AOG Spring 2026", "aog-spring-2026", 2026, "Spring", "T1", 1, "UPCOMING"),
            new DefaultTournamentSeed("RPL", "RPL Summer 2026", "rpl-summer-2026", 2026, "Summer", "T1", 1, "UPCOMING"),
            new DefaultTournamentSeed("GCS", "GCS Spring 2026", "gcs-spring-2026", 2026, "Spring", "T1", 1, "UPCOMING")
    );

    private final EsportsFranchiseService esportsFranchiseService;
    private final EsportsTournamentRepository esportsTournamentRepository;
    private final EsportsTournamentTeamRepository esportsTournamentTeamRepository;
    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsMatchRepository esportsMatchRepository;
    private final EloCalculationService eloCalculationService;

    public EsportsTournamentService(EsportsFranchiseService esportsFranchiseService,
                                    EsportsTournamentRepository esportsTournamentRepository,
                                    EsportsTournamentTeamRepository esportsTournamentTeamRepository,
                                    EsportsTeamRepository esportsTeamRepository,
                                    EsportsMatchRepository esportsMatchRepository,
                                    EloCalculationService eloCalculationService) {
        this.esportsFranchiseService = esportsFranchiseService;
        this.esportsTournamentRepository = esportsTournamentRepository;
        this.esportsTournamentTeamRepository = esportsTournamentTeamRepository;
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.eloCalculationService = eloCalculationService;
    }

    public List<EsportsTournamentResponse> getPublicTournaments(Long franchiseId, String franchiseCode) {
        seedDefaultsIfMissing();
        return mapTournaments(esportsTournamentRepository.findAllForListing(franchiseId, normalizeCodeFilter(franchiseCode))
                .stream()
                .sorted(tournamentComparator())
                .toList());
    }

    public List<EsportsTournamentResponse> getAdminTournaments(Long franchiseId, String franchiseCode) {
        seedDefaultsIfMissing();
        return mapTournaments(esportsTournamentRepository.findAllForListing(franchiseId, normalizeCodeFilter(franchiseCode))
                .stream()
                .sorted(tournamentComparator())
                .toList());
    }

    public EsportsTournamentResponse getTournamentDetail(Long tournamentId) {
        seedDefaultsIfMissing();
        return mapTournaments(List.of(findEntityById(tournamentId))).get(0);
    }

    public List<EsportsTournamentTeamResponse> listTournamentTeams(Long tournamentId) {
        seedDefaultsIfMissing();
        findEntityById(tournamentId);
        return esportsTournamentTeamRepository.findByTournamentId(tournamentId).stream()
                .sorted(tournamentTeamComparator())
                .map(this::toTournamentTeamResponse)
                .toList();
    }

    public Optional<EsportsTournament> findByNameOrSlug(String tournamentName) {
        if (!StringUtils.hasText(tournamentName)) {
            return Optional.empty();
        }
        return esportsTournamentRepository.findByNameIgnoreCase(tournamentName.trim())
                .or(() -> esportsTournamentRepository.findBySlugIgnoreCase(tournamentName.trim()));
    }

    public EsportsTournament findEntityById(Long id) {
        return esportsTournamentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tournament với ID: " + id));
    }

    @Transactional
    public EsportsTournamentResponse createTournament(EsportsTournamentRequest request) {
        String slug = normalizeSlug(request.slug());
        if (esportsTournamentRepository.existsBySlugIgnoreCase(slug)) {
            throw new IllegalArgumentException("slug tournament đã tồn tại.");
        }

        EsportsTournament tournament = new EsportsTournament();
        applyRequest(tournament, request, slug);
        return mapTournaments(List.of(esportsTournamentRepository.save(tournament))).get(0);
    }

    @Transactional
    public EsportsTournamentResponse updateTournament(Long id, EsportsTournamentRequest request) {
        EsportsTournament tournament = findEntityById(id);
        Integer previousAerTier = tournament.getAerTier();
        String slug = normalizeSlug(request.slug());
        if (esportsTournamentRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new IllegalArgumentException("slug tournament đã tồn tại.");
        }

        applyRequest(tournament, request, slug);
        EsportsTournament savedTournament = esportsTournamentRepository.save(tournament);
        if (!Objects.equals(previousAerTier, savedTournament.getAerTier())) {
            esportsMatchRepository.syncTierSnapshotByTournamentId(
                    savedTournament.getId(),
                    EsportsTierSupport.resolveTournamentSnapshotTier(savedTournament)
            );
            eloCalculationService.calculateAllRankings();
        }
        return mapTournaments(List.of(savedTournament)).get(0);
    }

    @Transactional
    public void deleteTournament(Long id) {
        EsportsTournament tournament = findEntityById(id);
        long linkedMatches = esportsMatchRepository.countByTournamentId(id);
        if (linkedMatches > 0) {
            throw new IllegalStateException("Không thể xóa tournament đã được link với esports_matches.");
        }
        esportsTournamentTeamRepository.findByTournamentId(id)
                .forEach(esportsTournamentTeamRepository::delete);
        esportsTournamentRepository.delete(tournament);
    }

    @Transactional
    public EsportsTournamentTeamResponse addTeamToTournament(Long tournamentId, EsportsTournamentTeamRequest request) {
        EsportsTournament tournament = findEntityById(tournamentId);
        Long teamId = request.teamId();
        if (teamId == null) {
            throw new IllegalArgumentException("teamId la bat buoc.");
        }
        if (esportsTournamentTeamRepository.existsByTournamentIdAndTeamId(tournamentId, teamId)) {
            throw new IllegalArgumentException("Team đã tồn tại trong tournament này.");
        }

        EsportsTeam team = esportsTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy esports team với ID: " + teamId));

        EsportsTournamentTeam relation = new EsportsTournamentTeam();
        relation.setTournament(tournament);
        relation.setTeam(team);
        relation.setGroupName(trimToNull(request.groupName()));
        relation.setSeedNumber(request.seedNumber());
        relation.setStatus(StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase(Locale.ROOT) : "ACTIVE");
        relation.setNote(trimToNull(request.note()));
        return toTournamentTeamResponse(esportsTournamentTeamRepository.save(relation));
    }

    @Transactional
    public void removeTeamFromTournament(Long tournamentId, Long teamId) {
        if (!esportsTournamentTeamRepository.existsByTournamentIdAndTeamId(tournamentId, teamId)) {
            throw new IllegalArgumentException("Không tìm thấy mapping team trong tournament.");
        }
        esportsTournamentTeamRepository.deleteByTournamentIdAndTeamId(tournamentId, teamId);
    }

    @Transactional
    public void seedDefaultsIfMissing() {
        esportsFranchiseService.seedDefaultsIfMissing();
        for (DefaultTournamentSeed seed : DEFAULT_TOURNAMENTS) {
            Optional<EsportsTournament> existingTournament = esportsTournamentRepository.findBySlugIgnoreCase(seed.slug());
            if (existingTournament.isPresent()) {
                EsportsTournament tournament = existingTournament.get();
                boolean dirty = false;
                if (tournament.getFranchise() == null) {
                    tournament.setFranchise(esportsFranchiseService.findEntityByCode(seed.franchiseCode()));
                    dirty = true;
                }
                if (!StringUtils.hasText(tournament.getTierLevel())) {
                    tournament.setTierLevel(seed.tierLevel());
                    dirty = true;
                }
                if (!EsportsTierSupport.isValidAerTier(tournament.getAerTier())) {
                    tournament.setAerTier(seed.aerTier());
                    dirty = true;
                }
                if (!StringUtils.hasText(tournament.getStatus())) {
                    tournament.setStatus(seed.status());
                    dirty = true;
                }
                if (dirty) {
                    esportsTournamentRepository.save(tournament);
                }
                continue;
            }
            EsportsTournament tournament = new EsportsTournament();
            tournament.setFranchise(esportsFranchiseService.findEntityByCode(seed.franchiseCode()));
            tournament.setName(seed.name());
            tournament.setSlug(seed.slug());
            tournament.setSeasonYear(seed.seasonYear());
            tournament.setSplitName(seed.splitName());
            tournament.setTierLevel(seed.tierLevel());
            tournament.setAerTier(seed.aerTier());
            tournament.setStatus(seed.status());
            esportsTournamentRepository.save(tournament);
        }
    }

    private List<EsportsTournamentResponse> mapTournaments(List<EsportsTournament> tournaments) {
        return tournaments.stream()
                .map(tournament -> new EsportsTournamentResponse(
                        tournament.getId(),
                        tournament.getFranchise() != null ? tournament.getFranchise().getId() : null,
                        tournament.getFranchise() != null ? tournament.getFranchise().getCode() : null,
                        tournament.getFranchise() != null ? tournament.getFranchise().getName() : null,
                        tournament.getName(),
                        tournament.getSlug(),
                        tournament.getSeasonYear(),
                        tournament.getSplitName(),
                        tournament.getTierLevel(),
                        tournament.getAerTier(),
                        tournament.getStartDate(),
                        tournament.getEndDate(),
                        tournament.getStatus(),
                        tournament.getDescription(),
                        tournament.getLogoUrl(),
                        esportsTournamentTeamRepository.countByTournamentId(tournament.getId()),
                        esportsMatchRepository.countByTournamentId(tournament.getId()),
                        tournament.getCreatedAt(),
                        tournament.getUpdatedAt()
                ))
                .toList();
    }

    private void applyRequest(EsportsTournament tournament, EsportsTournamentRequest request, String normalizedSlug) {
        if (request.franchiseId() == null) {
            throw new IllegalArgumentException("franchiseId la bat buoc.");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("name la bat buoc.");
        }

        tournament.setFranchise(esportsFranchiseService.findEntityById(request.franchiseId()));
        tournament.setName(request.name().trim());
        tournament.setSlug(normalizedSlug);
        tournament.setSeasonYear(request.seasonYear());
        tournament.setSplitName(trimToNull(request.splitName()));
        tournament.setTierLevel(StringUtils.hasText(request.tierLevel())
                ? request.tierLevel().trim().toUpperCase(Locale.ROOT)
                : tournament.getFranchise().getTierLevel());
        tournament.setAerTier(normalizeAerTier(request.aerTier()));
        tournament.setStartDate(request.startDate());
        tournament.setEndDate(request.endDate());
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("endDate không được nhỏ hơn startDate.");
        }
        tournament.setStatus(StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase(Locale.ROOT) : "UPCOMING");
        tournament.setDescription(trimToNull(request.description()));
        tournament.setLogoUrl(trimToNull(request.logoUrl()));
    }

    private EsportsTournamentTeamResponse toTournamentTeamResponse(EsportsTournamentTeam relation) {
        EsportsTeam team = relation.getTeam();
        return new EsportsTournamentTeamResponse(
                relation.getId(),
                relation.getTournament() != null ? relation.getTournament().getId() : null,
                team != null ? team.getId() : null,
                team != null ? team.getTeamCode() : null,
                displayTeamName(team),
                team != null ? team.getLogoUrl() : null,
                relation.getGroupName(),
                relation.getSeedNumber(),
                relation.getStatus(),
                relation.getNote(),
                relation.getCreatedAt(),
                relation.getUpdatedAt()
        );
    }

    private static Comparator<EsportsTournament> tournamentComparator() {
        return Comparator
                .comparing((EsportsTournament tournament) -> tournament.getFranchise() != null && tournament.getFranchise().getDisplayOrder() != null
                        ? tournament.getFranchise().getDisplayOrder()
                        : Integer.MAX_VALUE)
                .thenComparing((EsportsTournament tournament) -> tournament.getSeasonYear() == null ? 0 : tournament.getSeasonYear(), Comparator.reverseOrder())
                .thenComparing((EsportsTournament tournament) -> tournament.getStartDate() == null ? LocalDate.MIN : tournament.getStartDate(), Comparator.reverseOrder())
                .thenComparing(tournament -> String.valueOf(tournament.getName()));
    }

    private static Comparator<EsportsTournamentTeam> tournamentTeamComparator() {
        return Comparator
                .comparing((EsportsTournamentTeam relation) -> relation.getSeedNumber() == null ? Integer.MAX_VALUE : relation.getSeedNumber())
                .thenComparing(relation -> displayTeamName(relation.getTeam()));
    }

    private static String normalizeSlug(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("slug la bat buoc.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCodeFilter(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static int normalizeAerTier(Integer value) {
        int aerTier = value == null ? DEFAULT_AER_TIER : value;
        if (aerTier < 0 || aerTier > 2) {
            throw new IllegalArgumentException("aerTier chi hop le 0, 1 hoac 2.");
        }
        return aerTier;
    }

    private static String displayTeamName(EsportsTeam team) {
        if (team == null) {
            return "";
        }
        return StringUtils.hasText(team.getTeamName()) ? team.getTeamName().trim() : team.getTeamCode();
    }

    private record DefaultTournamentSeed(
            String franchiseCode,
            String name,
            String slug,
            Integer seasonYear,
            String splitName,
            String tierLevel,
            int aerTier,
            String status
    ) {
    }
}
