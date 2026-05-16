package com.example.demo.service;

import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.util.EsportsTierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tính điểm Elo cho các đội thi đấu Esports.
 * Dịch 1:1 từ tinhtoan.py (AER 2026) — bao gồm:
 * - Dynamic Tax (Thuế Động)
 * - Protected (Bảo hộ)
 * - Shockwave Zero-Sum (Phạt hội đồng)
 * - RDP (Regional Diff Pool)
 * - Champion Point
 */
@Service
public class EloCalculationService {

    private static final Logger log = LoggerFactory.getLogger(EloCalculationService.class);

    private final EsportsTeamRepository teamRepository;
    private final EsportsMatchRepository matchRepository;

    // ==================== CẤU HÌNH DÙNG CHUNG ====================

    /** Hệ số Tier: 0 = quốc tế, 1 = khu vực, 2 = nhỏ */
    private static final Map<String, Double> TIER_CONF = Map.of(
            "0", 1.5,
            "1", 1.0,
            "2", 0.5
    );

    /** Hệ số giai đoạn: ck = chung kết, playoff, bang = vòng bảng, vongloai */
    private static final Map<String, Double> STAGE_CONF = Map.of(
            "ck", 1.4,
            "playoff", 1.0,
            "bang", 0.7,
            "vongloai", 0.5
    );

    private static final double DEFAULT_TIER = 1.0;
    private static final double DEFAULT_STAGE = 1.0;

    /** Điểm cơ sở mỗi trận */
    private static final double BASE_VAL = 20.0;
    /** Hệ số thuế */
    private static final double X_VAL = 1.0;
    /** Mẫu thuế — Y = 12 (nới lỏng thuế để cân bằng) */
    private static final double Y_VAL = 12.0;
    /** Điểm bảo hộ tối thiểu (Tier != 2) */
    private static final double MIN_PROTECTED = 5.0;

    /** RDP (Regional Diff Pool) — chỉ Tier 0 */
    private static final double RDP_BASE = 30.0;
    private static final double RDP_DENOMINATOR = 1200.0;

    /** Champion Point */
    private static final double CP_GLOBAL = 75.0;
    private static final double CP_LOCAL = 50.0;

    /** Tỉ lệ phạt Shockwave */
    private static final double HARD_PENALTY_RATIO = 0.7;
    private static final double SOFT_PENALTY_RATIO = 0.3;

    /** Điểm khởi đầu */
    private static final double INITIAL_SCORE = 1200.0;

    /** Bản đồ khu vực — giống hệt REGIONS trong Python */
    private static final Map<String, List<String>> REGIONS = Map.of(
            "RPL", List.of("FS", "BAC", "BRU", "SLX", "eA", "TEN", "HD", "KOG", "GJC"),
            "AOG", List.of("SGP", "FPT", "1S", "BOX", "SPN", "FPL", "TS", "GAM"),
            "GCS", List.of("FW", "HKA", "ONE", "DCG", "BMG", "ANK", "LIT")
    );

    // ==============================================================

