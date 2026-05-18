package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickSeasonResetExecuteResponse;
import com.example.demo.dto.banpick.BanPickSeasonResetPreviewResponse;
import com.example.demo.entity.BanPickRankResetLog;
import com.example.demo.entity.BanPickSeasonResetType;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickRankResetLogRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;

@Service
public class BanPickSeasonResetService {

    static final String SYSTEM_EXECUTOR = "SYSTEM_SCHEDULER";
    private static final int SAMPLE_LIMIT = 5;

    private final PlayerStatsRepository playerStatsRepository;
    private final BanPickRankResetLogRepository resetLogRepository;
    private final BanPickSeasonResetSchedule resetSchedule;
    private final Clock clock;
    private final BanPickRatingSettingsAccessor settingsAccessor;

    @Autowired
    public BanPickSeasonResetService(PlayerStatsRepository playerStatsRepository,
                                     BanPickRankResetLogRepository resetLogRepository,
                                     BanPickSeasonResetSchedule resetSchedule,
                                     BanPickRatingSettingsAccessor settingsAccessor) {
        this(playerStatsRepository, resetLogRepository, resetSchedule, Clock.systemDefaultZone(), settingsAccessor);
    }

    BanPickSeasonResetService(PlayerStatsRepository playerStatsRepository,
                              BanPickRankResetLogRepository resetLogRepository,
                              BanPickSeasonResetSchedule resetSchedule,
                              Clock clock,
                              BanPickRatingSettingsAccessor settingsAccessor) {
        this.playerStatsRepository = playerStatsRepository;
        this.resetLogRepository = resetLogRepository;
        this.resetSchedule = resetSchedule;
        this.clock = clock;
        this.settingsAccessor = settingsAccessor;
    }

    @Transactional(readOnly = true)
    public BanPickSeasonResetPreviewResponse previewReset(BanPickSeasonResetType type) {
        BanPickSeasonResetType executableType = requireExecutableType(type);
        return buildPreview(executableType, playerStatsRepository.findAll(), settingsAccessor.getCurrentSettings());
    }

    @Transactional
    public BanPickSeasonResetExecuteResponse executeReset(BanPickSeasonResetType type,
                                                          String confirmationText,
                                                          String executedBy,
                                                          String note) {
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        if (!settings.resetConfirmationText().equals(confirmationText)) {
            throw new IllegalArgumentException("confirmationText phai chinh xac la '" + settings.resetConfirmationText() + "'.");
        }
        return executeResetInternal(
                requireExecutableType(type),
                LocalDate.now(clock),
                normalizeExecutor(executedBy),
                sanitizeNote(note),
                settings
        );
    }

    @Transactional(readOnly = true)
    public BanPickSeasonResetType determineScheduledResetType(LocalDate date) {
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        return resetSchedule.determineResetType(
                date,
                settings.seasonSoftResetMonths(),
                settings.seasonHardResetMonths(),
                settings.seasonHardPriorityOverSoft()
        );
    }

    @Transactional
    public BanPickSeasonResetExecuteResponse runScheduledResetIfDue(LocalDate date) {
        BanPickRatingSettingsSnapshot settings = settingsAccessor.getCurrentSettings();
        if (!settings.seasonSchedulerEnabled() || date == null) {
            return null;
        }

        BanPickSeasonResetType scheduledType = resetSchedule.determineResetType(
                date,
                settings.seasonSoftResetMonths(),
                settings.seasonHardResetMonths(),
                settings.seasonHardPriorityOverSoft()
        );
        if (!scheduledType.isExecutable() || resetLogRepository.existsByScheduledDate(date)) {
            return null;
        }

        return executeResetInternal(
                scheduledType,
                date,
                SYSTEM_EXECUTOR,
                "Scheduled seasonal reset for " + date,
                settings
        );
    }

