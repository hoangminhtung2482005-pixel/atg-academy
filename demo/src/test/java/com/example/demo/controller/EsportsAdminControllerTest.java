package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmResponse;
import com.example.demo.dto.esports.EsportsGameDraftImportPreviewResponse;
import com.example.demo.service.EsportsAdminService;
import com.example.demo.service.EsportsDraftService;
import com.example.demo.service.EsportsFranchiseService;
import com.example.demo.service.EsportsTournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EsportsAdminControllerTest {

    @Test
    void exportGameDraftsCsvReturnsAttachmentHeaders() {
        EsportsAdminService esportsAdminService = mock(EsportsAdminService.class);
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        byte[] payload = "\uFEFFDate,Tournament\r\n".getBytes(StandardCharsets.UTF_8);

        when(esportsDraftService.exportGameDraftsCsv(null, "AER Pro League", 10L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3)))
                .thenReturn(payload);

        EsportsAdminController controller = new EsportsAdminController(
                esportsAdminService,
                esportsDraftService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );

        ResponseEntity<?> response = controller.exportGameDraftsCsv(
                null,
                "AER Pro League",
                10L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("attachment; filename=\"esports-game-drafts.csv\"");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        assertThat((byte[]) response.getBody()).containsExactly(payload);
    }

    @Test
    void exportGameDraftsCsvReturnsBadRequestForInvalidFilter() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        when(esportsDraftService.exportGameDraftsCsv(null, "Bad League", null, null, null))
                .thenThrow(new IllegalArgumentException("tournamentName khong hop le."));

        EsportsAdminController controller = new EsportsAdminController(
                mock(EsportsAdminService.class),
                esportsDraftService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );

        ResponseEntity<?> response = controller.exportGameDraftsCsv(null, "Bad League", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "tournamentName khong hop le."));
    }

    @Test
    void previewGameDraftImportReturnsPayload() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        MockMultipartFile file = new MockMultipartFile("file", "drafts.csv", "text/csv", "Date,Tournament\r\n".getBytes(StandardCharsets.UTF_8));
        EsportsGameDraftImportPreviewResponse payload = new EsportsGameDraftImportPreviewResponse(
                "preview-1",
                true,
                new EsportsGameDraftImportPreviewResponse.ImportSummary(1, 1, 0, 0, 1, 0, 1, 0),
                List.of(),
                List.of(),
                List.of()
        );
        when(esportsDraftService.previewGameDraftImport(file, false)).thenReturn(payload);

        EsportsAdminController controller = new EsportsAdminController(
                mock(EsportsAdminService.class),
                esportsDraftService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );

        ResponseEntity<?> response = controller.previewGameDraftImport(file, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void confirmGameDraftImportReturnsBadRequestForInvalidPreviewToken() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        when(esportsDraftService.confirmGameDraftImport(new EsportsGameDraftImportConfirmRequest("bad-token")))
                .thenThrow(new IllegalArgumentException("Preview import da het han hoac khong ton tai. Hay preview lai file."));

        EsportsAdminController controller = new EsportsAdminController(
                mock(EsportsAdminService.class),
                esportsDraftService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );

        ResponseEntity<?> response = controller.confirmGameDraftImport(new EsportsGameDraftImportConfirmRequest("bad-token"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Preview import da het han hoac khong ton tai. Hay preview lai file."));
    }
}