    public EloCalculationService(EsportsTeamRepository teamRepository,
                                 EsportsMatchRepository matchRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    // ==================== HÀM TIỆN ÍCH ====================

    /** Tra khu vực theo mã đội */
    private String getRegion(String teamCode) {
        for (Map.Entry<String, List<String>> entry : REGIONS.entrySet()) {
            if (entry.getValue().contains(teamCode)) {
                return entry.getKey();
            }
        }
        return "OTHER";
    }

    /** Tính trung bình điểm khu vực (ARG) */
    private double calculateArg(String regionName, Map<String, Double> currentScores) {
        List<Double> scores = currentScores.entrySet().stream()
                .filter(e -> getRegion(e.getKey()).equals(regionName))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (scores.isEmpty()) return INITIAL_SCORE;
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(INITIAL_SCORE);
    }

    /** Tính tổng điểm khu vực */
    private double calculateTotalRegionScore(String regionName, Map<String, Double> currentScores) {
        return currentScores.entrySet().stream()
                .filter(e -> getRegion(e.getKey()).equals(regionName))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    /** Kiểm tra tỷ số có được bảo hộ không: 4-1, 4-0, 3-1, 3-0, 2-0 */
    private boolean isProtected(int wins, int losses) {
        return (wins == 4 && losses <= 1) || (wins == 3 && losses <= 1) || (wins == 2 && losses == 0);
    }

    // ==================== HÀM CHÍNH ====================

    /**
     * Tính toán lại toàn bộ bảng xếp hạng Elo.
     * Mô phỏng y hệt hàm tinhtoan() trong tinhtoan.py.
     */
    @Transactional
    public void calculateAllRankings() {
        log.info("--- ĐANG CHẠY: Y=12 | DYNAMIC TAX | TIER 2 SAFE MODE ---");

        // ====== BƯỚC 1: RESET — Khởi tạo điểm và thống kê ======
        Map<String, Double> teamScores = new LinkedHashMap<>();
        Map<String, int[]> teamStats = new LinkedHashMap<>();
        // int[]: [game_w, game_l, match_w, match_l]

        for (Map.Entry<String, List<String>> entry : REGIONS.entrySet()) {
            for (String team : entry.getValue()) {
                teamScores.put(team, INITIAL_SCORE);
                teamStats.put(team, new int[]{0, 0, 0, 0});
            }
        }

        // ====== BƯỚC 2: XỬ LÝ TRẬN ĐẤU ======
        List<EsportsMatch> matches = matchRepository.findAllByOrderByMatchDateAscIdAsc();
        log.info(">> Tổng số trận đấu: {}", matches.size());

        for (EsportsMatch match : matches) {
            String doi1 = match.getTeam1Code();
            String doi2 = match.getTeam2Code();
            int ts1 = match.getScore1();
            int ts2 = match.getScore2();
            String tierIn = EsportsTierSupport.resolveEffectiveTier(match);
            String stageIn = match.getStage() != null ? match.getStage() : "bang";

            double tierVal = TIER_CONF.getOrDefault(tierIn, DEFAULT_TIER);
            double stageVal = STAGE_CONF.getOrDefault(stageIn, DEFAULT_STAGE);

            // --- Đảm bảo đội tồn tại trong map ---
            teamScores.putIfAbsent(doi1, INITIAL_SCORE);
            teamScores.putIfAbsent(doi2, INITIAL_SCORE);
            teamStats.putIfAbsent(doi1, new int[]{0, 0, 0, 0});
            teamStats.putIfAbsent(doi2, new int[]{0, 0, 0, 0});

            // --- CẬP NHẬT THỐNG KÊ ---
            teamStats.get(doi1)[0] += ts1; // game_w
            teamStats.get(doi1)[1] += ts2; // game_l
            teamStats.get(doi2)[0] += ts2;
            teamStats.get(doi2)[1] += ts1;

            if (ts1 > ts2) {
                teamStats.get(doi1)[2] += 1; // match_w
                teamStats.get(doi2)[3] += 1; // match_l
            } else if (ts2 > ts1) {
                teamStats.get(doi2)[2] += 1;
                teamStats.get(doi1)[3] += 1;
            }

            // --- TÍNH TOÁN ĐIỂM SỐ ---
            double currentBase = BASE_VAL * tierVal * stageVal;
            double sc1 = teamScores.getOrDefault(doi1, INITIAL_SCORE);
            double sc2 = teamScores.getOrDefault(doi2, INITIAL_SCORE);
            int totalGames = (ts1 + ts2) > 0 ? (ts1 + ts2) : 1;
            double sf1 = 1.0 + (double) ts1 / totalGames;
            double sf2 = 1.0 + (double) ts2 / totalGames;

            String winner = "";
            String loser = "";
            double matchChange = 0;
            double scWinner = 0;
            double scLoser = 0;

            if (ts1 > ts2) {
                winner = doi1;
                loser = doi2;
                scWinner = sc1;
                scLoser = sc2;
                double diff = sc1 - sc2;
                double baseEarn = currentBase * sf1;

                // [CẬP NHẬT] THUẾ ĐỘNG (Dynamic Tax)
                double adj = ((diff * X_VAL) / Y_VAL) * tierVal * stageVal;
                matchChange = baseEarn - adj;

                // Bảo hộ: Tier 2 chỉ +1, Tier khác +MIN_PROTECTED
                double minProt = "2".equals(tierIn) ? 1.0 : MIN_PROTECTED;
                if (isProtected(ts1, ts2) && matchChange < minProt) {
                    matchChange = minProt;
                }

            } else if (ts2 > ts1) {
                winner = doi2;
                loser = doi1;
                scWinner = sc2;
                scLoser = sc1;
                double diff = sc2 - sc1;
                double baseEarn = currentBase * sf2;

                // [CẬP NHẬT] THUẾ ĐỘNG
                double adj = ((diff * X_VAL) / Y_VAL) * tierVal * stageVal;
                matchChange = baseEarn - adj;

                double minProt = "2".equals(tierIn) ? 1.0 : MIN_PROTECTED;
                if (isProtected(ts2, ts1) && matchChange < minProt) {
                    matchChange = minProt;
                }
            }

            // ====== [SHOCKWAVE ZERO-SUM] ======
            boolean isShockwave = false;
            // Chỉ kích hoạt nếu Tier KHÁC 2
            if (!winner.isEmpty() && scWinner < scLoser && matchChange > 0 && !"2".equals(tierIn)) {
                isShockwave = true;
            }

            if (!isShockwave) {
                // Tier 2 hoặc Kèo trên thắng -> Elo thường
                if (winner.equals(doi1)) {
                    teamScores.merge(doi1, matchChange, Double::sum);
                    teamScores.merge(doi2, -matchChange, Double::sum);
                } else if (winner.equals(doi2)) {
                    teamScores.merge(doi2, matchChange, Double::sum);
                    teamScores.merge(doi1, -matchChange, Double::sum);
                }
            } else {
                // Tier 0 & 1 Kèo dưới thắng -> Zero-Sum (Phạt hội đồng)
                teamScores.merge(winner, matchChange, Double::sum);

                List<Map.Entry<String, Double>> victims = new ArrayList<>();
                double sumDistance = 0;

                for (Map.Entry<String, Double> entry : teamScores.entrySet()) {
                    String t = entry.getKey();
                    double s = entry.getValue();
                    if (!t.equals(winner) && s > scWinner && s <= scLoser) {
                        boolean isVictim = false;
                        if ("0".equals(tierIn)) {
                            isVictim = true;
                        } else {
                            if (getRegion(t).equals(getRegion(loser))) {
                                isVictim = true;
                            }
                        }
                        if (isVictim) {
                            double dist = s - scWinner;
                            if (dist > 0) {
                                victims.add(Map.entry(t, dist));
                                sumDistance += dist;
                            }
                        }
                    }
                }

                if (!victims.isEmpty() && sumDistance > 0) {
                    double hardPool = matchChange * HARD_PENALTY_RATIO;
                    double softPool = matchChange * SOFT_PENALTY_RATIO;
                    teamScores.merge(loser, -hardPool, Double::sum);

                    for (Map.Entry<String, Double> v : victims) {
                        double ratio = v.getValue() / sumDistance;
                        double penalty = softPool * ratio;
                        teamScores.merge(v.getKey(), -penalty, Double::sum);
                    }
                } else {
                    teamScores.merge(loser, -matchChange, Double::sum);
                }
            }

            // ====== [RDP - Regional Diff Pool] (Chỉ Tier 0) ======
            String reg1 = getRegion(doi1);
            String reg2 = getRegion(doi2);
            if ("0".equals(tierIn) && !reg1.equals(reg2)
                    && !"OTHER".equals(reg1) && !"OTHER".equals(reg2)
                    && !winner.isEmpty()) {

                String rw, rl;
                int wg;
                if (winner.equals(doi1)) {
                    rw = reg1; rl = reg2; wg = ts1;
                } else {
                    rw = reg2; rl = reg1; wg = ts2;
                }

                double argW = calculateArg(rw, teamScores);
                double argL = calculateArg(rl, teamScores);
                double kDiff = 1.0 - (argW - argL) / RDP_DENOMINATOR;
                double rdpTotal = Math.max(10.0, RDP_BASE * (1.0 + (double) wg / totalGames) * kDiff);

                double sumW = calculateTotalRegionScore(rw, teamScores);
                double sumL = calculateTotalRegionScore(rl, teamScores);

                // Cần duyệt trên bản sao key set để tránh ConcurrentModification
                for (String t : new ArrayList<>(teamScores.keySet())) {
                    String tr = getRegion(t);
                    if (tr.equals(rw) && sumW != 0) {
                        double bonus = rdpTotal * (teamScores.get(t) / sumW);
                        teamScores.merge(t, bonus, Double::sum);
                    } else if (tr.equals(rl) && sumL != 0) {
                        double penalty = rdpTotal * (teamScores.get(t) / sumL);
                        teamScores.merge(t, -penalty, Double::sum);
                    }
                }
            }

            // ====== [CHAMPION POINT] ======
            if ("ck".equals(stageIn) && !winner.isEmpty()) {
                if ("0".equals(tierIn)) {
                    // Global: phạt tất cả đội khác
                    double cpVal = CP_GLOBAL;
                    final String cpWinner = winner;
                    double totalVictims = teamScores.entrySet().stream()
                            .filter(e -> !e.getKey().equals(cpWinner))
                            .mapToDouble(Map.Entry::getValue)
                            .sum();
                    if (totalVictims > 0) {
                        teamScores.merge(winner, cpVal, Double::sum);
                        for (String t : new ArrayList<>(teamScores.keySet())) {
                            if (!t.equals(winner)) {
                                double deduction = cpVal * (teamScores.get(t) / totalVictims);
                                teamScores.merge(t, -deduction, Double::sum);
                            }
                        }
                    }
                } else if ("1".equals(tierIn)) {
                    // Local: chỉ phạt đội cùng khu vực
                    double cpVal = CP_LOCAL;
                    final String cpWinner = winner;
                    final String winReg = getRegion(cpWinner);
                    double totalVictims = teamScores.entrySet().stream()
                            .filter(e -> getRegion(e.getKey()).equals(winReg) && !e.getKey().equals(cpWinner))
                            .mapToDouble(Map.Entry::getValue)
                            .sum();
                    if (totalVictims > 0) {
                        teamScores.merge(winner, cpVal, Double::sum);
                        for (String t : new ArrayList<>(teamScores.keySet())) {
                            if (getRegion(t).equals(winReg) && !t.equals(winner)) {
                                double deduction = cpVal * (teamScores.get(t) / totalVictims);
                                teamScores.merge(t, -deduction, Double::sum);
                            }
                        }
                    }
                }
            }
        }

        // ====== BƯỚC 3: LƯU KẾT QUẢ VÀO DATABASE ======
        List<EsportsTeam> teamsToSave = new ArrayList<>();

        for (Map.Entry<String, Double> entry : teamScores.entrySet()) {
            String code = entry.getKey();
            double score = entry.getValue();
            int[] stats = teamStats.getOrDefault(code, new int[]{0, 0, 0, 0});

            EsportsTeam team = teamRepository.findByTeamCode(code)
                    .orElseGet(() -> {
                        EsportsTeam newTeam = new EsportsTeam();
                        newTeam.setTeamCode(code);
                        newTeam.setRegion(getRegion(code));
                        return newTeam;
                    });

            team.setScore(score);
            team.setGameWins(stats[0]);
            team.setGameLosses(stats[1]);
            team.setMatchWins(stats[2]);
            team.setMatchLosses(stats[3]);
            team.setRegion(getRegion(code));

            teamsToSave.add(team);
        }

        teamRepository.saveAll(teamsToSave);
        log.info(">> [OK] Đã cập nhật xong điểm số và thống kê cho {} đội.", teamsToSave.size());
    }
}