    private BanPickSeasonResetExecuteResponse executeResetInternal(BanPickSeasonResetType type,
                                                                   LocalDate scheduledDate,
                                                                   String executedBy,
                                                                   String note,
                                                                   BanPickRatingSettingsSnapshot settings) {
        if (resetLogRepository.existsByScheduledDate(scheduledDate)) {
            throw new IllegalStateException("Solo Ban/Pick reset da chay cho ngay " + scheduledDate + ".");
        }

        List<PlayerStats> statsRows = playerStatsRepository.findAll();
        BanPickSeasonResetPreviewResponse preview = buildPreview(type, statsRows, settings);
        LocalDateTime executedAt = LocalDateTime.now(clock);

        for (PlayerStats stats : statsRows) {
            if (stats == null) {
                continue;
            }
            int newRating = type.applyTo(
                    safeStoredRating(stats.getRating(), settings.seasonBaseRating()),
                    settings.seasonBaseRating(),
                    settings.minRating()
            );
            stats.setRating(newRating);
            stats.setRatingAnchor(newRating);
            stats.setRatingAnchorAt(executedAt);
            stats.setLastResetType(type);
        }
        playerStatsRepository.saveAll(statsRows);

        BanPickRankResetLog log = new BanPickRankResetLog();
        log.setResetType(type);
        log.setScheduledDate(scheduledDate);
        log.setExecutedAt(executedAt);
        log.setAffectedPlayers(Math.toIntExact(preview.affectedPlayerCount()));
        log.setBaseRating(preview.baseRating());
        log.setFormula(preview.formula());
        log.setExecutedBy(executedBy);
        log.setNote(note);
        BanPickRankResetLog savedLog = resetLogRepository.save(log);

        return new BanPickSeasonResetExecuteResponse(
                true,
                savedLog.getId(),
                type,
                scheduledDate,
                executedAt,
                executedBy,
                note,
                preview
        );
    }

    private BanPickSeasonResetPreviewResponse buildPreview(BanPickSeasonResetType type,
                                                           List<PlayerStats> statsRows,
                                                           BanPickRatingSettingsSnapshot settings) {
        List<PlayerStats> safeRows = statsRows != null ? statsRows : List.of();
        List<Integer> beforeRatings = safeRows.stream()
                .map(stats -> safeStoredRating(stats != null ? stats.getRating() : null, settings.seasonBaseRating()))
                .toList();
        List<Integer> afterRatings = beforeRatings.stream()
                .map(rating -> type.applyTo(rating, settings.seasonBaseRating(), settings.minRating()))
                .toList();
        List<BanPickSeasonResetPreviewResponse.PlayerRatingSample> samples = safeRows.stream()
                .filter(stats -> stats != null && stats.getUser() != null && stats.getUser().getId() != null)
                .sorted(Comparator
                        .comparingInt((PlayerStats stats) -> safeStoredRating(stats.getRating(), settings.seasonBaseRating()))
                        .reversed()
                        .thenComparing(stats -> stats.getUser().getId()))
                .limit(SAMPLE_LIMIT)
                .map(stats -> toSample(stats, type, settings))
                .toList();

        return new BanPickSeasonResetPreviewResponse(
                type,
                settings.seasonBaseRating(),
                type.formulaDescription(settings.seasonBaseRating()),
                safeRows.size(),
                summarize(beforeRatings),
                summarize(afterRatings),
                samples
        );
    }

    private BanPickSeasonResetPreviewResponse.PlayerRatingSample toSample(PlayerStats stats,
                                                                          BanPickSeasonResetType type,
                                                                          BanPickRatingSettingsSnapshot settings) {
        User user = stats.getUser();
        int beforeRating = safeStoredRating(stats.getRating(), settings.seasonBaseRating());
        return new BanPickSeasonResetPreviewResponse.PlayerRatingSample(
                user.getId(),
                user.getEmail(),
                user.resolveDisplayName(),
                beforeRating,
                type.applyTo(beforeRating, settings.seasonBaseRating(), settings.minRating())
        );
    }

    private BanPickSeasonResetPreviewResponse.RatingSummary summarize(List<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return new BanPickSeasonResetPreviewResponse.RatingSummary(null, null, null);
        }

        IntSummaryStatistics summary = ratings.stream().mapToInt(Integer::intValue).summaryStatistics();
        BigDecimal average = BigDecimal.valueOf(summary.getAverage()).setScale(2, RoundingMode.HALF_UP);
        return new BanPickSeasonResetPreviewResponse.RatingSummary(
                summary.getMin(),
                summary.getMax(),
                average
        );
    }

    private BanPickSeasonResetType requireExecutableType(BanPickSeasonResetType type) {
        if (type == null || !type.isExecutable()) {
            throw new IllegalArgumentException("type phai la SOFT hoac HARD.");
        }
        return type;
    }

    private int safeStoredRating(Integer rating, int fallbackRating) {
        return rating != null ? Math.max(0, rating) : fallbackRating;
    }

    private String sanitizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }

    private String normalizeExecutor(String executedBy) {
        if (executedBy == null || executedBy.isBlank()) {
            return "UNKNOWN_ADMIN";
        }
        return executedBy.trim();
    }
}
