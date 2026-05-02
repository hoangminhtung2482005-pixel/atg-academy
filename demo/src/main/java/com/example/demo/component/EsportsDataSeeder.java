package com.example.demo.component;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.service.EloCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seeder tự động tạo dữ liệu đội tuyển và trận đấu Esports khi database trống.
 * Danh sách đội và trận đấu được lấy y hệt từ dữ liệu AER 2026 gốc.
 */
@Component
public class EsportsDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EsportsDataSeeder.class);

    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsMatchRepository esportsMatchRepository;
    private final EloCalculationService eloCalculationService;

    /** Bản đồ khu vực — giống hệt REGIONS trong Python */
    private static final Map<String, List<String>> REGIONS = Map.of(
            "RPL", List.of("FS", "BAC", "BRU", "SLX", "eA", "TEN", "HD", "KOG", "GJC"),
            "AOG", List.of("SGP", "FPT", "1S", "BOX", "SPN", "FPL", "TS", "GAM"),
            "GCS", List.of("FW", "HKA", "ONE", "DCG", "BMG", "ANK", "LIT")
    );

    private static final double DEFAULT_SCORE = 1200.0;

    public EsportsDataSeeder(EsportsTeamRepository esportsTeamRepository,
                              EsportsMatchRepository esportsMatchRepository,
                              EloCalculationService eloCalculationService) {
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.eloCalculationService = eloCalculationService;
    }

    @Override
    public void run(String... args) {
        seedTeams();
        seedMatches();
    }

    // ==================== SEED ĐỘI TUYỂN ====================

    private void seedTeams() {
        if (esportsTeamRepository.count() > 0) {
            log.info(">> [Esports Seeder] Đã có {} đội trong DB — bỏ qua seed đội.", esportsTeamRepository.count());
            return;
        }

        log.info(">> [Esports Seeder] Database trống — đang khởi tạo danh sách đội...");

        List<EsportsTeam> teams = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : REGIONS.entrySet()) {
            String region = entry.getKey();
            for (String teamCode : entry.getValue()) {
                EsportsTeam team = new EsportsTeam();
                team.setTeamCode(teamCode);
                team.setRegion(region);
                team.setScore(DEFAULT_SCORE);
                team.setGameWins(0);
                team.setGameLosses(0);
                team.setMatchWins(0);
                team.setMatchLosses(0);
                teams.add(team);
            }
        }

        esportsTeamRepository.saveAll(teams);
        log.info(">> [Esports Seeder] Đã tạo thành công {} đội với {} điểm mặc định.",
                teams.size(), (int) DEFAULT_SCORE);
    }

    // ==================== SEED TRẬN ĐẤU ====================

    private void seedMatches() {
        if (esportsMatchRepository.count() > 0) {
            log.info(">> [Esports Seeder] Đã có {} trận đấu trong DB — bỏ qua seed trận.",
                    esportsMatchRepository.count());
            return;
        }

        log.info(">> [Esports Seeder] Đang khởi tạo lịch sử trận đấu...");

        // Dữ liệu 161 trận đấu lịch sử — lấy từ aer2026_tisodoidau.txt
        String rawMatchData = """
                FPT BOX 3 1 1 bang
                1S FPL 3 0 1 bang
                SGP SPN 3 1 1 bang
                GAM TS 3 1 1 bang
                FPT FPL 3 1 1 bang
                1S SGP 3 0 1 bang
                BOX TS 3 0 1 bang
                GAM SPN 3 1 1 bang
                GAM SGP 3 0 1 bang
                1S TS 3 2 1 bang
                FPT SPN 3 0 1 bang
                BOX FPL 3 1 1 bang
                SGP TS 3 2 1 bang
                1S FPT 3 0 1 bang
                FPL SPN 3 0 1 bang
                BOX GAM 3 1 1 bang
                FPT SGP 3 0 1 bang
                TS SPN 3 0 1 bang
                FPL GAM 3 0 1 bang
                BOX 1S 3 1 1 bang
                FPL TS 3 1 1 bang
                GAM FPT 3 1 1 bang
                SGP BOX 3 0 1 bang
                1S SPN 3 0 1 bang
                SGP FPL 3 1 1 bang
                FPT TS 3 0 1 bang
                GAM 1S 3 1 1 bang
                BOX SPN 3 1 1 bang
                SGP GAM 3 0 1 bang
                FPT FPL 3 0 1 bang
                1S TS 3 0 1 bang
                SPN BOX 3 0 1 bang
                FPL 1S 3 1 1 bang
                SGP FPT 3 2 1 bang
                SPN GAM 3 1 1 bang
                BOX TS 3 1 1 bang
                FPT 1S 3 1 1 bang
                GAM TS 3 0 1 bang
                BOX FPL 3 2 1 bang
                SGP SPN 3 1 1 bang
                FPT TS 3 1 1 bang
                BOX GAM 3 1 1 bang
                SGP FPL 3 2 1 bang
                1S SPN 3 0 1 bang
                FPT GAM 3 1 1 bang
                FS GJC 3 0 1 bang
                SLX BAC 3 2 1 bang
                BRU eA 3 0 1 bang
                BAC HD 3 0 1 bang
                SLX BRU 3 2 1 bang
                FS TEN 3 0 1 bang
                KOG GJC 3 1 1 bang
                SLX eA 3 0 1 bang
                FS HD 3 0 1 bang
                TEN KOG 3 1 1 bang
                BRU GJC 3 0 1 bang
                FS eA 3 2 1 bang
                TEN BAC 3 0 1 bang
                eA KOG 3 2 1 bang
                SLX HD 3 0 1 bang
                BRU BAC 3 2 1 bang
                GJC BAC 3 0 1 bang
                TEN HD 3 1 1 bang
                GJC eA 3 2 1 bang
                FS BAC 3 0 1 bang
                SLX TEN 3 1 1 bang
                BRU KOG 3 0 1 bang
                eA HD 3 1 1 bang
                BRU FS 3 1 1 bang
                HD GJC 3 1 1 bang
                SLX KOG 3 0 1 bang
                FS KOG 3 2 1 bang
                TEN GJC 3 0 1 bang
                BRU HD 3 0 1 bang
                GJC SLX 3 2 1 bang
                BAC eA 3 2 1 bang
                BRU TEN 3 2 1 bang
                KOG HD 3 0 1 bang
                TEN eA 3 0 1 bang
                BAC KOG 3 1 1 bang
                FS SLX 3 1 1 bang
                SLX BRU 0 3 1 bang
                BAC HD 3 1 1 bang
                FS TEN 3 0 1 bang
                KOG GJC 3 0 1 bang
                FS eA 3 1 1 bang
                BAC BRU 1 3 1 bang
                TEN HD 3 1 1 bang
                FS GJC 3 0 1 bang
                BAC SLX 3 0 1 bang
                KOG eA 3 0 1 bang
                HD SLX 3 2 1 bang
                KOG TEN 3 2 1 bang
                FS BRU 3 1 1 bang
                GJC HD 3 0 1 bang
                BRU eA 3 1 1 bang
                BAC TEN 3 0 1 bang
                BRU KOG 3 1 1 bang
                GJC BAC 1 3 1 bang
                TEN SLX 1 3 1 bang
                BRU HD 3 1 1 bang
                FS KOG 3 0 1 bang
                GJC TEN 3 2 1 bang
                SLX eA 3 1 1 bang
                FS BAC 3 1 1 bang
                eA GJC 3 1 1 bang
                KOG HD 3 0 1 bang
                TEN eA 3 0 1 bang
                SLX FS 0 3 1 bang
                KOG BAC 3 1 1 bang
                BRU GJC 3 0 1 bang
                eA BAC 3 1 1 bang
                HD FS 0 3 1 bang
                SLX KOG 0 3 1 bang
                BRU TEN 3 2 1 bang
                GJC SLX 2 3 1 bang
                eA HD 3 2 1 bang
                KOG SLX 4 0 1 playoff
                FS BRU 4 1 1 playoff
                BRU KOG 4 0 1 playoff
                BRU FS 4 2 1 ck
                LIT HKA 3 2 1 bang
                ONE BMG 3 1 1 bang
                FW BMG 3 1 1 bang
                ANK DCG 3 2 1 bang
                ANK LIT 3 1 1 bang
                FW HKA 3 2 1 bang
                ONE LIT 3 0 1 bang
                BMG DCG 3 1 1 bang
                HKA BMG 3 2 1 bang
                ANK FW 3 0 1 bang
                ONE DCG 3 1 1 bang
                HKA ANK 3 2 1 bang
                FW LIT 3 1 1 bang
                DCG HKA 3 0 1 bang
                BMG LIT 3 0 1 bang
                FW DCG 3 2 1 bang
                HKA ONE 3 1 1 bang
                ANK BMG 3 2 1 bang
                DCG LIT 3 0 1 bang
                FW ONE 3 2 1 bang
                ONE ANK 3 2 1 bang
                BMG FW 2 3 1 bang
                ANK DCG 1 3 1 bang
                BMG HKA 3 1 1 bang
                DCG LIT 3 0 1 bang
                ONE ANK 3 0 1 bang
                HKA LIT 3 2 1 bang
                FW ANK 3 1 1 bang
                FW HKA 3 1 1 bang
                BMG ONE 3 1 1 bang
                LIT ANK 3 0 1 bang
                DCG HKA 3 2 1 bang
                FW ONE 3 2 1 bang
                ANK HKA 3 0 1 bang
                LIT ONE 3 1 1 bang
                DCG BMG 3 2 1 bang
                FW LIT 3 1 1 bang
                DCG ONE 3 1 1 bang
                BMG LIT 3 1 1 bang
                FW DCG 3 2 1 bang
                """;

        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        String[] lines = rawMatchData.strip().split("\\r?\\n");
        List<EsportsMatch> matchesToSave = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                log.warn(">> [Seeder] Bỏ qua dòng {} — không đủ dữ liệu: '{}'", i + 1, line);
                continue;
            }

            EsportsMatch match = new EsportsMatch();
            match.setTeam1Code(parts[0]);
            match.setTeam2Code(parts[1]);
            match.setScore1(Integer.parseInt(parts[2]));
            match.setScore2(Integer.parseInt(parts[3]));
            match.setTier(parts.length >= 5 ? parts[4] : "1");
            match.setStage(parts.length >= 6 ? parts[5] : "bang");
            match.setMatchDate(baseTime.plusMinutes(i));

            matchesToSave.add(match);
        }

        esportsMatchRepository.saveAll(matchesToSave);
        log.info(">> [Esports Seeder] Đã tạo thành công {} trận đấu lịch sử.", matchesToSave.size());

        // Tính toán Elo từ đầu
        eloCalculationService.calculateAllRankings();
        log.info(">> [Esports Seeder] Đã tính toán xong điểm Elo cho toàn bộ đội.");
    }
}
