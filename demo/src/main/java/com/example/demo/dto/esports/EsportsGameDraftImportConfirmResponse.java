package com.example.demo.dto.esports;

import java.util.List;

public record EsportsGameDraftImportConfirmResponse(
        int importedRows,
        int createdMatches,
        int updatedMatches,
        int createdDrafts,
        int overwrittenDrafts,
        boolean rankingsRecalculated,
        List<Long> affectedMatchIds
) {
}
