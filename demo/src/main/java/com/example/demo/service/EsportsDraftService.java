package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftTournamentScopeAggregate;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmResponse;
import com.example.demo.dto.esports.EsportsGameDraftImportPreviewResponse;
import com.example.demo.dto.esports.EsportsGameDraftRequest;
import com.example.demo.dto.esports.EsportsGameDraftResponse;
import com.example.demo.entity.EsportsGameDraft;
import com.example.demo.entity.EsportsLineupLaneRole;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.entity.EsportsTeam;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.entity.Hero;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.EsportsDraftDefaults;
import com.example.demo.util.EsportsStageSupport;
import com.example.demo.util.EsportsTierSupport;
import com.example.demo.util.EsportsTournamentCatalog;
import com.example.demo.util.SlugUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class EsportsDraftService {

    private static final int MAX_BANS_PER_SIDE = 5;
    private static final int EXPECTED_COMPLETE_BANS = 8;
    private static final int EXPECTED_COMPLETE_PICKS = 10;
    private static final String DEFAULT_IMPORT_MATCH_STAGE = "bang";
    private static final String IMPORT_DRAFT_FORMAT_CODE = "AOV_STANDARD_18";
    private static final String IMPORT_SOURCE = "import";
    private static final long IMPORT_PREVIEW_TTL_MINUTES = 30L;
    private static final DateTimeFormatter IMPORT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> IMPORT_OPTIONAL_HEADERS = List.of("Stage");
    private static final List<String> EXPORT_CSV_HEADERS = List.of(
            "Date",
            "Tournament",
            "Match",
            "Team_1",
            "T1_Side",
            "T1_DSL",
            "T1_JGL",
            "T1_MID",
            "T1_ADL",
            "T1_SUP",
            "T1_Ban_1",
            "T1_Ban_2",
            "T1_Ban_3",
            "T1_Ban_4",
            "T1_Ban_5",
            "Team_2",
            "T2_Side",
            "T2_DSL",
            "T2_JGL",
            "T2_MID",
            "T2_ADL",
            "T2_SUP",
            "T2_Ban_1",
            "T2_Ban_2",
            "T2_Ban_3",
            "T2_Ban_4",
            "T2_Ban_5",
            "Winner",
            "Length"
    );

    private final EsportsMatchRepository esportsMatchRepository;
    private final EsportsTeamRepository esportsTeamRepository;
    private final EsportsTournamentRepository esportsTournamentRepository;
    private final HeroRepository heroRepository;
    private final EsportsGameDraftRepository esportsGameDraftRepository;
    private final EloCalculationService eloCalculationService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ImportPreviewSession> importPreviewSessions = new ConcurrentHashMap<>();

    public EsportsDraftService(EsportsMatchRepository esportsMatchRepository,
                               EsportsTeamRepository esportsTeamRepository,
                               EsportsTournamentRepository esportsTournamentRepository,
                               HeroRepository heroRepository,
                               EsportsGameDraftRepository esportsGameDraftRepository,
                               EloCalculationService eloCalculationService,
                               ObjectMapper objectMapper) {
        this.esportsMatchRepository = esportsMatchRepository;
        this.esportsTeamRepository = esportsTeamRepository;
        this.esportsTournamentRepository = esportsTournamentRepository;
        this.heroRepository = heroRepository;
        this.esportsGameDraftRepository = esportsGameDraftRepository;
        this.eloCalculationService = eloCalculationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<EsportsGameDraftResponse> listGameDraftsByMatch(Long matchId) {
        requireMatch(matchId);
        List<EsportsGameDraft> drafts = esportsGameDraftRepository.findByMatchIdOrderByGameNumberAsc(matchId);
        Map<Long, Hero> heroesById = loadHeroesForDrafts(drafts);
        return drafts.stream()
                .map(draft -> toGameDraftResponse(draft, heroesById))
                .toList();
    }

    @Transactional(readOnly = true)
    public EsportsGameDraftResponse getGameDraft(Long gameDraftId) {
        EsportsGameDraft draft = requireGameDraft(gameDraftId);
        return toGameDraftResponse(draft, loadHeroesForDraft(draft));
    }

    @Transactional
    public EsportsGameDraftResponse createGameDraft(Long matchId, EsportsGameDraftRequest request) {
        EsportsMatch match = requireMatch(matchId);
        NormalizedDraftWrite normalized = normalizeDraftWrite(match, null, request);

        EsportsGameDraft draft = new EsportsGameDraft();
        applyDraftWrite(draft, normalized);
        EsportsGameDraft saved = esportsGameDraftRepository.save(draft);
        return toGameDraftResponse(saved, normalized.heroesById());
    }

    @Transactional
    public EsportsGameDraftResponse updateGameDraft(Long gameDraftId, EsportsGameDraftRequest request) {
        EsportsGameDraft existing = requireGameDraft(gameDraftId);
        NormalizedDraftWrite normalized = normalizeDraftWrite(existing.getMatch(), existing, request);

        applyDraftWrite(existing, normalized);
        EsportsGameDraft saved = esportsGameDraftRepository.save(existing);
        return toGameDraftResponse(saved, normalized.heroesById());
    }

    @Transactional
    public void deleteGameDraft(Long gameDraftId) {
        esportsGameDraftRepository.delete(requireGameDraft(gameDraftId));
    }

    @Transactional(readOnly = true)
    public byte[] exportGameDraftsCsv(String tournamentName,
                                      Long matchId,
                                      LocalDate dateFrom,
                                      LocalDate dateTo) {
        return exportGameDraftsCsv(null, tournamentName, matchId, dateFrom, dateTo);
    }

    @Transactional(readOnly = true)
    public byte[] exportGameDraftsCsv(Long tournamentId,
                                      String tournamentName,
                                      Long matchId,
                                      LocalDate dateFrom,
                                      LocalDate dateTo) {
        DraftExportFilter filter = resolveDraftExportFilter(tournamentId, tournamentName, matchId, dateFrom, dateTo);
        if (filter.matchId() != null) {
            requireMatch(filter.matchId());
        }

        List<EsportsGameDraft> drafts = esportsGameDraftRepository.findAllForExportScope(
                filter.tournamentId(),
                filter.tournamentTier(),
                filter.matchId(),
                filter.dateFrom(),
                filter.dateTo()
        );
        Map<Long, Hero> heroesById = loadHeroesForDrafts(drafts);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(buildCsvLine(EXPORT_CSV_HEADERS));
        csv.append("\r\n");
        for (EsportsGameDraft draft : drafts) {
            csv.append(buildExportRow(draft, heroesById));
            csv.append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public EsportsGameDraftImportPreviewResponse previewGameDraftImport(MultipartFile file, boolean overwriteExisting) {
        ImportedSheet importedSheet = readImportSheet(file);
        pruneExpiredImportPreviewSessions();

        ImportReferenceContext context = loadImportReferenceContext();
        List<MutableImportRow> rows = new ArrayList<>();
        Map<SeriesGroupKey, SeriesGroupAccumulator> groups = new LinkedHashMap<>();

        for (ImportedSheetRow sourceRow : importedSheet.rows) {
            MutableImportRow row = parseImportRow(sourceRow, context, overwriteExisting, importedSheet.sourceTag);
            rows.add(row);
            if (row.groupKey != null) {
                groups.computeIfAbsent(
                                row.groupKey,
                                ignored -> new SeriesGroupAccumulator(
                                        row.groupKey,
                                        row.matchDate,
                                        row.matchStage,
                                        row.tournament,
                                        row.team1,
                                        row.team2))
                        .rows.add(row);
            }
        }

        markDuplicateRowsInsidePreview(groups);

        List<ResolvedGroupPlan> groupPlans = new ArrayList<>();
        int newMatchIndex = 0;
        for (SeriesGroupAccumulator group : groups.values()) {
            ResolvedGroupPlan plan = resolveGroupPlan(group, context, overwriteExisting, newMatchIndex);
            if (plan.createMatch) {
                newMatchIndex++;
            }
            groupPlans.add(plan);
        }

        List<String> previewErrors = new ArrayList<>();
        List<String> previewWarnings = new ArrayList<>();
        int validRows = 0;
        int rowsWithWarnings = 0;
        int errorRows = 0;
        int matchesToCreate = 0;
        int matchesToUpdate = 0;
        int draftsToCreate = 0;
        int draftsToOverwrite = 0;

        for (ResolvedGroupPlan plan : groupPlans) {
            if (plan.createMatch) {
                matchesToCreate++;
                previewWarnings.add("Match mới sẽ được tạo với stage=" + plan.matchStage + " và giờ mặc định 12:00.");
            } else if (plan.updateTournamentLink || plan.updateSeriesScore) {
                matchesToUpdate++;
            }
        }

        for (MutableImportRow row : rows) {
            if (row.hasErrors()) {
                errorRows++;
            } else {
                validRows++;
                if (row.overwriteDraftId != null) {
                    draftsToOverwrite++;
                } else {
                    draftsToCreate++;
                }
            }
            if (!row.warnings.isEmpty()) {
                rowsWithWarnings++;
            }
        }

        if (errorRows > 0) {
            previewErrors.add("Có " + errorRows + " dòng đang lỗi. Cần sửa hết lỗi trước khi Confirm Import.");
        }
        if (matchesToUpdate > 0) {
            previewWarnings.add("Một số match cha sẽ được cập nhật tournament hoặc tỷ số series để đồng bộ với file import.");
        }
        if (draftsToOverwrite > 0) {
            previewWarnings.add("Preview đang bật overwrite cho " + draftsToOverwrite + " game draft đã tồn tại trong DB.");
        }

        String previewToken = UUID.randomUUID().toString();
        boolean readyToImport = errorRows == 0 && !rows.isEmpty();
        ImportPreviewSession session = new ImportPreviewSession(
                previewToken,
                LocalDateTime.now(),
                groupPlans,
                readyToImport
        );
        importPreviewSessions.put(previewToken, session);

        return new EsportsGameDraftImportPreviewResponse(
                previewToken,
                readyToImport,
                new EsportsGameDraftImportPreviewResponse.ImportSummary(
                        rows.size(),
                        validRows,
                        errorRows,
                        rowsWithWarnings,
                        matchesToCreate,
                        matchesToUpdate,
                        draftsToCreate,
                        draftsToOverwrite
                ),
                rows.stream().map(this::toRowPreview).toList(),
                distinctMessages(previewErrors),
                distinctMessages(previewWarnings)
        );
    }

    @Transactional
    public EsportsGameDraftImportConfirmResponse confirmGameDraftImport(EsportsGameDraftImportConfirmRequest request) {
        if (request == null || !StringUtils.hasText(request.previewToken())) {
            throw new IllegalArgumentException("previewToken là bắt buộc.");
        }

        pruneExpiredImportPreviewSessions();
        ImportPreviewSession session = importPreviewSessions.get(request.previewToken().trim());
        if (session == null) {
            throw new IllegalArgumentException("Preview import đã hết hạn hoặc không tồn tại. Hãy preview lại file.");
        }
        if (!session.readyToImport) {
            throw new IllegalArgumentException("Preview hiện tại vẫn còn lỗi, không thể confirm import.");
        }

        int createdMatches = 0;
        int updatedMatches = 0;
        int createdDrafts = 0;
        int overwrittenDrafts = 0;
        boolean rankingsRecalculated = false;
        Set<Long> affectedMatchIds = new LinkedHashSet<>();

        for (ResolvedGroupPlan plan : session.groupPlans) {
            EsportsMatch targetMatch;
            boolean matchChanged = false;

            if (plan.createMatch) {
                targetMatch = new EsportsMatch();
                targetMatch.setMatchDate(plan.newMatchDateTime);
                targetMatch.setTeam1Code(plan.matchTeam1.getTeamCode());
                targetMatch.setTeam2Code(plan.matchTeam2.getTeamCode());
                targetMatch.setTournament(plan.tournament);
                targetMatch.setTier(resolveMatchTier(plan.tournament));
                targetMatch.setStage(plan.matchStage);
                targetMatch.setScore1(plan.seriesScore1);
                targetMatch.setScore2(plan.seriesScore2);
                targetMatch = esportsMatchRepository.save(targetMatch);
                createdMatches++;
                matchChanged = true;
            } else {
                targetMatch = requireMatch(plan.matchId);
                if (plan.updateTournamentLink) {
                    targetMatch.setTournament(plan.tournament);
                    targetMatch.setTier(resolveMatchTier(plan.tournament));
                    matchChanged = true;
                }
                if (plan.updateSeriesScore) {
                    targetMatch.setScore1(plan.seriesScore1);
                    targetMatch.setScore2(plan.seriesScore2);
                    matchChanged = true;
                }
                if (matchChanged) {
                    targetMatch = esportsMatchRepository.save(targetMatch);
                    updatedMatches++;
                }
            }

            affectedMatchIds.add(targetMatch.getId());
            for (MutableImportRow row : plan.rows) {
                EsportsGameDraft draft = row.overwriteDraftId != null
                        ? requireGameDraft(row.overwriteDraftId)
                        : new EsportsGameDraft();
                NormalizedDraftWrite normalized = normalizeDraftWrite(
                        targetMatch,
                        draft.getId() != null ? draft : null,
                        row.draftRequest
                );
                applyDraftWrite(draft, normalized);
                esportsGameDraftRepository.save(draft);
                if (row.overwriteDraftId != null) {
                    overwrittenDrafts++;
                } else {
                    createdDrafts++;
                }
            }

            if (matchChanged) {
                rankingsRecalculated = true;
            }
        }

        if (rankingsRecalculated) {
            eloCalculationService.calculateAllRankings();
        }
        importPreviewSessions.remove(request.previewToken().trim());
        return new EsportsGameDraftImportConfirmResponse(
                createdDrafts + overwrittenDrafts,
                createdMatches,
                updatedMatches,
                createdDrafts,
                overwrittenDrafts,
                affectedMatchIds.stream().toList(),
                affectedMatchIds.size(),
                rankingsRecalculated
        );
    }

    private ImportedSheet readImportSheet(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import dang trong.");
        }
        String filename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim()
                : "upload";

        List<List<String>> rawRows;
        try {
            rawRows = isExcelFile(filename)
                    ? readExcelRows(file)
                    : readCsvRows(file);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc file import: " + exception.getMessage(), exception);
        }

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("File import không có dữ liệu.");
        }

        List<String> headerRow = rawRows.get(0).stream()
                .map(this::cleanCellText)
                .toList();
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int index = 0; index < headerRow.size(); index++) {
            String normalizedHeader = normalizeHeader(headerRow.get(index));
            if (StringUtils.hasText(normalizedHeader) && !headerIndex.containsKey(normalizedHeader)) {
                headerIndex.put(normalizedHeader, index);
            }
        }

        List<String> missingHeaders = EXPORT_CSV_HEADERS.stream()
                .filter(header -> !headerIndex.containsKey(normalizeHeader(header)))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("File import thiếu cột bắt buộc: " + String.join(", ", missingHeaders));
        }

        List<ImportedSheetRow> rows = new ArrayList<>();
        List<String> availableImportHeaders = new ArrayList<>(EXPORT_CSV_HEADERS);
        IMPORT_OPTIONAL_HEADERS.stream()
                .filter(header -> headerIndex.containsKey(normalizeHeader(header)))
                .forEach(availableImportHeaders::add);
        for (int rowIndex = 1; rowIndex < rawRows.size(); rowIndex++) {
            List<String> values = rawRows.get(rowIndex);
            boolean blank = values.stream().allMatch(value -> !StringUtils.hasText(cleanCellText(value)));
            if (blank) {
                continue;
            }

            Map<String, String> cells = new LinkedHashMap<>();
            for (String header : availableImportHeaders) {
                int columnIndex = headerIndex.get(normalizeHeader(header));
                String value = columnIndex < values.size() ? values.get(columnIndex) : "";
                cells.put(header, cleanCellText(value));
            }
            rows.add(new ImportedSheetRow(rowIndex + 1, cells));
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("File import chỉ có header, không có dòng dữ liệu nào.");
        }

        String sourceTag = isExcelFile(filename) ? "excel-import" : "csv-import";
        return new ImportedSheet(filename, sourceTag, rows);
    }

    private List<List<String>> readCsvRows(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (inQuotes) {
                if (character == '"') {
                    boolean escapedQuote = index + 1 < content.length() && content.charAt(index + 1) == '"';
                    if (escapedQuote) {
                        currentField.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(character);
                }
                continue;
            }

            if (character == '"') {
                inQuotes = true;
            } else if (character == ',') {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
            } else if (character == '\r') {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
                if (index + 1 < content.length() && content.charAt(index + 1) == '\n') {
                    index++;
                }
            } else if (character == '\n') {
                currentRow.add(currentField.toString());
                currentField.setLength(0);
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            } else {
                currentField.append(character);
            }
        }

        currentRow.add(currentField.toString());
        rows.add(currentRow);
        return rows;
    }

    private List<List<String>> readExcelRows(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<List<String>> rows = new ArrayList<>();
            int maxColumns = 0;

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow != null) {
                maxColumns = Math.max(maxColumns, headerRow.getLastCellNum());
            }

            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                int columnCount = Math.max(maxColumns, row != null ? row.getLastCellNum() : 0);
                List<String> values = new ArrayList<>();
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    Cell cell = row == null ? null : row.getCell(columnIndex);
                    values.add(readExcelCell(cell, formatter, evaluator));
                }
                rows.add(values);
            }
            return rows;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("File Excel không hợp lệ hoặc đang bị lỗi dữ liệu.");
        }
    }

    private String readExcelCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        try {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate().format(IMPORT_DATE_FORMATTER);
            }
            return formatter.formatCellValue(cell, evaluator);
        } catch (RuntimeException exception) {
            return formatter.formatCellValue(cell);
        }
    }

    private ImportReferenceContext loadImportReferenceContext() {
        List<EsportsTeam> teams = esportsTeamRepository.findAll();
        List<Hero> heroes = heroRepository.findAllByOrderByNameAsc();
        List<EsportsTournament> tournaments = esportsTournamentRepository.findAll();
        List<EsportsMatch> matches = esportsMatchRepository.findAllByOrderByMatchDateAscIdAsc();

        Map<Long, Hero> heroesById = new LinkedHashMap<>();
        heroes.forEach(hero -> heroesById.put(hero.getId(), hero));
        return new ImportReferenceContext(
                teams,
                heroes,
                tournaments,
                matches,
                buildTeamLookup(teams),
                buildHeroLookup(heroes),
                buildTournamentLookup(tournaments),
                heroesById
        );
    }

    private Map<String, List<EsportsTeam>> buildTeamLookup(List<EsportsTeam> teams) {
        Map<String, List<EsportsTeam>> lookup = new LinkedHashMap<>();
        for (EsportsTeam team : teams) {
            addLookupValue(lookup, normalizeLookupValue(team.getTeamCode()), team);
            addLookupValue(lookup, normalizeLookupValue(team.getTeamName()), team);
        }
        return lookup;
    }

    private Map<String, List<Hero>> buildHeroLookup(List<Hero> heroes) {
        Map<String, List<Hero>> lookup = new LinkedHashMap<>();
        for (Hero hero : heroes) {
            addLookupValue(lookup, normalizeLookupValue(hero.getName()), hero);
            addLookupValue(lookup, normalizeLookupValue(hero.getSlug()), hero);
        }
        return lookup;
    }

    private Map<String, List<EsportsTournament>> buildTournamentLookup(List<EsportsTournament> tournaments) {
        Map<String, List<EsportsTournament>> lookup = new LinkedHashMap<>();
        for (EsportsTournament tournament : tournaments) {
            addLookupValue(lookup, normalizeLookupValue(tournament.getName()), tournament);
            addLookupValue(lookup, normalizeLookupValue(tournament.getSlug()), tournament);
        }
        return lookup;
    }

    private <T> void addLookupValue(Map<String, List<T>> lookup, String key, T value) {
        if (!StringUtils.hasText(key) || value == null) {
            return;
        }
        List<T> bucket = lookup.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!bucket.contains(value)) {
            bucket.add(value);
        }
    }

    private MutableImportRow parseImportRow(ImportedSheetRow sourceRow,
                                            ImportReferenceContext context,
                                            boolean overwriteExisting,
                                            String importSource) {
        String dateText = sourceRow.cells.get("Date");
        String tournamentText = sourceRow.cells.get("Tournament");
        String stageText = sourceRow.cells.getOrDefault("Stage", "");
        String team1Text = sourceRow.cells.get("Team_1");
        String team2Text = sourceRow.cells.get("Team_2");
        String t1SideText = sourceRow.cells.get("T1_Side");
        String t2SideText = sourceRow.cells.get("T2_Side");
        String winnerText = sourceRow.cells.get("Winner");
        String lengthText = sourceRow.cells.get("Length");
        String matchText = sourceRow.cells.get("Match");

        MutableImportRow row = new MutableImportRow(sourceRow.rowNumber, dateText, tournamentText, team1Text, team2Text);

        row.matchDate = parseImportDate(dateText, row.errors);
        row.matchStage = parseImportStage(stageText, row.errors);
        row.gameNumber = parseImportGameNumber(matchText, row.errors);
        row.durationSeconds = parseImportDuration(lengthText, row.errors);

        if (!StringUtils.hasText(team1Text) || !StringUtils.hasText(team2Text)) {
            row.errors.add("Team_1 va Team_2 la bat buoc.");
        }
        if (!StringUtils.hasText(t1SideText) || !StringUtils.hasText(t2SideText)) {
            row.errors.add("T1_Side va T2_Side la bat buoc.");
        }
        if (!StringUtils.hasText(tournamentText)) {
            row.errors.add("Tournament la bat buoc.");
        }

        row.tournament = resolveLookupValue(context.tournamentsByKey, tournamentText, "Tournament", row.errors);
        row.team1 = resolveLookupValue(context.teamsByKey, team1Text, "Team_1", row.errors);
        row.team2 = resolveLookupValue(context.teamsByKey, team2Text, "Team_2", row.errors);

        if (row.team1 != null && row.team2 != null && row.team1.getId().equals(row.team2.getId())) {
            row.errors.add("Team_1 và Team_2 không được map vào cùng một team.");
        }

        ImportSide t1Side = parseImportSide(t1SideText, "T1_Side", row.errors);
        ImportSide t2Side = parseImportSide(t2SideText, "T2_Side", row.errors);
        if (t1Side != null && t2Side != null && t1Side == t2Side) {
            row.errors.add("T1_Side và T2_Side phải đối nghịch Blue/Red.");
        }

        if (row.team1 != null && row.team2 != null && t1Side != null && t2Side != null && t1Side != t2Side) {
            if (t1Side == ImportSide.BLUE) {
                row.blueTeam = row.team1;
                row.redTeam = row.team2;
            } else {
                row.blueTeam = row.team2;
                row.redTeam = row.team1;
            }
        }

        row.winnerTeam = resolveWinnerTeamForImport(winnerText, row, row.errors);
        if (row.blueTeam != null
                && row.redTeam != null
                && row.matchDate != null
                && row.tournament != null
                && row.matchStage != null) {
            long lowerTeamId = Math.min(row.blueTeam.getId(), row.redTeam.getId());
            long higherTeamId = Math.max(row.blueTeam.getId(), row.redTeam.getId());
            row.groupKey = new SeriesGroupKey(
                    row.matchDate,
                    row.tournament.getId(),
                    row.matchStage,
                    lowerTeamId,
                    higherTeamId
            );
        }

        List<Long> blueBans = new ArrayList<>();
        List<Long> redBans = new ArrayList<>();
        for (int slot = 1; slot <= 5; slot++) {
            Long t1BanHeroId = resolveHeroId(context, sourceRow.cells.get("T1_Ban_" + slot), "T1_Ban_" + slot, row.errors);
            Long t2BanHeroId = resolveHeroId(context, sourceRow.cells.get("T2_Ban_" + slot), "T2_Ban_" + slot, row.errors);
            if (t1Side == ImportSide.BLUE) {
                blueBans.add(t1BanHeroId);
                redBans.add(t2BanHeroId);
            } else {
                blueBans.add(t2BanHeroId);
                redBans.add(t1BanHeroId);
            }
        }

        Long t1Dsl = resolveHeroId(context, sourceRow.cells.get("T1_DSL"), "T1_DSL", row.errors);
        Long t1Jgl = resolveHeroId(context, sourceRow.cells.get("T1_JGL"), "T1_JGL", row.errors);
        Long t1Mid = resolveHeroId(context, sourceRow.cells.get("T1_MID"), "T1_MID", row.errors);
        Long t1Adl = resolveHeroId(context, sourceRow.cells.get("T1_ADL"), "T1_ADL", row.errors);
        Long t1Sup = resolveHeroId(context, sourceRow.cells.get("T1_SUP"), "T1_SUP", row.errors);
        Long t2Dsl = resolveHeroId(context, sourceRow.cells.get("T2_DSL"), "T2_DSL", row.errors);
        Long t2Jgl = resolveHeroId(context, sourceRow.cells.get("T2_JGL"), "T2_JGL", row.errors);
        Long t2Mid = resolveHeroId(context, sourceRow.cells.get("T2_MID"), "T2_MID", row.errors);
        Long t2Adl = resolveHeroId(context, sourceRow.cells.get("T2_ADL"), "T2_ADL", row.errors);
        Long t2Sup = resolveHeroId(context, sourceRow.cells.get("T2_SUP"), "T2_SUP", row.errors);

        if (t1Side == ImportSide.BLUE) {
            row.blueLineup = new LineupSlots(t1Dsl, t1Jgl, t1Mid, t1Adl, t1Sup);
            row.redLineup = new LineupSlots(t2Dsl, t2Jgl, t2Mid, t2Adl, t2Sup);
        } else if (t1Side == ImportSide.RED) {
            row.blueLineup = new LineupSlots(t2Dsl, t2Jgl, t2Mid, t2Adl, t2Sup);
            row.redLineup = new LineupSlots(t1Dsl, t1Jgl, t1Mid, t1Adl, t1Sup);
        }

        if (row.blueLineup != null && row.redLineup != null) {
            validateImportRowDuplicateHeroes(context, row, blueBans, redBans, row.blueLineup, row.redLineup);
        }

        if (!row.hasErrors() && row.blueTeam != null && row.redTeam != null && row.blueLineup != null && row.redLineup != null) {
            row.draftRequest = new EsportsGameDraftRequest(
                    row.gameNumber,
                    row.blueTeam.getId(),
                    row.redTeam.getId(),
                    row.winnerTeam != null ? row.winnerTeam.getId() : null,
                    row.durationSeconds,
                    IMPORT_DRAFT_FORMAT_CODE,
                    importSource,
                    blueBans,
                    redBans,
                    new EsportsGameDraftRequest.LineupRequest(
                            row.blueLineup.dsl(),
                            row.blueLineup.jgl(),
                            row.blueLineup.mid(),
                            row.blueLineup.adl(),
                            row.blueLineup.sup()
                    ),
                    new EsportsGameDraftRequest.LineupRequest(
                            row.redLineup.dsl(),
                            row.redLineup.jgl(),
                            row.redLineup.mid(),
                            row.redLineup.adl(),
                            row.redLineup.sup()
                    )
            );
        }

        row.durationText = formatDurationText(row.durationSeconds);
        if (overwriteExisting) {
            row.warnings.add("Preview đang bật overwrite duplicate nếu match/game đã tồn tại.");
        }
        return row;
    }

    private void validateImportRowDuplicateHeroes(ImportReferenceContext context,
                                                  MutableImportRow row,
                                                  List<Long> blueBans,
                                                  List<Long> redBans,
                                                  LineupSlots blueLineup,
                                                  LineupSlots redLineup) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        List<Long> allHeroIds = new ArrayList<>();
        allHeroIds.addAll(blueBans);
        allHeroIds.addAll(redBans);
        allHeroIds.addAll(blueLineup.values());
        allHeroIds.addAll(redLineup.values());
        for (Long heroId : allHeroIds) {
            if (heroId == null) {
                continue;
            }
            counts.merge(heroId, 1, Integer::sum);
        }

        List<String> duplicateHeroNames = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> {
                    Hero hero = context.heroesById.get(entry.getKey());
                    return hero != null && StringUtils.hasText(hero.getName())
                            ? hero.getName()
                            : "Hero #" + entry.getKey();
                })
                .toList();
        if (!duplicateHeroNames.isEmpty()) {
            row.errors.add("Không được trùng hero trong cùng 1 game: " + String.join(", ", duplicateHeroNames) + ".");
        }
    }

    private void markDuplicateRowsInsidePreview(Map<SeriesGroupKey, SeriesGroupAccumulator> groups) {
        for (SeriesGroupAccumulator group : groups.values()) {
            Map<Integer, List<MutableImportRow>> rowsByGameNumber = new LinkedHashMap<>();
            for (MutableImportRow row : group.rows) {
                if (row.gameNumber != null) {
                    rowsByGameNumber.computeIfAbsent(row.gameNumber, ignored -> new ArrayList<>()).add(row);
                }
            }
            for (Map.Entry<Integer, List<MutableImportRow>> entry : rowsByGameNumber.entrySet()) {
                if (entry.getValue().size() <= 1) {
                    continue;
                }
                for (MutableImportRow row : entry.getValue()) {
                    row.errors.add("File dang trung game_number " + entry.getKey() + " trong cung mot series.");
                }
            }
        }
    }

    private ResolvedGroupPlan resolveGroupPlan(SeriesGroupAccumulator group,
                                               ImportReferenceContext context,
                                               boolean overwriteExisting,
                                               int newMatchIndex) {
        List<MutableImportRow> validRows = group.rows.stream()
                .filter(row -> !row.hasErrors())
                .sorted(Comparator.comparing(row -> row.gameNumber))
                .toList();
        if (validRows.isEmpty()) {
            return new ResolvedGroupPlan(
                    null,
                    false,
                    false,
                    false,
                    null,
                    group.matchStage,
                    group.tournament,
                    group.matchTeam1,
                    group.matchTeam2,
                    null,
                    0,
                    0,
                    List.of()
            );
        }

        boolean anyMissingWinner = validRows.stream().anyMatch(row -> row.winnerTeam == null);
        Map<Long, List<EsportsGameDraft>> draftCache = new LinkedHashMap<>();
        List<EsportsMatch> sameDateSameStagePairMatches = context.matches.stream()
                .filter(match -> match.getMatchDate() != null && match.getMatchDate().toLocalDate().equals(group.matchDate))
                .filter(match -> matchesSameStage(match, group.matchStage))
                .filter(match -> matchesSameTeamPair(match, group))
                .toList();

        List<EsportsMatch> exactTournamentMatches = sameDateSameStagePairMatches.stream()
                .filter(match -> match.getTournament() != null
                        && group.tournament != null
                        && match.getTournament().getId() != null
                        && match.getTournament().getId().equals(group.tournament.getId()))
                .toList();
        List<EsportsMatch> nullTournamentMatches = sameDateSameStagePairMatches.stream()
                .filter(match -> match.getTournament() == null)
                .toList();
        List<EsportsMatch> mismatchedTournamentMatches = sameDateSameStagePairMatches.stream()
                .filter(match -> match.getTournament() != null
                        && (group.tournament == null
                        || !match.getTournament().getId().equals(group.tournament.getId())))
                .toList();

        List<EsportsMatch> exactScoreTournamentMatches = anyMissingWinner
                ? List.of()
                : exactTournamentMatches.stream()
                .filter(match -> matchesSameSeriesScore(match, validRows))
                .toList();

        EsportsMatch targetMatch = null;
        if (!exactScoreTournamentMatches.isEmpty()) {
            targetMatch = selectCanonicalParentMatch(exactScoreTournamentMatches, draftCache);
            if (exactScoreTournamentMatches.size() > 1 && targetMatch != null) {
                Long canonicalMatchId = targetMatch.getId();
                String duplicateIds = exactScoreTournamentMatches.stream()
                        .map(match -> "#" + match.getId())
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                validRows.forEach(row -> row.warnings.add(
                        "Tìm thấy nhiều esports_matches exact cho series này (" + duplicateIds
                                + "). Preview se uu tien match #" + canonicalMatchId
                                + " theo uu tien parent da co draft, sau do id nho hon."
                ));
            }
        }

        if (targetMatch == null && exactTournamentMatches.size() > 1) {
            group.rows.forEach(row -> row.errors.add("Tìm thấy nhiều esports_matches cùng ngày/cặp đội/tournament. Hãy xử lý match cha trước khi import."));
            return new ResolvedGroupPlan(
                    null,
                    false,
                    false,
                    false,
                    null,
                    group.matchStage,
                    group.tournament,
                    group.matchTeam1,
                    group.matchTeam2,
                    null,
                    0,
                    0,
                    List.of()
            );
        }
        if (targetMatch == null && exactTournamentMatches.isEmpty() && nullTournamentMatches.size() > 1) {
            group.rows.forEach(row -> row.errors.add("Tìm thấy nhiều esports_matches cùng ngày/cặp đội nhưng chưa gán tournament. Không thể tự đoán match cha."));
            return new ResolvedGroupPlan(
                    null,
                    false,
                    false,
                    false,
                    null,
                    group.matchStage,
                    group.tournament,
                    group.matchTeam1,
                    group.matchTeam2,
                    null,
                    0,
                    0,
                    List.of()
            );
        }
        if (targetMatch == null && exactTournamentMatches.isEmpty() && nullTournamentMatches.isEmpty() && !mismatchedTournamentMatches.isEmpty()) {
            group.rows.forEach(row -> row.errors.add("Đã có esports_matches cùng ngày/cặp đội nhưng tournament không khớp. Hãy cập nhật match cha trước khi import."));
            return new ResolvedGroupPlan(
                    null,
                    false,
                    false,
                    false,
                    null,
                    group.matchStage,
                    group.tournament,
                    group.matchTeam1,
                    group.matchTeam2,
                    null,
                    0,
                    0,
                    List.of()
            );
        }

        if (targetMatch == null) {
            targetMatch = !exactTournamentMatches.isEmpty()
                    ? exactTournamentMatches.get(0)
                    : (nullTournamentMatches.isEmpty() ? null : nullTournamentMatches.get(0));
        }
        EsportsMatch resolvedTargetMatch = targetMatch;
        boolean updateTournamentLink = resolvedTargetMatch != null
                && resolvedTargetMatch.getTournament() == null
                && group.tournament != null;

        int seriesScore1;
        int seriesScore2;
        if (resolvedTargetMatch != null) {
            seriesScore1 = countSeriesWins(validRows, resolvedTargetMatch.getTeam1Code());
            seriesScore2 = countSeriesWins(validRows, resolvedTargetMatch.getTeam2Code());
        } else {
            seriesScore1 = countSeriesWins(validRows, group.matchTeam1.getTeamCode());
            seriesScore2 = countSeriesWins(validRows, group.matchTeam2.getTeamCode());
        }

        boolean updateSeriesScore = resolvedTargetMatch != null
                && !anyMissingWinner
                && (resolvedTargetMatch.getScore1() == null || resolvedTargetMatch.getScore2() == null
                || resolvedTargetMatch.getScore1() != seriesScore1 || resolvedTargetMatch.getScore2() != seriesScore2);

        if (resolvedTargetMatch == null && anyMissingWinner) {
            validRows.forEach(row -> row.warnings.add("Series mới sẽ được tạo với tỷ số suy ra từ Winner hiện có; nếu cột Winner còn thiếu thì score series có thể chưa đầy đủ."));
        }
        if (resolvedTargetMatch != null && updateSeriesScore) {
            validRows.forEach(row -> row.warnings.add("Match #" + resolvedTargetMatch.getId() + " sẽ được cập nhật tỷ số series từ file import."));
        }
        if (updateTournamentLink && resolvedTargetMatch != null) {
            validRows.forEach(row -> row.warnings.add("Match #" + resolvedTargetMatch.getId() + " sẽ được gán tournament chính thức từ file import."));
        }

        for (MutableImportRow row : validRows) {
            row.matchId = resolvedTargetMatch != null ? resolvedTargetMatch.getId() : null;
            row.matchLabel = resolvedTargetMatch != null
                    ? "Match #" + resolvedTargetMatch.getId()
                    : "Match moi";
            row.matchAction = resolvedTargetMatch == null
                    ? "Tao match moi"
                    : (updateTournamentLink || updateSeriesScore
                    ? "Cập nhật match #" + resolvedTargetMatch.getId()
                    : "Dùng match #" + resolvedTargetMatch.getId());

            if (resolvedTargetMatch != null && esportsGameDraftRepository.existsByMatchIdAndGameNumber(resolvedTargetMatch.getId(), row.gameNumber)) {
                EsportsGameDraft existingDraft = esportsGameDraftRepository.findByMatchId(resolvedTargetMatch.getId()).stream()
                        .filter(item -> item.getGameNumber() != null && item.getGameNumber().equals(row.gameNumber))
                        .findFirst()
                        .orElse(null);
                if (!overwriteExisting) {
                    row.errors.add("Match #" + resolvedTargetMatch.getId() + " đã có game_number " + row.gameNumber + ". Bật overwrite nếu muốn ghi đè.");
                } else if (existingDraft != null) {
                    row.overwriteDraftId = existingDraft.getId();
                    row.draftAction = "Overwrite game draft #" + existingDraft.getId();
                }
            } else {
                row.draftAction = "Tao game " + row.gameNumber;
            }
        }

        LocalDateTime newMatchDateTime = targetMatch == null
                ? group.matchDate.atTime(12, 0).plusMinutes(newMatchIndex)
                : null;

        return new ResolvedGroupPlan(
                resolvedTargetMatch != null ? resolvedTargetMatch.getId() : null,
                resolvedTargetMatch == null,
                updateTournamentLink,
                updateSeriesScore,
                newMatchDateTime,
                group.matchStage,
                group.tournament,
                group.matchTeam1,
                group.matchTeam2,
                resolvedTargetMatch,
                seriesScore1,
                seriesScore2,
                validRows
        );
    }

    private EsportsMatch selectCanonicalParentMatch(List<EsportsMatch> matches,
                                                    Map<Long, List<EsportsGameDraft>> draftCache) {
        return matches.stream()
                .sorted(Comparator
                        .comparing((EsportsMatch match) -> hasExistingDraft(match, draftCache) ? 0 : 1)
                        .thenComparing(match -> match.getId() == null ? Long.MAX_VALUE : match.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean hasExistingDraft(EsportsMatch match,
                                     Map<Long, List<EsportsGameDraft>> draftCache) {
        return !loadDraftsForMatch(match != null ? match.getId() : null, draftCache).isEmpty();
    }

    private List<EsportsGameDraft> loadDraftsForMatch(Long matchId,
                                                      Map<Long, List<EsportsGameDraft>> draftCache) {
        if (matchId == null) {
            return List.of();
        }
        return draftCache.computeIfAbsent(matchId, ignored -> {
            List<EsportsGameDraft> drafts = esportsGameDraftRepository.findByMatchId(matchId);
            return drafts != null ? drafts : List.of();
        });
    }

    private int countSeriesWins(List<MutableImportRow> rows, String teamCode) {
        String normalizedTeamCode = normalizeCode(teamCode);
        int wins = 0;
        for (MutableImportRow row : rows) {
            if (row.winnerTeam != null && normalizedTeamCode.equals(normalizeCode(row.winnerTeam.getTeamCode()))) {
                wins++;
            }
        }
        return wins;
    }

    private boolean matchesSameSeriesScore(EsportsMatch match, List<MutableImportRow> rows) {
        if (match == null || match.getScore1() == null || match.getScore2() == null) {
            return false;
        }
        int expectedScore1 = countSeriesWins(rows, match.getTeam1Code());
        int expectedScore2 = countSeriesWins(rows, match.getTeam2Code());
        return match.getScore1() == expectedScore1 && match.getScore2() == expectedScore2;
    }

    private boolean matchesSameTeamPair(EsportsMatch match, SeriesGroupAccumulator group) {
        if (match == null || group == null || group.matchTeam1 == null || group.matchTeam2 == null) {
            return false;
        }
        String matchTeam1 = normalizeCode(match.getTeam1Code());
        String matchTeam2 = normalizeCode(match.getTeam2Code());
        String groupTeam1 = normalizeCode(group.matchTeam1.getTeamCode());
        String groupTeam2 = normalizeCode(group.matchTeam2.getTeamCode());
        return (matchTeam1.equals(groupTeam1) && matchTeam2.equals(groupTeam2))
                || (matchTeam1.equals(groupTeam2) && matchTeam2.equals(groupTeam1));
    }

    private boolean matchesSameStage(EsportsMatch match, String matchStage) {
        return normalizeImportStage(match != null ? match.getStage() : null)
                .equals(normalizeImportStage(matchStage));
    }

    private EsportsTeam resolveWinnerTeamForImport(String winnerText,
                                                   MutableImportRow row,
                                                   List<String> errors) {
        if (!StringUtils.hasText(winnerText)) {
            return null;
        }
        String normalizedWinner = normalizeLookupValue(winnerText);
        if ("blue".equals(normalizedWinner)) {
            if (row.blueTeam == null) {
                errors.add("Winner=Blue nhưng row này chưa map được Blue side.");
            }
            return row.blueTeam;
        }
        if ("red".equals(normalizedWinner)) {
            if (row.redTeam == null) {
                errors.add("Winner=Red nhưng row này chưa map được Red side.");
            }
            return row.redTeam;
        }
        if (row.team1 != null
                && (normalizedWinner.equals(normalizeLookupValue(row.team1.getTeamCode()))
                || normalizedWinner.equals(normalizeLookupValue(row.team1.getTeamName())))) {
            return row.team1;
        }
        if (row.team2 != null
                && (normalizedWinner.equals(normalizeLookupValue(row.team2.getTeamCode()))
                || normalizedWinner.equals(normalizeLookupValue(row.team2.getTeamName())))) {
            return row.team2;
        }
        errors.add("Winner không map được vào Team_1 hoặc Team_2.");
        return null;
    }

    private <T> T resolveLookupValue(Map<String, List<T>> lookup,
                                     String rawValue,
                                     String fieldName,
                                     List<String> errors) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        List<T> candidates = lookup.getOrDefault(normalizeLookupValue(rawValue), List.of());
        if (candidates.isEmpty()) {
            errors.add(fieldName + " không match dữ liệu hiện có: " + rawValue);
            return null;
        }
        if (candidates.size() > 1) {
            errors.add(fieldName + " đang match nhiều bản ghi, không thể tự đoán: " + rawValue);
            return null;
        }
        return candidates.get(0);
    }

    private Long resolveHeroId(ImportReferenceContext context,
                               String rawValue,
                               String fieldName,
                               List<String> errors) {
        Hero hero = resolveLookupValue(context.heroesByKey, rawValue, fieldName, errors);
        return hero != null ? hero.getId() : null;
    }

    private LocalDate parseImportDate(String rawValue, List<String> errors) {
        String cleaned = cleanCellText(rawValue);
        if (!StringUtils.hasText(cleaned)) {
            errors.add("Date la bat buoc.");
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("M/d/uuuu"),
                DateTimeFormatter.ofPattern("uuuu/M/d"),
                DateTimeFormatter.ofPattern("uuuu-M-d")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        errors.add("Date không hợp lệ: " + cleaned);
        return null;
    }

    private Integer parseImportGameNumber(String rawValue, List<String> errors) {
        String cleaned = cleanCellText(rawValue);
        if (!StringUtils.hasText(cleaned)) {
            errors.add("Match / game_number la bat buoc.");
            return null;
        }
        String candidate = cleaned.trim();
        if (candidate.toLowerCase(Locale.ROOT).startsWith("game")) {
            candidate = candidate.substring(4).trim();
        }
        try {
            int numeric = Integer.parseInt(candidate);
            if (numeric <= 0) {
                errors.add("Match / game_number phải lớn hơn 0.");
                return null;
            }
            return numeric;
        } catch (NumberFormatException exception) {
            errors.add("Match / game_number không hợp lệ: " + cleaned);
            return null;
        }
    }

    private Integer parseImportDuration(String rawValue, List<String> errors) {
        String cleaned = cleanCellText(rawValue);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        if (cleaned.matches("\\d+")) {
            return Integer.parseInt(cleaned);
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+):(\\d{1,2})$").matcher(cleaned);
        if (!matcher.matches()) {
            errors.add("Length phải có dạng mm:ss hoặc số giây.");
            return null;
        }
        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));
        if (seconds >= 60) {
            errors.add("Length mm:ss không hợp lệ: " + cleaned);
            return null;
        }
        return minutes * 60 + seconds;
    }

    private String parseImportStage(String stageText, List<String> errors) {
        if (!StringUtils.hasText(stageText)) {
            return DEFAULT_IMPORT_MATCH_STAGE;
        }
        return EsportsStageSupport.toCanonicalStage(stageText)
                .orElseGet(() -> {
                    errors.add("Stage không hợp lệ: " + cleanCellText(stageText));
                    return null;
                });
    }

    private ImportSide parseImportSide(String rawValue, String fieldName, List<String> errors) {
        String normalized = normalizeLookupValue(rawValue);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if ("blue".equals(normalized)) {
            return ImportSide.BLUE;
        }
        if ("red".equals(normalized)) {
            return ImportSide.RED;
        }
        errors.add(fieldName + " chi nhan Blue hoac Red.");
        return null;
    }

    private String resolveMatchTier(EsportsTournament tournament) {
        return EsportsTierSupport.resolveTournamentSnapshotTier(tournament);
    }

    private void pruneExpiredImportPreviewSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(IMPORT_PREVIEW_TTL_MINUTES);
        importPreviewSessions.entrySet().removeIf(entry -> entry.getValue().createdAt.isBefore(cutoff));
    }

    private EsportsGameDraftImportPreviewResponse.RowPreview toRowPreview(MutableImportRow row) {
        return new EsportsGameDraftImportPreviewResponse.RowPreview(
                row.rowNumber,
                row.matchId,
                row.matchLabel,
                row.gameNumber,
                row.matchDate != null ? row.matchDate.toString() : row.rawDate,
                row.tournament != null ? row.tournament.getName() : row.rawTournament,
                row.rawTeam1,
                row.rawTeam2,
                displayTeamName(row.blueTeam),
                displayTeamName(row.redTeam),
                displayTeamName(row.winnerTeam),
                row.durationText,
                row.matchAction,
                row.draftAction,
                distinctMessages(row.errors),
                distinctMessages(row.warnings)
        );
    }

    private List<String> distinctMessages(List<String> messages) {
        return messages.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalizeHeader(String header) {
        return cleanCellText(header).toUpperCase(Locale.ROOT);
    }

    private String normalizeImportStage(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_IMPORT_MATCH_STAGE;
        }
        return EsportsStageSupport.normalizeStageKey(value);
    }

    private String cleanCellText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "").trim();
    }

    private boolean isExcelFile(String filename) {
        String lowercase = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return lowercase.endsWith(".xlsx") || lowercase.endsWith(".xls");
    }

    private String normalizeLookupValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized;
    }

    private NormalizedDraftWrite normalizeDraftWrite(EsportsMatch match,
                                                     EsportsGameDraft existingDraft,
                                                     EsportsGameDraftRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("payload la bat buoc.");
        }

        int gameNumber = requirePositive(request.gameNumber(), "gameNumber");
        validateDurationSeconds(request.durationSeconds());

        EsportsTeam blueTeam = requireTeam(requireId(request.blueTeamId(), "blueTeamId"));
        EsportsTeam redTeam = requireTeam(requireId(request.redTeamId(), "redTeamId"));
        validateMatchTeams(match, blueTeam, redTeam);
        EsportsTeam winnerTeam = resolveWinnerTeam(request.winnerTeamId(), blueTeam, redTeam);

        if (blueTeam.getId().equals(redTeam.getId())) {
            throw new IllegalArgumentException("blueTeamId và redTeamId không được trùng nhau.");
        }

        Long existingId = existingDraft == null ? null : existingDraft.getId();
        if (existingId == null) {
            if (esportsGameDraftRepository.existsByMatchIdAndGameNumber(match.getId(), gameNumber)) {
                throw new IllegalArgumentException("gameNumber đã tồn tại trong match này.");
            }
        } else if (esportsGameDraftRepository.existsByMatchIdAndGameNumberAndIdNot(match.getId(), gameNumber, existingId)) {
            throw new IllegalArgumentException("gameNumber đã tồn tại trong match này.");
        }

        List<Long> blueBans = normalizeHeroSlots(request.blueBans(), "blueBans");
        List<Long> redBans = normalizeHeroSlots(request.redBans(), "redBans");
        LineupSlots blueLineup = normalizeLineup(request.blueLineup());
        LineupSlots redLineup = normalizeLineup(request.redLineup());

        Set<Long> referencedHeroIds = new LinkedHashSet<>();
        collectNonNullHeroIds(referencedHeroIds, blueBans);
        collectNonNullHeroIds(referencedHeroIds, redBans);
        collectNonNullHeroIds(referencedHeroIds, blueLineup.values());
        collectNonNullHeroIds(referencedHeroIds, redLineup.values());

        Map<Long, Hero> heroesById = loadHeroesById(referencedHeroIds);
        validateDuplicateHeroes(heroesById, blueBans, redBans, blueLineup, redLineup);

        return new NormalizedDraftWrite(
                match,
                gameNumber,
                blueTeam,
                redTeam,
                winnerTeam,
                request.durationSeconds(),
                normalizeDraftFormatCode(request.draftFormatCode()),
                normalizeSource(request.source()),
                blueBans,
                redBans,
                blueLineup,
                redLineup,
                serializeRawDraftJson(request),
                heroesById
        );
    }

    private void applyDraftWrite(EsportsGameDraft draft, NormalizedDraftWrite normalized) {
        draft.setMatch(normalized.match());
        draft.setGameNumber(normalized.gameNumber());
        draft.setBlueTeam(normalized.blueTeam());
        draft.setRedTeam(normalized.redTeam());
        draft.setWinnerTeam(normalized.winnerTeam());
        draft.setDurationSeconds(normalized.durationSeconds());
        draft.setDraftFormatCode(normalized.draftFormatCode());
        draft.setSource(normalized.source());
        draft.setRawDraftJson(normalized.rawDraftJson());

        applyBanSlots(draft, normalized.blueBans(), normalized.redBans());
        applyLineupSlots(draft, normalized.blueLineup(), normalized.redLineup());
    }

    private void applyBanSlots(EsportsGameDraft draft, List<Long> blueBans, List<Long> redBans) {
        draft.setBlueBan1HeroId(slotValue(blueBans, 0));
        draft.setBlueBan2HeroId(slotValue(blueBans, 1));
        draft.setBlueBan3HeroId(slotValue(blueBans, 2));
        draft.setBlueBan4HeroId(slotValue(blueBans, 3));
        draft.setBlueBan5HeroId(slotValue(blueBans, 4));

        draft.setRedBan1HeroId(slotValue(redBans, 0));
        draft.setRedBan2HeroId(slotValue(redBans, 1));
        draft.setRedBan3HeroId(slotValue(redBans, 2));
        draft.setRedBan4HeroId(slotValue(redBans, 3));
        draft.setRedBan5HeroId(slotValue(redBans, 4));
    }

    private void applyLineupSlots(EsportsGameDraft draft, LineupSlots blueLineup, LineupSlots redLineup) {
        draft.setBlueDslHeroId(blueLineup.dsl());
        draft.setBlueJglHeroId(blueLineup.jgl());
        draft.setBlueMidHeroId(blueLineup.mid());
        draft.setBlueAdlHeroId(blueLineup.adl());
        draft.setBlueSupHeroId(blueLineup.sup());

        draft.setRedDslHeroId(redLineup.dsl());
        draft.setRedJglHeroId(redLineup.jgl());
        draft.setRedMidHeroId(redLineup.mid());
        draft.setRedAdlHeroId(redLineup.adl());
        draft.setRedSupHeroId(redLineup.sup());
    }

    private DraftExportFilter resolveDraftExportFilter(Long tournamentId,
                                                       String tournamentName,
                                                       Long matchId,
                                                       LocalDate dateFrom,
                                                       LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("date range không hợp lệ.");
        }
        TournamentScope tournamentScope = resolveTournamentScope(tournamentId, tournamentName);
        return new DraftExportFilter(
                tournamentScope.tournamentId(),
                tournamentScope.tournamentTier(),
                matchId,
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? LocalDateTime.of(dateTo, LocalTime.MAX) : null
        );
    }

    private EsportsGameDraftResponse toGameDraftResponse(EsportsGameDraft draft, Map<Long, Hero> heroesById) {
        return new EsportsGameDraftResponse(
                draft.getId(),
                draft.getMatch().getId(),
                draft.getGameNumber(),
                toTeamSummary(draft.getBlueTeam()),
                toTeamSummary(draft.getRedTeam()),
                toTeamSummary(draft.getWinnerTeam()),
                draft.getDurationSeconds(),
                formatDurationText(draft.getDurationSeconds()),
                normalizeDraftFormatCode(draft.getDraftFormatCode()),
                draft.getSource(),
                toHeroSummarySlots(heroesById, blueBanSlots(draft)),
                toHeroSummarySlots(heroesById, redBanSlots(draft)),
                toLineupMap(heroesById, blueLineupSlots(draft)),
                toLineupMap(heroesById, redLineupSlots(draft)),
                buildDraftCompleteness(draft),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }

    private EsportsGameDraftResponse.TeamSummary toTeamSummary(EsportsTeam team) {
        if (team == null) {
            return null;
        }
        return new EsportsGameDraftResponse.TeamSummary(
                team.getId(),
                team.getTeamCode(),
                displayTeamName(team),
                team.getLogoUrl()
        );
    }

    private List<EsportsGameDraftResponse.HeroSummary> toHeroSummarySlots(Map<Long, Hero> heroesById, List<Long> heroIds) {
        List<EsportsGameDraftResponse.HeroSummary> items = new ArrayList<>();
        for (Long heroId : heroIds) {
            items.add(toHeroSummary(heroesById.get(heroId)));
        }
        return items;
    }

    private Map<String, EsportsGameDraftResponse.HeroSummary> toLineupMap(Map<Long, Hero> heroesById,
                                                                          Map<String, Long> lineupSlots) {
        Map<String, EsportsGameDraftResponse.HeroSummary> mapped = new LinkedHashMap<>();
        lineupSlots.forEach((lane, heroId) -> mapped.put(lane, toHeroSummary(heroesById.get(heroId))));
        return mapped;
    }

    private EsportsGameDraftResponse.HeroSummary toHeroSummary(Hero hero) {
        if (hero == null) {
            return null;
        }
        return new EsportsGameDraftResponse.HeroSummary(
                hero.getId(),
                hero.getName(),
                hero.getSlug(),
                hero.getAvatarUrl()
        );
    }

    private EsportsGameDraftResponse.DraftCompleteness buildDraftCompleteness(EsportsGameDraft draft) {
        int banCount = countNonNull(blueBanSlots(draft)) + countNonNull(redBanSlots(draft));
        int pickCount = countNonNull(blueLineupSlots(draft).values()) + countNonNull(redLineupSlots(draft).values());
        boolean complete = banCount >= EXPECTED_COMPLETE_BANS
                && pickCount == EXPECTED_COMPLETE_PICKS
                && draft.getWinnerTeam() != null;

        List<String> missingFields = new ArrayList<>();
        if (banCount < EXPECTED_COMPLETE_BANS) {
            missingFields.add("bans");
        }
        if (pickCount < EXPECTED_COMPLETE_PICKS) {
            missingFields.add("lineup");
        }
        if (draft.getWinnerTeam() == null) {
            missingFields.add("winner");
        }

        String status;
        if (complete) {
            status = "Complete";
        } else if (banCount < EXPECTED_COMPLETE_BANS && pickCount < EXPECTED_COMPLETE_PICKS) {
            status = "Missing bans + lineup";
        } else if (pickCount < EXPECTED_COMPLETE_PICKS) {
            status = "Missing lineup";
        } else if (banCount < EXPECTED_COMPLETE_BANS) {
            status = "Missing bans";
        } else if (draft.getWinnerTeam() == null) {
            status = "Missing winner";
        } else {
            status = "Incomplete";
        }

        return new EsportsGameDraftResponse.DraftCompleteness(
                banCount,
                pickCount,
                complete,
                status,
                missingFields
        );
    }

    private String buildExportRow(EsportsGameDraft draft, Map<Long, Hero> heroesById) {
        return buildCsvLine(Arrays.asList(
                formatMatchDateForExport(draft.getMatch()),
                resolveTournamentLabelForExport(draft.getMatch()),
                draft.getGameNumber() == null ? "" : String.valueOf(draft.getGameNumber()),
                displayTeamName(draft.getBlueTeam()),
                "Blue",
                heroName(heroesById, draft.getBlueDslHeroId()),
                heroName(heroesById, draft.getBlueJglHeroId()),
                heroName(heroesById, draft.getBlueMidHeroId()),
                heroName(heroesById, draft.getBlueAdlHeroId()),
                heroName(heroesById, draft.getBlueSupHeroId()),
                heroName(heroesById, draft.getBlueBan1HeroId()),
                heroName(heroesById, draft.getBlueBan2HeroId()),
                heroName(heroesById, draft.getBlueBan3HeroId()),
                heroName(heroesById, draft.getBlueBan4HeroId()),
                heroName(heroesById, draft.getBlueBan5HeroId()),
                displayTeamName(draft.getRedTeam()),
                "Red",
                heroName(heroesById, draft.getRedDslHeroId()),
                heroName(heroesById, draft.getRedJglHeroId()),
                heroName(heroesById, draft.getRedMidHeroId()),
                heroName(heroesById, draft.getRedAdlHeroId()),
                heroName(heroesById, draft.getRedSupHeroId()),
                heroName(heroesById, draft.getRedBan1HeroId()),
                heroName(heroesById, draft.getRedBan2HeroId()),
                heroName(heroesById, draft.getRedBan3HeroId()),
                heroName(heroesById, draft.getRedBan4HeroId()),
                heroName(heroesById, draft.getRedBan5HeroId()),
                resolveWinnerNameForExport(draft),
                formatDurationText(draft.getDurationSeconds())
        ));
    }

    private String buildCsvLine(List<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        values.forEach(value -> joiner.add(escapeCsv(value)));
        return joiner.toString();
    }

    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        String escaped = safeValue.replace("\"", "\"\"");
        if (safeValue.contains(",") || safeValue.contains("\"")
                || safeValue.contains("\n") || safeValue.contains("\r")
                || safeValue.contains("\t")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatMatchDateForExport(EsportsMatch match) {
        if (match == null || match.getMatchDate() == null) {
            return "";
        }
        return match.getMatchDate().toLocalDate().toString();
    }

    private String resolveTournamentLabelForExport(EsportsMatch match) {
        if (match == null) {
            return "";
        }
        if (match.getTournament() != null && StringUtils.hasText(match.getTournament().getName())) {
            return match.getTournament().getName().trim();
        }
        String rawTier = EsportsTierSupport.resolveEffectiveTier(match);
        String resolvedTier = EsportsTournamentCatalog.resolveTournamentTier(rawTier);
        if (resolvedTier != null) {
            return EsportsTournamentCatalog.resolveTournamentName(resolvedTier);
        }
        return rawTier;
    }

    private String heroName(Map<Long, Hero> heroesById, Long heroId) {
        Hero hero = heroesById.get(heroId);
        return hero != null && StringUtils.hasText(hero.getName()) ? hero.getName().trim() : "";
    }

    private String resolveWinnerNameForExport(EsportsGameDraft draft) {
        if (draft.getWinnerTeam() == null) {
            return "";
        }
        Long winnerId = draft.getWinnerTeam().getId();
        if (draft.getBlueTeam() != null && winnerId != null && winnerId.equals(draft.getBlueTeam().getId())) {
            return displayTeamName(draft.getBlueTeam());
        }
        if (draft.getRedTeam() != null && winnerId != null && winnerId.equals(draft.getRedTeam().getId())) {
            return displayTeamName(draft.getRedTeam());
        }
        return "";
    }

    private EsportsMatch requireMatch(Long matchId) {
        return esportsMatchRepository.findById(matchId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy esports match với ID: " + matchId));
    }

    private EsportsGameDraft requireGameDraft(Long gameDraftId) {
        return esportsGameDraftRepository.findById(gameDraftId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy esports game draft với ID: " + gameDraftId));
    }

    private EsportsTeam requireTeam(Long teamId) {
        return esportsTeamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy esports team với ID: " + teamId));
    }

    private Map<Long, Hero> loadHeroesById(Collection<Long> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Hero> heroesById = new LinkedHashMap<>();
        heroRepository.findAllById(heroIds)
                .forEach(hero -> heroesById.put(hero.getId(), hero));

        for (Long heroId : heroIds) {
            if (heroId != null && !heroesById.containsKey(heroId)) {
                throw new NoSuchElementException("Không tìm thấy hero với ID: " + heroId);
            }
        }
        return heroesById;
    }

    private Map<Long, Hero> loadHeroesForDraft(EsportsGameDraft draft) {
        Set<Long> heroIds = new LinkedHashSet<>();
        collectNonNullHeroIds(heroIds, blueBanSlots(draft));
        collectNonNullHeroIds(heroIds, redBanSlots(draft));
        collectNonNullHeroIds(heroIds, blueLineupSlots(draft).values());
        collectNonNullHeroIds(heroIds, redLineupSlots(draft).values());
        return loadHeroesById(heroIds);
    }

    private Map<Long, Hero> loadHeroesForDrafts(List<EsportsGameDraft> drafts) {
        Set<Long> heroIds = new LinkedHashSet<>();
        for (EsportsGameDraft draft : drafts) {
            collectNonNullHeroIds(heroIds, blueBanSlots(draft));
            collectNonNullHeroIds(heroIds, redBanSlots(draft));
            collectNonNullHeroIds(heroIds, blueLineupSlots(draft).values());
            collectNonNullHeroIds(heroIds, redLineupSlots(draft).values());
        }
        return loadHeroesById(heroIds);
    }

    private void validateDurationSeconds(Integer durationSeconds) {
        if (durationSeconds != null && durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds không được âm.");
        }
    }

    private void validateMatchTeams(EsportsMatch match, EsportsTeam blueTeam, EsportsTeam redTeam) {
        Set<String> matchTeamCodes = new LinkedHashSet<>();
        matchTeamCodes.add(normalizeCode(match.getTeam1Code()));
        matchTeamCodes.add(normalizeCode(match.getTeam2Code()));
        if (!matchTeamCodes.contains(normalizeCode(blueTeam.getTeamCode()))
                || !matchTeamCodes.contains(normalizeCode(redTeam.getTeamCode()))) {
            throw new IllegalArgumentException("blueTeamId và redTeamId phải thuộc đúng 2 đội của esports match.");
        }
    }

    private EsportsTeam resolveWinnerTeam(Long winnerTeamId, EsportsTeam blueTeam, EsportsTeam redTeam) {
        if (winnerTeamId == null) {
            return null;
        }

        EsportsTeam winnerTeam = requireTeam(winnerTeamId);
        if (!winnerTeam.getId().equals(blueTeam.getId()) && !winnerTeam.getId().equals(redTeam.getId())) {
            throw new IllegalArgumentException("winnerTeamId phải là blue team hoặc red team của game.");
        }
        return winnerTeam;
    }

    private List<Long> normalizeHeroSlots(List<Long> heroIds, String fieldName) {
        List<Long> slots = heroIds == null ? new ArrayList<>() : new ArrayList<>(heroIds);
        if (slots.size() > MAX_BANS_PER_SIDE) {
            throw new IllegalArgumentException(fieldName + " chỉ được tối đa 5 slot.");
        }
        while (slots.size() < MAX_BANS_PER_SIDE) {
            slots.add(null);
        }
        return slots;
    }

    private LineupSlots normalizeLineup(EsportsGameDraftRequest.LineupRequest request) {
        if (request == null) {
            return new LineupSlots(null, null, null, null, null);
        }
        return new LineupSlots(request.dsl(), request.jgl(), request.mid(), request.adl(), request.sup());
    }

    private void validateDuplicateHeroes(Map<Long, Hero> heroesById,
                                         List<Long> blueBans,
                                         List<Long> redBans,
                                         LineupSlots blueLineup,
                                         LineupSlots redLineup) {
        Map<Long, Integer> heroCounts = new LinkedHashMap<>();
        List<Long> orderedHeroIds = new ArrayList<>();
        orderedHeroIds.addAll(blueBans);
        orderedHeroIds.addAll(redBans);
        orderedHeroIds.addAll(blueLineup.values());
        orderedHeroIds.addAll(redLineup.values());

        for (Long heroId : orderedHeroIds) {
            if (heroId == null) {
                continue;
            }
            heroCounts.merge(heroId, 1, Integer::sum);
        }

        List<String> duplicateHeroNames = heroCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> {
                    Hero hero = heroesById.get(entry.getKey());
                    return hero != null && StringUtils.hasText(hero.getName())
                            ? hero.getName()
                            : "Hero #" + entry.getKey();
                })
                .toList();

        if (!duplicateHeroNames.isEmpty()) {
            throw new IllegalArgumentException("Không được trùng hero trong cùng game draft: "
                    + String.join(", ", duplicateHeroNames) + ".");
        }
    }

    private void collectNonNullHeroIds(Set<Long> destination, Collection<Long> heroIds) {
        if (heroIds == null) {
            return;
        }
        heroIds.stream()
                .filter(value -> value != null && value > 0)
                .forEach(destination::add);
    }

    private Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " la bat buoc.");
        }
        return value;
    }

    private int requirePositive(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " la bat buoc.");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
        }
        return value;
    }

    private TournamentScope resolveTournamentScope(Long tournamentId, String tournamentName) {
        if (tournamentId != null) {
            EsportsTournament tournament = esportsTournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("tournamentId không hợp lệ."));
            return new TournamentScope(tournament.getId(), null);
        }

        if (!StringUtils.hasText(tournamentName)) {
            return new TournamentScope(null, null);
        }

        String normalizedTournamentName = tournamentName.trim();
        Optional<EsportsTournament> officialTournament = esportsTournamentRepository.findByNameIgnoreCase(normalizedTournamentName)
                .or(() -> esportsTournamentRepository.findBySlugIgnoreCase(normalizedTournamentName));
        if (officialTournament.isPresent()) {
            return new TournamentScope(officialTournament.get().getId(), null);
        }

        String resolvedTier = EsportsTournamentCatalog.resolveTournamentTier(normalizedTournamentName);
        if (resolvedTier != null) {
            return new TournamentScope(null, resolvedTier);
        }

        for (EsportsDraftTournamentScopeAggregate aggregate : esportsGameDraftRepository.findDraftTournamentScopesOrderByLatestMatchDesc()) {
            if (aggregate.tournamentId() != null) {
                continue;
            }
            String legacyTier = aggregate.tournamentTier() == null ? "" : aggregate.tournamentTier().trim();
            String legacyLabel = resolveLegacyTournamentLabel(aggregate.tournamentTier());
            if (legacyTier.equalsIgnoreCase(normalizedTournamentName)
                    || legacyLabel.equalsIgnoreCase(normalizedTournamentName)) {
                return new TournamentScope(null, legacyTier);
            }
        }

        throw new IllegalArgumentException("tournamentName không hợp lệ.");
    }

    private String resolveLegacyTournamentLabel(String tournamentTier) {
        if (!StringUtils.hasText(tournamentTier)) {
            return "";
        }
        return EsportsTournamentCatalog.resolveTournamentName(tournamentTier.trim());
    }

    private String normalizeDraftFormatCode(String draftFormatCode) {
        return StringUtils.hasText(draftFormatCode)
                ? draftFormatCode.trim().toUpperCase(Locale.ROOT)
                : EsportsDraftDefaults.DEFAULT_FORMAT_CODE;
    }

    private String normalizeSource(String source) {
        return StringUtils.hasText(source) ? source.trim() : "manual";
    }

    private String serializeRawDraftJson(EsportsGameDraftRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Không thể serialize raw_draft_json.");
        }
    }

    private List<Long> blueBanSlots(EsportsGameDraft draft) {
        return Arrays.asList(
                draft.getBlueBan1HeroId(),
                draft.getBlueBan2HeroId(),
                draft.getBlueBan3HeroId(),
                draft.getBlueBan4HeroId(),
                draft.getBlueBan5HeroId()
        );
    }

    private List<Long> redBanSlots(EsportsGameDraft draft) {
        return Arrays.asList(
                draft.getRedBan1HeroId(),
                draft.getRedBan2HeroId(),
                draft.getRedBan3HeroId(),
                draft.getRedBan4HeroId(),
                draft.getRedBan5HeroId()
        );
    }

    private Map<String, Long> blueLineupSlots(EsportsGameDraft draft) {
        return lineupMap(
                draft.getBlueDslHeroId(),
                draft.getBlueJglHeroId(),
                draft.getBlueMidHeroId(),
                draft.getBlueAdlHeroId(),
                draft.getBlueSupHeroId()
        );
    }

    private Map<String, Long> redLineupSlots(EsportsGameDraft draft) {
        return lineupMap(
                draft.getRedDslHeroId(),
                draft.getRedJglHeroId(),
                draft.getRedMidHeroId(),
                draft.getRedAdlHeroId(),
                draft.getRedSupHeroId()
        );
    }

    private Map<String, Long> lineupMap(Long dsl, Long jgl, Long mid, Long adl, Long sup) {
        Map<String, Long> lineup = new LinkedHashMap<>();
        lineup.put(EsportsLineupLaneRole.DSL.name(), dsl);
        lineup.put(EsportsLineupLaneRole.JGL.name(), jgl);
        lineup.put(EsportsLineupLaneRole.MID.name(), mid);
        lineup.put(EsportsLineupLaneRole.ADL.name(), adl);
        lineup.put(EsportsLineupLaneRole.SUP.name(), sup);
        return lineup;
    }

    private int countNonNull(Collection<Long> heroIds) {
        if (heroIds == null) {
            return 0;
        }
        return (int) heroIds.stream().filter(value -> value != null && value > 0).count();
    }

    private Long slotValue(List<Long> slots, int index) {
        return index >= 0 && index < slots.size() ? slots.get(index) : null;
    }

    private String formatDurationText(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds < 0) {
            return null;
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String displayTeamName(EsportsTeam team) {
        if (team == null) {
            return "";
        }
        if (StringUtils.hasText(team.getTeamName())) {
            return team.getTeamName().trim();
        }
        return team.getTeamCode();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private record LineupSlots(
            Long dsl,
            Long jgl,
            Long mid,
            Long adl,
            Long sup
    ) {
        private List<Long> values() {
            return Arrays.asList(dsl, jgl, mid, adl, sup);
        }
    }

    private record NormalizedDraftWrite(
            EsportsMatch match,
            int gameNumber,
            EsportsTeam blueTeam,
            EsportsTeam redTeam,
            EsportsTeam winnerTeam,
            Integer durationSeconds,
            String draftFormatCode,
            String source,
            List<Long> blueBans,
            List<Long> redBans,
            LineupSlots blueLineup,
            LineupSlots redLineup,
            String rawDraftJson,
            Map<Long, Hero> heroesById
    ) {
    }

    private record DraftExportFilter(
            Long tournamentId,
            String tournamentTier,
            Long matchId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
    }

    private record TournamentScope(
            Long tournamentId,
            String tournamentTier
    ) {
    }

    private enum ImportSide {
        BLUE,
        RED
    }

    private static final class ImportedSheet {
        private final String filename;
        private final String sourceTag;
        private final List<ImportedSheetRow> rows;

        private ImportedSheet(String filename, String sourceTag, List<ImportedSheetRow> rows) {
            this.filename = filename;
            this.sourceTag = sourceTag;
            this.rows = rows;
        }
    }

    private static final class ImportedSheetRow {
        private final int rowNumber;
        private final Map<String, String> cells;

        private ImportedSheetRow(int rowNumber, Map<String, String> cells) {
            this.rowNumber = rowNumber;
            this.cells = cells;
        }
    }

    private static final class ImportReferenceContext {
        private final List<EsportsTeam> teams;
        private final List<Hero> heroes;
        private final List<EsportsTournament> tournaments;
        private final List<EsportsMatch> matches;
        private final Map<String, List<EsportsTeam>> teamsByKey;
        private final Map<String, List<Hero>> heroesByKey;
        private final Map<String, List<EsportsTournament>> tournamentsByKey;
        private final Map<Long, Hero> heroesById;

        private ImportReferenceContext(List<EsportsTeam> teams,
                                       List<Hero> heroes,
                                       List<EsportsTournament> tournaments,
                                       List<EsportsMatch> matches,
                                       Map<String, List<EsportsTeam>> teamsByKey,
                                       Map<String, List<Hero>> heroesByKey,
                                       Map<String, List<EsportsTournament>> tournamentsByKey,
                                       Map<Long, Hero> heroesById) {
            this.teams = teams;
            this.heroes = heroes;
            this.tournaments = tournaments;
            this.matches = matches;
            this.teamsByKey = teamsByKey;
            this.heroesByKey = heroesByKey;
            this.tournamentsByKey = tournamentsByKey;
            this.heroesById = heroesById;
        }
    }

    private record SeriesGroupKey(LocalDate matchDate,
                                  Long tournamentId,
                                  String matchStage,
                                  long lowerTeamId,
                                  long higherTeamId) {
    }

    private static final class SeriesGroupAccumulator {
        private final SeriesGroupKey groupKey;
        private final LocalDate matchDate;
        private final String matchStage;
        private final EsportsTournament tournament;
        private final EsportsTeam matchTeam1;
        private final EsportsTeam matchTeam2;
        private final List<MutableImportRow> rows = new ArrayList<>();

        private SeriesGroupAccumulator(SeriesGroupKey groupKey,
                                       LocalDate matchDate,
                                       String matchStage,
                                       EsportsTournament tournament,
                                       EsportsTeam matchTeam1,
                                       EsportsTeam matchTeam2) {
            this.groupKey = groupKey;
            this.matchDate = matchDate;
            this.matchStage = matchStage;
            this.tournament = tournament;
            this.matchTeam1 = matchTeam1;
            this.matchTeam2 = matchTeam2;
        }
    }

    private static final class ResolvedGroupPlan {
        private final Long matchId;
        private final boolean createMatch;
        private final boolean updateTournamentLink;
        private final boolean updateSeriesScore;
        private final LocalDateTime newMatchDateTime;
        private final String matchStage;
        private final EsportsTournament tournament;
        private final EsportsTeam matchTeam1;
        private final EsportsTeam matchTeam2;
        private final EsportsMatch existingMatch;
        private final int seriesScore1;
        private final int seriesScore2;
        private final List<MutableImportRow> rows;

        private ResolvedGroupPlan(Long matchId,
                                  boolean createMatch,
                                  boolean updateTournamentLink,
                                  boolean updateSeriesScore,
                                  LocalDateTime newMatchDateTime,
                                  String matchStage,
                                  EsportsTournament tournament,
                                  EsportsTeam matchTeam1,
                                  EsportsTeam matchTeam2,
                                  EsportsMatch existingMatch,
                                  int seriesScore1,
                                  int seriesScore2,
                                  List<MutableImportRow> rows) {
            this.matchId = matchId;
            this.createMatch = createMatch;
            this.updateTournamentLink = updateTournamentLink;
            this.updateSeriesScore = updateSeriesScore;
            this.newMatchDateTime = newMatchDateTime;
            this.matchStage = matchStage;
            this.tournament = tournament;
            this.matchTeam1 = matchTeam1;
            this.matchTeam2 = matchTeam2;
            this.existingMatch = existingMatch;
            this.seriesScore1 = seriesScore1;
            this.seriesScore2 = seriesScore2;
            this.rows = rows;
        }
    }

    private static final class ImportPreviewSession {
        private final String token;
        private final LocalDateTime createdAt;
        private final List<ResolvedGroupPlan> groupPlans;
        private final boolean readyToImport;

        private ImportPreviewSession(String token,
                                     LocalDateTime createdAt,
                                     List<ResolvedGroupPlan> groupPlans,
                                     boolean readyToImport) {
            this.token = token;
            this.createdAt = createdAt;
            this.groupPlans = groupPlans;
            this.readyToImport = readyToImport;
        }
    }

    private static final class MutableImportRow {
        private final int rowNumber;
        private final String rawDate;
        private final String rawTournament;
        private final String rawTeam1;
        private final String rawTeam2;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        private LocalDate matchDate;
        private String matchStage;
        private EsportsTournament tournament;
        private EsportsTeam team1;
        private EsportsTeam team2;
        private EsportsTeam blueTeam;
        private EsportsTeam redTeam;
        private EsportsTeam winnerTeam;
        private Integer gameNumber;
        private Integer durationSeconds;
        private String durationText;
        private LineupSlots blueLineup;
        private LineupSlots redLineup;
        private SeriesGroupKey groupKey;
        private EsportsGameDraftRequest draftRequest;
        private Long matchId;
        private String matchLabel;
        private String matchAction;
        private String draftAction;
        private Long overwriteDraftId;

        private MutableImportRow(int rowNumber,
                                 String rawDate,
                                 String rawTournament,
                                 String rawTeam1,
                                 String rawTeam2) {
            this.rowNumber = rowNumber;
            this.rawDate = rawDate;
            this.rawTournament = rawTournament;
            this.rawTeam1 = rawTeam1;
            this.rawTeam2 = rawTeam2;
        }

        private boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
