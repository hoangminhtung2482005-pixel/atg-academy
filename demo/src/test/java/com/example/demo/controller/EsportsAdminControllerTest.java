package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsMatchRequest;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmRequest;
import com.example.demo.dto.esports.EsportsGameDraftImportConfirmResponse;
import com.example.demo.dto.esports.EsportsGameDraftImportPreviewResponse;
import com.example.demo.dto.esports.EsportsResetDataRequest;
import com.example.demo.dto.esports.EsportsResetDataResponse;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.service.EsportsAdminMaintenanceService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EsportsAdminControllerTest {

    @Test
    void exportGameDraftsCsvReturnsAttachmentHeaders() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        byte[] payload = "\uFEFFDate,Tournament\r\n".getBytes(StandardCharsets.UTF_8);

        when(esportsDraftService.exportGameDraftsCsv(null, "AER Pro League", 10L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3)))
                .thenReturn(payload);

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), mock(EsportsAdminMaintenanceService.class), esportsDraftService);

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
                .thenThrow(new IllegalArgumentException("tournamentName không hợp lệ."));

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), mock(EsportsAdminMaintenanceService.class), esportsDraftService);

        ResponseEntity<?> response = controller.exportGameDraftsCsv(null, "Bad League", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "tournamentName không hợp lệ."));
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

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), mock(EsportsAdminMaintenanceService.class), esportsDraftService);

        ResponseEntity<?> response = controller.previewGameDraftImport(file, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void confirmGameDraftImportReturnsPayload() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        EsportsGameDraftImportConfirmRequest request = new EsportsGameDraftImportConfirmRequest("preview-1");
        EsportsGameDraftImportConfirmResponse payload = new EsportsGameDraftImportConfirmResponse(
                3,
                1,
                0,
                2,
                1,
                List.of(10L),
                1,
                true
        );
        when(esportsDraftService.confirmGameDraftImport(request)).thenReturn(payload);

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), mock(EsportsAdminMaintenanceService.class), esportsDraftService);

        ResponseEntity<?> response = controller.confirmGameDraftImport(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void confirmGameDraftImportReturnsBadRequestForInvalidPreviewToken() {
        EsportsDraftService esportsDraftService = mock(EsportsDraftService.class);
        EsportsGameDraftImportConfirmRequest request = new EsportsGameDraftImportConfirmRequest("bad-token");
        when(esportsDraftService.confirmGameDraftImport(request))
                .thenThrow(new IllegalArgumentException("Preview import đã hết hạn hoặc không tồn tại. Hãy preview lại file."));

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), mock(EsportsAdminMaintenanceService.class), esportsDraftService);

        ResponseEntity<?> response = controller.confirmGameDraftImport(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Preview import đã hết hạn hoặc không tồn tại. Hãy preview lại file."));
    }

    @Test
    void resetEsportsDataReturnsPayload() {
        EsportsAdminMaintenanceService esportsAdminMaintenanceService = mock(EsportsAdminMaintenanceService.class);
        EsportsResetDataResponse payload = new EsportsResetDataResponse(
                true,
                "sql/backups/aov_tactics_before_reset_esports_data_20260514_140000.sql",
                850,
                209,
                0,
                0,
                0,
                false,
                "player_stats thuoc Ban/Pick leaderboard"
        );
        when(esportsAdminMaintenanceService.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", true)))
                .thenReturn(payload);

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), esportsAdminMaintenanceService, mock(EsportsDraftService.class));

        ResponseEntity<?> response = controller.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", true));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void resetEsportsDataReturnsBadRequestForInvalidConfirmation() {
        EsportsAdminMaintenanceService esportsAdminMaintenanceService = mock(EsportsAdminMaintenanceService.class);
        when(esportsAdminMaintenanceService.resetEsportsData(new EsportsResetDataRequest("WRONG", true)))
                .thenThrow(new IllegalArgumentException("confirmationText phải chính xác là 'RESET ESPORTS DATA'."));

        EsportsAdminController controller = controller(mock(EsportsAdminService.class), esportsAdminMaintenanceService, mock(EsportsDraftService.class));

        ResponseEntity<?> response = controller.resetEsportsData(new EsportsResetDataRequest("WRONG", true));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "confirmationText phải chính xác là 'RESET ESPORTS DATA'."));
    }

    @Test
    void addMatchReturnsBadRequestForUnknownStage() {
        EsportsAdminService esportsAdminService = mock(EsportsAdminService.class);
        EsportsMatchRequest request = new EsportsMatchRequest(null, "FS", "SGP", 3, 1, "1", "abcxyz", null);
        when(esportsAdminService.addMatch(any()))
                .thenThrow(new IllegalArgumentException("Stage không hợp lệ: abcxyz. Chỉ chấp nhận: ck, playoff, bang, vongloai."));

        EsportsAdminController controller = controller(esportsAdminService, mock(EsportsAdminMaintenanceService.class), mock(EsportsDraftService.class));

        ResponseEntity<?> response = controller.addMatch(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Stage không hợp lệ: abcxyz. Chỉ chấp nhận: ck, playoff, bang, vongloai."));
    }

    @Test
    void updateMatchReturnsUpdatedPayload() {
        EsportsAdminService esportsAdminService = mock(EsportsAdminService.class);
        EsportsMatchRequest request = new EsportsMatchRequest(null, "FS", "SGP", 3, 1, "1", "play-off", null);
        EsportsMatch payload = new EsportsMatch();
        payload.setId(10L);
        payload.setStage("playoff");
        when(esportsAdminService.updateMatch(10L, request)).thenReturn(payload);

        EsportsAdminController controller = controller(esportsAdminService, mock(EsportsAdminMaintenanceService.class), mock(EsportsDraftService.class));

        ResponseEntity<?> response = controller.updateMatch(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    private EsportsAdminController controller(EsportsAdminService esportsAdminService,
                                              EsportsAdminMaintenanceService esportsAdminMaintenanceService,
                                              EsportsDraftService esportsDraftService) {
        return new EsportsAdminController(
                esportsAdminService,
                esportsAdminMaintenanceService,
                esportsDraftService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );
    }
}
