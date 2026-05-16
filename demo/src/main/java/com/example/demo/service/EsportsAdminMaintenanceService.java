package com.example.demo.service;

import com.example.demo.dto.esports.EsportsResetDataRequest;
import com.example.demo.dto.esports.EsportsResetDataResponse;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

@Service
public class EsportsAdminMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(EsportsAdminMaintenanceService.class);
    private static final String REQUIRED_CONFIRMATION_TEXT = "RESET ESPORTS DATA";
    private static final String PLAYER_STATS_RETAINED_REASON =
            "player_stats thuộc Ban/Pick leaderboard, không derived từ esports_matches nên không bị xóa.";

    private final EsportsGameDraftRepository esportsGameDraftRepository;
    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsDatabaseBackupService esportsDatabaseBackupService;
    private final Environment environment;

    @Value("${app.admin.esports-reset.allow-production:false}")
    private boolean allowProductionReset;

    public EsportsAdminMaintenanceService(EsportsGameDraftRepository esportsGameDraftRepository,
                                          EsportsMatchRepository esportsMatchRepository,
                                          EsportsTeamRepository esportsTeamRepository,
                                          EsportsDatabaseBackupService esportsDatabaseBackupService,
                                          Environment environment) {
        this.esportsGameDraftRepository = esportsGameDraftRepository;
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsDatabaseBackupService = esportsDatabaseBackupService;
        this.environment = environment;
    }

    @Transactional
    public EsportsResetDataResponse resetEsportsData(EsportsResetDataRequest request) {
        validateRequest(request);
        guardAgainstProductionProfile();

        long deletedGameDrafts = esportsGameDraftRepository.count();
        long deletedMatches = esportsMatchRepository.count();
        String backupFile = esportsDatabaseBackupService.backupBeforeResetEsportsData();

        esportsGameDraftRepository.deleteAllInBatch();
        esportsMatchRepository.deleteAllInBatch();
        esportsTeamRepository.resetAllRankingFields();

        long remainingGameDrafts = esportsGameDraftRepository.count();
        long remainingMatches = esportsMatchRepository.count();
        if (remainingGameDrafts != 0 || remainingMatches != 0) {
            throw new IllegalStateException("Reset dữ liệu esports không hoàn tất. Vui lòng kiểm tra lại trạng thái DB.");
        }

        log.info(">> [Admin] Da reset esports data: {} game drafts, {} matches. Team rows duoc giu nguyen va reset ranking ve baseline.",
                deletedGameDrafts,
                deletedMatches);

        return new EsportsResetDataResponse(
                true,
                backupFile,
                deletedGameDrafts,
                deletedMatches,
                0,
                remainingGameDrafts,
                remainingMatches,
                false,
                PLAYER_STATS_RETAINED_REASON
        );
    }

    private void validateRequest(EsportsResetDataRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request reset dữ liệu esports không hợp lệ.");
        }
        if (!REQUIRED_CONFIRMATION_TEXT.equals(request.confirmationText())) {
            throw new IllegalArgumentException("confirmationText phải chính xác là '" + REQUIRED_CONFIRMATION_TEXT + "'.");
        }
        if (!request.backupBeforeReset()) {
            throw new IllegalArgumentException("backupBeforeReset phải = true để reset dữ liệu esports.");
        }
    }

    private void guardAgainstProductionProfile() {
        if (allowProductionReset) {
            return;
        }

        boolean productionProfileActive = Arrays.stream(environment.getActiveProfiles())
                .filter(StringUtils::hasText)
                .map(profile -> profile.trim().toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod")
                        || profile.equals("production")
                        || profile.startsWith("prod-")
                        || profile.endsWith("-prod")
                        || profile.contains("production"));

        if (productionProfileActive) {
            throw new UnsupportedOperationException(
                    "Reset dữ liệu esports bị chặn trên production profile. Bật app.admin.esports-reset.allow-production=true nếu thực sự muốn thực hiện."
            );
        }
    }
}
