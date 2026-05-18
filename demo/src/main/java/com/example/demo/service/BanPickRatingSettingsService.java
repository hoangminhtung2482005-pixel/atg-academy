package com.example.demo.service;

import com.example.demo.dto.banpick.BanPickRatingSettingsResponse;
import com.example.demo.dto.banpick.BanPickRatingSettingsUpdateRequest;
import com.example.demo.entity.BanPickRankResetLog;
import com.example.demo.entity.BanPickRatingSettings;
import com.example.demo.entity.BanPickSeasonResetType;
import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import com.example.demo.repository.BanPickRankResetLogRepository;
import com.example.demo.repository.BanPickRatingSettingsRepository;
import com.example.demo.repository.DraftHistoryRepository;
import com.example.demo.repository.PlayerStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class BanPickRatingSettingsService implements BanPickRatingSettingsAccessor {

    private static final long SETTINGS_ROW_ID = 1L;

    private final BanPickRatingSettingsRepository settingsRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final DraftHistoryRepository draftHistoryRepository;
    private final BanPickRankResetLogRepository resetLogRepository;
    private final BanPickSeasonResetSchedule seasonResetSchedule;
    private final Clock clock;

    @Autowired
    public BanPickRatingSettingsService(BanPickRatingSettingsRepository settingsRepository,
                                        PlayerStatsRepository playerStatsRepository,
                                        DraftHistoryRepository draftHistoryRepository,
                                        BanPickRankResetLogRepository resetLogRepository,
                                        BanPickSeasonResetSchedule seasonResetSchedule) {
        this(
                settingsRepository,
                playerStatsRepository,
                draftHistoryRepository,
                resetLogRepository,
                seasonResetSchedule,
                Clock.systemDefaultZone()
        );
    }

    BanPickRatingSettingsService(BanPickRatingSettingsRepository settingsRepository,
                                 PlayerStatsRepository playerStatsRepository,
                                 DraftHistoryRepository draftHistoryRepository,
                                 BanPickRankResetLogRepository resetLogRepository,
                                 BanPickSeasonResetSchedule seasonResetSchedule,
                                 Clock clock) {
        this.settingsRepository = settingsRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.draftHistoryRepository = draftHistoryRepository;
        this.resetLogRepository = resetLogRepository;
        this.seasonResetSchedule = seasonResetSchedule;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BanPickRatingSettingsSnapshot getCurrentSettings() {
        return settingsRepository.findById(SETTINGS_ROW_ID)
                .map(this::toSnapshot)
                .orElseGet(BanPickRatingSettingsSnapshot::defaults);
    }

    @Transactional(readOnly = true)
    public BanPickRatingSettingsResponse getSettingsView() {
        return toResponse(getCurrentSettings(), false);
    }

    @Transactional
    public BanPickRatingSettingsResponse updateSettings(BanPickRatingSettingsUpdateRequest request, String updatedBy) {
        BanPickRatingSettingsUpdateRequest safeRequest = requireRequest(request);
        BanPickRatingSettingsSnapshot previous = getCurrentSettings();
        BanPickRatingSettings entity = settingsRepository.findById(SETTINGS_ROW_ID)
                .orElseGet(this::newSettingsEntity);

        applyRequest(entity, safeRequest);
        validateEntity(entity);

        LocalDateTime now = LocalDateTime.now(clock);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(normalizeActor(updatedBy));
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }

        BanPickRatingSettings saved = settingsRepository.save(entity);
        BanPickRatingSettingsSnapshot current = toSnapshot(saved);
        boolean replayAnchorAdvanced = advanceReplayBarrierIfNeeded(previous, current, now);
        return toResponse(current, replayAnchorAdvanced);
    }

    @Transactional
    public BanPickRatingSettingsResponse resetDefaults(String updatedBy) {
        BanPickRatingSettingsSnapshot defaults = BanPickRatingSettingsSnapshot.defaults();
        BanPickRatingSettingsSnapshot previous = getCurrentSettings();
        BanPickRatingSettings entity = settingsRepository.findById(SETTINGS_ROW_ID)
                .orElseGet(this::newSettingsEntity);

        applySnapshot(entity, defaults);
        LocalDateTime now = LocalDateTime.now(clock);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(normalizeActor(updatedBy));
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }

        BanPickRatingSettings saved = settingsRepository.save(entity);
        BanPickRatingSettingsSnapshot current = toSnapshot(saved);
        boolean replayAnchorAdvanced = advanceReplayBarrierIfNeeded(previous, current, now);
        return toResponse(current, replayAnchorAdvanced);
    }

    private BanPickRatingSettings newSettingsEntity() {
        BanPickRatingSettings entity = new BanPickRatingSettings();
        entity.setId(SETTINGS_ROW_ID);
        applySnapshot(entity, BanPickRatingSettingsSnapshot.defaults());
        return entity;
    }

    private void applyRequest(BanPickRatingSettings entity, BanPickRatingSettingsUpdateRequest request) {
        entity.setInitialRating(request.base().initialRating());
        entity.setBaseWinDelta(request.base().baseWinDelta());
        entity.setBaseLossDelta(request.base().baseLossDelta());
        entity.setMinRating(request.base().minRating());

        entity.setMacroEnabled(request.macro().enabled());
        entity.setMacroActiveWindowDays(request.macro().activeWindowDays());
        entity.setMacroBalanceRating(request.macro().balanceRating());
        entity.setMacroActiveTopPercent(request.macro().activeTopPercent());
        entity.setMacroRatingStep(request.macro().ratingStep());
        entity.setMacroWinAdjustmentPerStep(toDecimal(request.macro().winAdjustmentPerStep()));
        entity.setMacroMinWinDelta(request.macro().minWinDelta());
        entity.setMacroMinimumActivePlayers(request.macro().minimumActivePlayers());

        entity.setGapEnabled(request.gap().enabled());
        entity.setGapRatingDiffStep(request.gap().ratingDiffStep());
        entity.setGapModifierPerStep(toDecimal(request.gap().modifierPerStep()));
        entity.setGapMaxModifier(toDecimal(request.gap().maxModifier()));

        entity.setAntiTradingEnabled(request.antiTrading().enabled());
        entity.setAntiTradingWindowHours(request.antiTrading().windowHours());
        entity.setAntiTradingAllowedRecentMatches(request.antiTrading().allowedRecentMatches());

        entity.setDodgeEnabled(request.dodge().enabled());
        entity.setDodgeDisconnectGraceSeconds(request.dodge().disconnectGraceSeconds());
        entity.setDodgeCooldownMinutes(request.dodge().cooldownMinutes());
        entity.setDodgeApplyInDraftOnly(request.dodge().applyInDraftOnly());
        entity.setDodgeRejectResetDuringDraft(request.dodge().rejectResetDuringDraft());

        entity.setSeasonSchedulerEnabled(request.seasonalReset().schedulerEnabled());
        entity.setSeasonSoftResetMonths(serializeMonths(request.seasonalReset().softResetMonths()));
        entity.setSeasonHardResetMonths(serializeMonths(request.seasonalReset().hardResetMonths()));
        entity.setSeasonHardPriorityOverSoft(request.seasonalReset().hardPriorityOverSoft());
    }

    private void applySnapshot(BanPickRatingSettings entity, BanPickRatingSettingsSnapshot snapshot) {
        entity.setInitialRating(snapshot.initialRating());
        entity.setBaseWinDelta(snapshot.baseWinDelta());
        entity.setBaseLossDelta(snapshot.baseLossDelta());
        entity.setMinRating(snapshot.minRating());
        entity.setMacroEnabled(snapshot.macroEnabled());
        entity.setMacroActiveWindowDays(snapshot.macroActiveWindowDays());
        entity.setMacroBalanceRating(snapshot.macroBalanceRating());
        entity.setMacroActiveTopPercent(snapshot.macroActiveTopPercent());
        entity.setMacroRatingStep(snapshot.macroRatingStep());
        entity.setMacroWinAdjustmentPerStep(toDecimal(snapshot.macroWinAdjustmentPerStep()));
        entity.setMacroMinWinDelta(snapshot.macroMinWinDelta());
        entity.setMacroMinimumActivePlayers(snapshot.macroMinimumActivePlayers());
        entity.setGapEnabled(snapshot.gapEnabled());
        entity.setGapRatingDiffStep(snapshot.gapRatingDiffStep());
        entity.setGapModifierPerStep(toDecimal(snapshot.gapModifierPerStep()));
        entity.setGapMaxModifier(toDecimal(snapshot.gapMaxModifier()));
        entity.setAntiTradingEnabled(snapshot.antiTradingEnabled());
        entity.setAntiTradingWindowHours(snapshot.antiTradingWindowHours());
        entity.setAntiTradingAllowedRecentMatches(snapshot.antiTradingAllowedRecentMatches());
        entity.setDodgeEnabled(snapshot.dodgeEnabled());
        entity.setDodgeDisconnectGraceSeconds(snapshot.dodgeDisconnectGraceSeconds());
        entity.setDodgeCooldownMinutes(snapshot.dodgeCooldownMinutes());
        entity.setDodgeApplyInDraftOnly(snapshot.dodgeApplyInDraftOnly());
        entity.setDodgeRejectResetDuringDraft(snapshot.dodgeRejectResetDuringDraft());
        entity.setSeasonSchedulerEnabled(snapshot.seasonSchedulerEnabled());
        entity.setSeasonSoftResetMonths(serializeMonths(snapshot.seasonSoftResetMonths()));
        entity.setSeasonHardResetMonths(serializeMonths(snapshot.seasonHardResetMonths()));
        entity.setSeasonHardPriorityOverSoft(snapshot.seasonHardPriorityOverSoft());
    }

    private BanPickRatingSettingsSnapshot toSnapshot(BanPickRatingSettings entity) {
        BanPickRatingSettingsSnapshot defaults = BanPickRatingSettingsSnapshot.defaults();
        if (entity == null) {
            return defaults;
        }
        return new BanPickRatingSettingsSnapshot(
                safeInt(entity.getInitialRating(), defaults.initialRating()),
                safeInt(entity.getBaseWinDelta(), defaults.baseWinDelta()),
                safeInt(entity.getBaseLossDelta(), defaults.baseLossDelta()),
                safeInt(entity.getMinRating(), defaults.minRating()),
                safeBoolean(entity.getMacroEnabled(), defaults.macroEnabled()),
                safeInt(entity.getMacroActiveWindowDays(), defaults.macroActiveWindowDays()),
                safeInt(entity.getMacroBalanceRating(), defaults.macroBalanceRating()),
                safeInt(entity.getMacroActiveTopPercent(), defaults.macroActiveTopPercent()),
                safeInt(entity.getMacroRatingStep(), defaults.macroRatingStep()),
                safeDouble(entity.getMacroWinAdjustmentPerStep(), defaults.macroWinAdjustmentPerStep()),
                safeInt(entity.getMacroMinWinDelta(), defaults.macroMinWinDelta()),
                safeInt(entity.getMacroMinimumActivePlayers(), defaults.macroMinimumActivePlayers()),
                safeBoolean(entity.getGapEnabled(), defaults.gapEnabled()),
                safeInt(entity.getGapRatingDiffStep(), defaults.gapRatingDiffStep()),
                safeDouble(entity.getGapModifierPerStep(), defaults.gapModifierPerStep()),
                safeDouble(entity.getGapMaxModifier(), defaults.gapMaxModifier()),
                safeBoolean(entity.getAntiTradingEnabled(), defaults.antiTradingEnabled()),
                safeInt(entity.getAntiTradingWindowHours(), defaults.antiTradingWindowHours()),
                safeInt(entity.getAntiTradingAllowedRecentMatches(), defaults.antiTradingAllowedRecentMatches()),
                safeBoolean(entity.getDodgeEnabled(), defaults.dodgeEnabled()),
                safeInt(entity.getDodgeDisconnectGraceSeconds(), defaults.dodgeDisconnectGraceSeconds()),
                safeInt(entity.getDodgeCooldownMinutes(), defaults.dodgeCooldownMinutes()),
                safeBoolean(entity.getDodgeApplyInDraftOnly(), defaults.dodgeApplyInDraftOnly()),
                safeBoolean(entity.getDodgeRejectResetDuringDraft(), defaults.dodgeRejectResetDuringDraft()),
                safeBoolean(entity.getSeasonSchedulerEnabled(), defaults.seasonSchedulerEnabled()),
                parseMonths(entity.getSeasonSoftResetMonths(), defaults.seasonSoftResetMonths()),
                parseMonths(entity.getSeasonHardResetMonths(), defaults.seasonHardResetMonths()),
                safeBoolean(entity.getSeasonHardPriorityOverSoft(), defaults.seasonHardPriorityOverSoft()),
                BanPickRatingDefaults.SEASON_RESET_CONFIRMATION_TEXT,
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    private BanPickRatingSettingsResponse toResponse(BanPickRatingSettingsSnapshot snapshot, boolean replayAnchorAdvanced) {
        MacroDiagnostics macroDiagnostics = resolveMacroDiagnostics(snapshot);
        Optional<BanPickRankResetLog> lastResetLog = resetLogRepository.findFirstByOrderByExecutedAtDescIdDesc();
        BanPickSeasonResetSchedule.ScheduledReset nextReset = seasonResetSchedule.findNextReset(
                LocalDate.now(clock),
                snapshot.seasonSoftResetMonths(),
                snapshot.seasonHardResetMonths(),
                snapshot.seasonHardPriorityOverSoft()
        ).orElse(null);

        return new BanPickRatingSettingsResponse(
                new BanPickRatingSettingsResponse.BaseSettings(
                        snapshot.initialRating(),
                        snapshot.baseWinDelta(),
                        snapshot.baseLossDelta(),
                        snapshot.minRating()
                ),
                new BanPickRatingSettingsResponse.MacroSettings(
                        snapshot.macroEnabled(),
                        snapshot.macroActiveWindowDays(),
                        snapshot.macroBalanceRating(),
                        snapshot.macroActiveTopPercent(),
                        snapshot.macroRatingStep(),
                        snapshot.macroWinAdjustmentPerStep(),
                        snapshot.macroMinWinDelta(),
                        snapshot.macroMinimumActivePlayers()
                ),
                new BanPickRatingSettingsResponse.GapSettings(
                        snapshot.gapEnabled(),
                        snapshot.gapRatingDiffStep(),
                        snapshot.gapModifierPerStep(),
                        snapshot.gapMaxModifier()
                ),
                new BanPickRatingSettingsResponse.AntiTradingSettings(
                        snapshot.antiTradingEnabled(),
                        snapshot.antiTradingWindowHours(),
                        snapshot.antiTradingAllowedRecentMatches(),
                        0,
                        0
                ),
                new BanPickRatingSettingsResponse.DodgeSettings(
                        snapshot.dodgeEnabled(),
                        snapshot.dodgeDisconnectGraceSeconds(),
                        snapshot.dodgeCooldownMinutes(),
                        snapshot.dodgeApplyInDraftOnly(),
                        snapshot.dodgeRejectResetDuringDraft()
                ),
                new BanPickRatingSettingsResponse.SeasonalResetSettings(
                        snapshot.seasonSchedulerEnabled(),
                        snapshot.seasonSoftResetMonths(),
                        snapshot.seasonHardResetMonths(),
                        snapshot.seasonHardPriorityOverSoft(),
                        snapshot.resetConfirmationText()
                ),
                new BanPickRatingSettingsResponse.Diagnostics(
                        macroDiagnostics.currentMacroWinDelta(),
                        macroDiagnostics.currentActivePlayerCount(),
                        macroDiagnostics.currentActivePoolSize(),
                        macroDiagnostics.activePoolAverageRating(),
                        lastResetLog.map(this::toLastResetLog).orElse(null),
                        nextReset != null
                                ? new BanPickRatingSettingsResponse.NextScheduledReset(nextReset.scheduledDate(), nextReset.resetType())
                                : null,
                        snapshot.updatedAt(),
                        snapshot.updatedBy(),
                        replayAnchorAdvanced
                )
        );
    }

    private BanPickRatingSettingsResponse.LastResetLog toLastResetLog(BanPickRankResetLog log) {
        return new BanPickRatingSettingsResponse.LastResetLog(
                log.getId(),
                log.getResetType(),
                log.getScheduledDate(),
                log.getExecutedAt(),
                log.getAffectedPlayers(),
                log.getBaseRating(),
                log.getFormula(),
                log.getExecutedBy(),
                log.getNote()
        );
    }

    private MacroDiagnostics resolveMacroDiagnostics(BanPickRatingSettingsSnapshot settings) {
        LocalDate snapshotDate = LocalDate.now(clock);
        LocalDateTime windowEnd = snapshotDate.atStartOfDay();
        LocalDateTime windowStart = windowEnd.minusDays(settings.macroActiveWindowDays());
        List<DraftHistory> completedHistories = draftHistoryRepository.findCompletedBetween(windowStart, windowEnd);
        Map<Long, Integer> completedMatchesByUserId = countCompletedMatchesByUser(completedHistories);
        int activePlayerCount = completedMatchesByUserId.size();
        if (activePlayerCount < settings.macroMinimumActivePlayers()) {
            return new MacroDiagnostics(settings.baseWinDelta(), activePlayerCount, 0, null);
        }

        List<Map.Entry<Long, Integer>> sortedPlayers = completedMatchesByUserId.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        int basePoolSize = Math.max(1, (int) Math.ceil(sortedPlayers.size() * (settings.macroActiveTopPercent() / 100.0)));
        int cutoffMatchCount = sortedPlayers.get(Math.min(basePoolSize, sortedPlayers.size()) - 1).getValue();
        List<Long> activePoolUserIds = sortedPlayers.stream()
                .filter(entry -> entry.getValue() >= cutoffMatchCount)
                .map(Map.Entry::getKey)
                .toList();

        if (activePoolUserIds.isEmpty()) {
            return new MacroDiagnostics(settings.baseWinDelta(), activePlayerCount, 0, null);
        }

        Map<Long, Integer> ratingsByUserId = currentRatingsByUserId(settings);
        double averageRating = activePoolUserIds.stream()
                .mapToInt(userId -> ratingsByUserId.getOrDefault(userId, settings.initialRating()))
                .average()
                .orElse(settings.macroBalanceRating());

        int winDelta = resolveMacroWinDelta(settings, averageRating);
        return new MacroDiagnostics(
                winDelta,
                activePlayerCount,
                activePoolUserIds.size(),
                BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private int resolveMacroWinDelta(BanPickRatingSettingsSnapshot settings, double averageRating) {
        if (!settings.macroEnabled()) {
            return settings.baseWinDelta();
        }
        double difference = averageRating - settings.macroBalanceRating();
        double adjustmentRatio = (difference / settings.macroRatingStep()) * settings.macroWinAdjustmentPerStep();
        double rawWinDelta = settings.baseWinDelta() * (1.0 - adjustmentRatio);
        if (!Double.isFinite(rawWinDelta)) {
            return settings.baseWinDelta();
        }
        return Math.max(settings.macroMinWinDelta(), (int) Math.round(rawWinDelta));
    }

    private Map<Long, Integer> countCompletedMatchesByUser(List<DraftHistory> completedHistories) {
        Map<Long, Integer> completedMatchesByUserId = new LinkedHashMap<>();
        if (completedHistories == null) {
            return completedMatchesByUserId;
        }
        for (DraftHistory history : completedHistories) {
            incrementCompletedMatches(completedMatchesByUserId, history != null ? history.getBlueUser() : null);
            incrementCompletedMatches(completedMatchesByUserId, history != null ? history.getRedUser() : null);
        }
        return completedMatchesByUserId;
    }

    private void incrementCompletedMatches(Map<Long, Integer> countsByUserId, User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        countsByUserId.merge(user.getId(), 1, Integer::sum);
    }

    private Map<Long, Integer> currentRatingsByUserId(BanPickRatingSettingsSnapshot settings) {
        Map<Long, Integer> ratingsByUserId = new LinkedHashMap<>();
        for (PlayerStats stats : playerStatsRepository.findAll()) {
            if (stats == null || stats.getUser() == null || stats.getUser().getId() == null) {
                continue;
            }
            ratingsByUserId.put(stats.getUser().getId(), safeStoredRating(stats.getRating(), settings.initialRating()));
        }
        return ratingsByUserId;
    }

    private boolean advanceReplayBarrierIfNeeded(BanPickRatingSettingsSnapshot previous,
                                                 BanPickRatingSettingsSnapshot current,
                                                 LocalDateTime anchoredAt) {
        if (!current.changesReplaySensitiveRulesComparedTo(previous)) {
            return false;
        }

        List<PlayerStats> statsRows = playerStatsRepository.findAll();
        if (statsRows.isEmpty()) {
            return false;
        }

        for (PlayerStats stats : statsRows) {
            if (stats == null) {
                continue;
            }
            stats.setRatingAnchor(safeStoredRating(stats.getRating(), previous.initialRating()));
            stats.setRatingAnchorAt(anchoredAt);
        }
        playerStatsRepository.saveAll(statsRows);
        return true;
    }

    private void validateEntity(BanPickRatingSettings entity) {
        require(entity.getInitialRating() != null && entity.getInitialRating() >= 0, "initialRating phai >= 0.");
        require(entity.getBaseWinDelta() != null && entity.getBaseWinDelta() >= 0, "baseWinDelta phai >= 0.");
        require(entity.getBaseLossDelta() != null && entity.getBaseLossDelta() <= 0, "baseLossDelta phai <= 0.");
        require(entity.getMinRating() != null && entity.getMinRating() >= 0, "minRating phai >= 0.");
        require(entity.getInitialRating() != null && entity.getMinRating() <= entity.getInitialRating(),
                "minRating khong duoc lon hon initialRating.");

        require(entity.getMacroEnabled() != null, "macro.enabled la bat buoc.");
        require(inRange(entity.getMacroActiveWindowDays(), 1, 365), "macro.activeWindowDays phai trong khoang 1-365.");
        require(entity.getMacroBalanceRating() != null && entity.getMacroBalanceRating() >= 0, "macro.balanceRating phai >= 0.");
        require(inRange(entity.getMacroActiveTopPercent(), 1, 100), "macro.activeTopPercent phai trong khoang 1-100.");
        require(entity.getMacroRatingStep() != null && entity.getMacroRatingStep() > 0, "macro.ratingStep phai > 0.");
        require(inRange(entity.getMacroWinAdjustmentPerStep(), 0.0, 1.0), "macro.winAdjustmentPerStep phai trong khoang 0-1.");
        require(entity.getMacroMinWinDelta() != null && entity.getMacroMinWinDelta() >= 0, "macro.minWinDelta phai >= 0.");
        require(entity.getMacroMinimumActivePlayers() != null && entity.getMacroMinimumActivePlayers() >= 1,
                "macro.minimumActivePlayers phai >= 1.");

        require(entity.getGapEnabled() != null, "gap.enabled la bat buoc.");
        require(entity.getGapRatingDiffStep() != null && entity.getGapRatingDiffStep() > 0, "gap.ratingDiffStep phai > 0.");
        require(inRange(entity.getGapModifierPerStep(), 0.0, 1.0), "gap.modifierPerStep phai trong khoang 0-1.");
        require(inRange(entity.getGapMaxModifier(), 0.0, 1.0), "gap.maxModifier phai trong khoang 0-1.");

        require(entity.getAntiTradingEnabled() != null, "antiTrading.enabled la bat buoc.");
        require(inRange(entity.getAntiTradingWindowHours(), 1, 720), "antiTrading.windowHours phai trong khoang 1-720.");
        require(entity.getAntiTradingAllowedRecentMatches() != null && entity.getAntiTradingAllowedRecentMatches() >= 0,
                "antiTrading.allowedRecentMatches phai >= 0.");

        require(entity.getDodgeEnabled() != null, "dodge.enabled la bat buoc.");
        require(inRange(entity.getDodgeDisconnectGraceSeconds(), 0, 300), "dodge.disconnectGraceSeconds phai trong khoang 0-300.");
        require(inRange(entity.getDodgeCooldownMinutes(), 0, 1440), "dodge.cooldownMinutes phai trong khoang 0-1440.");
        require(entity.getDodgeApplyInDraftOnly() != null, "dodge.applyInDraftOnly la bat buoc.");
        require(entity.getDodgeRejectResetDuringDraft() != null, "dodge.rejectResetDuringDraft la bat buoc.");

        require(entity.getSeasonSchedulerEnabled() != null, "seasonalReset.schedulerEnabled la bat buoc.");
        parseMonths(entity.getSeasonSoftResetMonths(), List.of());
        parseMonths(entity.getSeasonHardResetMonths(), List.of());
        require(entity.getSeasonHardPriorityOverSoft() != null, "seasonalReset.hardPriorityOverSoft la bat buoc.");
    }

    private BanPickRatingSettingsUpdateRequest requireRequest(BanPickRatingSettingsUpdateRequest request) {
        if (request == null
                || request.base() == null
                || request.macro() == null
                || request.gap() == null
                || request.antiTrading() == null
                || request.dodge() == null
                || request.seasonalReset() == null) {
            throw new IllegalArgumentException("Payload rating settings khong hop le.");
        }
        return request;
    }

    private List<Integer> parseMonths(String encoded, List<Integer> fallback) {
        if (encoded == null || encoded.isBlank()) {
            return List.copyOf(fallback);
        }
        try {
            Set<Integer> uniqueMonths = new LinkedHashSet<>();
            for (String token : encoded.split(",")) {
                String trimmed = token != null ? token.trim() : "";
                if (trimmed.isEmpty()) {
                    continue;
                }
                int month = Integer.parseInt(trimmed);
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Month phai trong khoang 1-12.");
                }
                uniqueMonths.add(month);
            }
            return uniqueMonths.stream().sorted().toList();
        } catch (RuntimeException exception) {
            if (fallback != null) {
                return List.copyOf(fallback);
            }
            throw new IllegalArgumentException("Danh sach thang reset khong hop le.");
        }
    }

    private String serializeMonths(List<Integer> months) {
        List<Integer> normalized = normalizeMonths(months);
        if (normalized.isEmpty()) {
            return "";
        }
        List<String> encoded = new ArrayList<>();
        for (Integer month : normalized) {
            encoded.add(String.format("%02d", month));
        }
        return String.join(",", encoded);
    }

    private List<Integer> normalizeMonths(List<Integer> months) {
        if (months == null) {
            return List.of();
        }
        Set<Integer> uniqueMonths = new LinkedHashSet<>();
        for (Integer month : months) {
            require(month != null && month >= 1 && month <= 12, "Reset month phai trong khoang 1-12.");
            uniqueMonths.add(month);
        }
        return uniqueMonths.stream().sorted().toList();
    }

    private String normalizeActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "UNKNOWN_ADMIN";
        }
        return actor.trim();
    }

    private int safeStoredRating(Integer value, int fallback) {
        return value != null ? Math.max(0, value) : fallback;
    }

    private boolean inRange(Integer value, int min, int max) {
        return value != null && value >= min && value <= max;
    }

    private boolean inRange(BigDecimal value, double min, double max) {
        if (value == null) {
            return false;
        }
        BigDecimal lowerBound = BigDecimal.valueOf(min);
        BigDecimal upperBound = BigDecimal.valueOf(max);
        return value.compareTo(lowerBound) >= 0 && value.compareTo(upperBound) <= 0;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private int safeInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private double safeDouble(BigDecimal value, double fallback) {
        return value != null && Double.isFinite(value.doubleValue()) ? value.doubleValue() : fallback;
    }

    private BigDecimal toDecimal(Double value) {
        return value != null && Double.isFinite(value) ? BigDecimal.valueOf(value) : null;
    }

    private BigDecimal toDecimal(double value) {
        return Double.isFinite(value) ? BigDecimal.valueOf(value) : null;
    }

    private boolean safeBoolean(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private record MacroDiagnostics(
            Integer currentMacroWinDelta,
            Integer currentActivePlayerCount,
            Integer currentActivePoolSize,
            BigDecimal activePoolAverageRating
    ) {
    }
}
