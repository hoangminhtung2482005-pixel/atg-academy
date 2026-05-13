package com.example.demo.service;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EsportsAdminService {

    private static final Logger log = LoggerFactory.getLogger(EsportsAdminService.class);
    private static final Set<String> DEFAULT_TEAM_CODES = Set.of(
            "FS", "BAC", "BRU", "SLX", "eA", "TEN", "HD", "KOG", "GJC",
            "SGP", "FPT", "1S", "BOX", "SPN", "FPL", "TS", "GAM",
            "FW", "HKA", "ONE", "DCG", "BMG", "ANK", "LIT"
    );

    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsTournamentRepository esportsTournamentRepository;
    private final EloCalculationService eloCalculationService;

    public EsportsAdminService(EsportsTeamRepository esportsTeamRepository,
                               EsportsMatchRepository esportsMatchRepository,
                               EsportsTournamentRepository esportsTournamentRepository,
                               EloCalculationService eloCalculationService) {
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsTournamentRepository = esportsTournamentRepository;
        this.eloCalculationService = eloCalculationService;
    }

    public List<EsportsTeam> getAllTeams() {
        return esportsTeamRepository.findAll();
    }

    public EsportsTeam getTeamById(Long id) {
        return esportsTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội tuyển với ID: " + id));
    }

    @Transactional
    public EsportsTeam addNewTeam(EsportsTeam team) {
        if (esportsTeamRepository.findByTeamCode(team.getTeamCode()).isPresent()) {
            throw new RuntimeException("Mã đội '" + team.getTeamCode() + "' đã tồn tại!");
        }

        if (team.getScore() == null) team.setScore(1200.0);
        if (team.getGameWins() == null) team.setGameWins(0);
        if (team.getGameLosses() == null) team.setGameLosses(0);
        if (team.getMatchWins() == null) team.setMatchWins(0);
        if (team.getMatchLosses() == null) team.setMatchLosses(0);

        EsportsTeam saved = esportsTeamRepository.save(team);
        log.info(">> [Admin] Đã thêm đội mới: {} ({})", saved.getTeamCode(), saved.getRegion());
        return saved;
    }

    @Transactional
    public EsportsTeam updateTeam(Long id, EsportsTeam updatedData) {
        EsportsTeam existing = esportsTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội tuyển với ID: " + id));

        if (updatedData.getTeamCode() != null) {
            if (!existing.getTeamCode().equals(updatedData.getTeamCode())
                    && esportsTeamRepository.findByTeamCode(updatedData.getTeamCode()).isPresent()) {
                throw new RuntimeException("Mã đội '" + updatedData.getTeamCode() + "' đã tồn tại!");
            }
            existing.setTeamCode(updatedData.getTeamCode());
        }
        if (updatedData.getTeamName() != null) existing.setTeamName(updatedData.getTeamName());
        if (updatedData.getLogoUrl() != null) existing.setLogoUrl(updatedData.getLogoUrl());
        if (updatedData.getRegion() != null) existing.setRegion(updatedData.getRegion());
        if (updatedData.getScore() != null) existing.setScore(updatedData.getScore());
        if (updatedData.getGameWins() != null) existing.setGameWins(updatedData.getGameWins());
        if (updatedData.getGameLosses() != null) existing.setGameLosses(updatedData.getGameLosses());
        if (updatedData.getMatchWins() != null) existing.setMatchWins(updatedData.getMatchWins());
        if (updatedData.getMatchLosses() != null) existing.setMatchLosses(updatedData.getMatchLosses());

        EsportsTeam saved = esportsTeamRepository.save(existing);
        log.info(">> [Admin] Đã cập nhật đội: {} (ID={})", saved.getTeamCode(), saved.getId());
        return saved;
    }

    @Transactional
    public void deleteTeam(Long id) {
        EsportsTeam existing = esportsTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội tuyển với ID: " + id));

        esportsTeamRepository.delete(existing);
        log.info(">> [Admin] Đã xóa đội: {} (ID={})", existing.getTeamCode(), id);
    }

    public List<EsportsMatch> getAllMatches() {
        return esportsMatchRepository.findAllByOrderByMatchDateDesc();
    }

    public EsportsMatch getMatchById(Long id) {
        return esportsMatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu với ID: " + id));
    }

    @Transactional
    public EsportsMatch addMatch(EsportsMatch match) {
        EsportsTournament tournament = resolveTournament(match.getTournamentId());
        if (tournament != null) {
            match.setTier(resolveMatchTier(tournament));
        } else if (match.getTier() == null) {
            match.setTier("1");
        }
        if (match.getStage() == null) match.setStage("bang");
        if (match.getMatchDate() == null) match.setMatchDate(LocalDateTime.now());
        match.setTournament(tournament);

        EsportsMatch saved = esportsMatchRepository.save(match);
        log.info(">> [Admin] Đã thêm trận đấu mới: {} vs {} ({}-{}) | ID={}",
                saved.getTeam1Code(), saved.getTeam2Code(),
                saved.getScore1(), saved.getScore2(), saved.getId());

        eloCalculationService.calculateAllRankings();
        return saved;
    }

    @Transactional
    public EsportsMatch updateMatch(Long id, EsportsMatch updatedData) {
        EsportsMatch existing = esportsMatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu với ID: " + id));

        if (updatedData.getTeam1Code() != null) existing.setTeam1Code(updatedData.getTeam1Code());
        if (updatedData.getTeam2Code() != null) existing.setTeam2Code(updatedData.getTeam2Code());
        if (updatedData.getScore1() != null) existing.setScore1(updatedData.getScore1());
        if (updatedData.getScore2() != null) existing.setScore2(updatedData.getScore2());
        if (updatedData.getStage() != null) existing.setStage(updatedData.getStage());
        if (updatedData.getMatchDate() != null) existing.setMatchDate(updatedData.getMatchDate());
        EsportsTournament tournament = resolveTournament(updatedData.getTournamentId());
        if (tournament != null) {
            existing.setTier(resolveMatchTier(tournament));
        } else if (updatedData.getTier() != null) {
            existing.setTier(updatedData.getTier());
        }
        existing.setTournament(tournament);

        EsportsMatch saved = esportsMatchRepository.save(existing);
        log.info(">> [Admin] Đã cập nhật trận đấu ID={}: {} vs {} ({}-{})",
                saved.getId(), saved.getTeam1Code(), saved.getTeam2Code(),
                saved.getScore1(), saved.getScore2());

        eloCalculationService.calculateAllRankings();
        return saved;
    }

    @Transactional
    public void deleteMatch(Long id) {
        EsportsMatch existing = esportsMatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trận đấu với ID: " + id));

        esportsMatchRepository.delete(existing);
        log.info(">> [Admin] Đã xóa trận đấu ID={}: {} vs {}",
                id, existing.getTeam1Code(), existing.getTeam2Code());

        eloCalculationService.calculateAllRankings();
    }

    @Transactional
    public long resetMatchHistory() {
        long deletedCount = esportsMatchRepository.count();
        esportsMatchRepository.deleteAllInBatch();
        pruneTeamsOutside(DEFAULT_TEAM_CODES);
        eloCalculationService.calculateAllRankings();
        log.info(">> [Admin] Đã reset {} trận đấu và tính lại Elo.", deletedCount);
        return deletedCount;
    }

    @Transactional
    public int importMatchesFromText(String rawData) {
        List<EsportsMatch> matchesToSave = parseMatchesFromText(rawData);
        if (matchesToSave.isEmpty()) {
            throw new IllegalArgumentException("Không có trận đấu hợp lệ để import.");
        }

        Set<String> activeTeamCodes = collectActiveTeamCodes(matchesToSave);
        long oldMatchCount = esportsMatchRepository.count();
        esportsMatchRepository.deleteAllInBatch();
        pruneTeamsOutside(activeTeamCodes);
        esportsMatchRepository.saveAll(matchesToSave);
        log.info(">> [Import] Đã reset {} trận cũ và import {} trận mới.",
                oldMatchCount, matchesToSave.size());

        eloCalculationService.calculateAllRankings();
        log.info(">> [Import] Đã tính toán lại điểm Elo cho tất cả đội.");
        return matchesToSave.size();
    }

    private List<EsportsMatch> parseMatchesFromText(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            throw new IllegalArgumentException("Dữ liệu import đang trống.");
        }

        String[] lines = rawData.split("\\r?\\n");
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);
        List<EsportsMatch> matchesToSave = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                log.warn(">> [Import] Bỏ qua dòng {} vì không đủ dữ liệu: '{}'", i + 1, line);
                continue;
            }

            int score1;
            int score2;
            try {
                score1 = Integer.parseInt(parts[2]);
                score2 = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Dòng " + (i + 1) + " có tỷ số không hợp lệ: " + line);
            }

            EsportsMatch match = new EsportsMatch();
            match.setTeam1Code(parts[0]);
            match.setTeam2Code(parts[1]);
            match.setScore1(score1);
            match.setScore2(score2);
            match.setTier(parts.length >= 5 ? parts[4] : "1");
            match.setStage(parts.length >= 6 ? parts[5] : "bang");
            match.setMatchDate(baseTime.plusMinutes(i));
            matchesToSave.add(match);
        }

        log.info(">> [Import] Đã parse {} trận đấu hợp lệ từ raw text.", matchesToSave.size());
        return matchesToSave;
    }

    private Set<String> collectActiveTeamCodes(List<EsportsMatch> matches) {
        Set<String> activeTeamCodes = new LinkedHashSet<>(DEFAULT_TEAM_CODES);
        for (EsportsMatch match : matches) {
            activeTeamCodes.add(match.getTeam1Code());
            activeTeamCodes.add(match.getTeam2Code());
        }
        return activeTeamCodes;
    }

    private void pruneTeamsOutside(Set<String> activeTeamCodes) {
        List<EsportsTeam> staleTeams = esportsTeamRepository.findAll().stream()
                .filter(team -> team.getTeamCode() != null)
                .filter(team -> !activeTeamCodes.contains(team.getTeamCode()))
                .toList();

        if (!staleTeams.isEmpty()) {
            esportsTeamRepository.deleteAllInBatch(staleTeams);
            log.info(">> [Admin] Đã xóa {} team không còn trong dataset hiện tại.", staleTeams.size());
        }
    }
    private EsportsTournament resolveTournament(Long tournamentId) {
        if (tournamentId == null) {
            return null;
        }
        return esportsTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tournament voi ID: " + tournamentId));
    }

    private String resolveMatchTier(EsportsTournament tournament) {
        if (tournament == null) {
            return "1";
        }
        Integer aerTier = tournament.getAerTier();
        if (aerTier != null && aerTier > 0) {
            return String.valueOf(aerTier);
        }
        return "1";
    }
}
