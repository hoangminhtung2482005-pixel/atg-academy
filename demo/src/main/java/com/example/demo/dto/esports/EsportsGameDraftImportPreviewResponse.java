package com.example.demo.dto.esports;

import java.util.List;

public record EsportsGameDraftImportPreviewResponse(
        String previewToken,
        boolean readyToImport,
        ImportSummary summary,
        List<RowPreview> rows,
        List<String> errors,
        List<String> warnings
) {

    public record ImportSummary(
            int totalRows,
            int validRows,
            int errorRows,
            int rowsWithWarnings,
            int matchesToCreate,
            int matchesToUpdate,
            int draftsToCreate,
            int draftsToOverwrite
    ) {
    }

    public record RowPreview(
            int rowNumber,
            Long matchId,
            String matchLabel,
            Integer gameNumber,
            String date,
            String tournament,
            String team1,
            String team2,
            String blueTeam,
            String redTeam,
            String winner,
            String durationText,
            String matchAction,
            String draftAction,
            List<String> errors,
            List<String> warnings
    ) {
    }
}
