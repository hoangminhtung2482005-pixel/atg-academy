package com.example.demo.controller;

import com.example.demo.dto.esports.EsportsDashboardResponse;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.service.EsportsDataService;
import com.example.demo.service.EsportsFranchiseService;
import com.example.demo.service.EsportsTournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EsportsControllerTest {

    @Test
    void getDraftTournamentsReturnsServicePayload() {
        EsportsDataService esportsDataService = mock(EsportsDataService.class);
        when(esportsDataService.getAvailableTournaments()).thenReturn(List.of(
                new EsportsTournamentOptionResponse(null, "AER Pro League", "1", null, true)
        ));

        EsportsController controller = controller(esportsDataService);

        ResponseEntity<List<EsportsTournamentOptionResponse>> response = controller.getDraftTournaments();

        assertThat(response.getBody()).containsExactly(
                new EsportsTournamentOptionResponse(null, "AER Pro League", "1", null, true)
        );
    }

    @Test
    void getTopBannedHeroesReturnsOkPayload() {
        EsportsDataService esportsDataService = mock(EsportsDataService.class);
        List<EsportsHeroBanStatResponse> payload = List.of(
                new EsportsHeroBanStatResponse(11L, "Lorion", "/images/heroes/Lorion.jpg", 8L, "AER Pro League")
        );
        when(esportsDataService.getTopBannedHeroes(null, "AER Pro League", 5)).thenReturn(payload);

        EsportsController controller = controller(esportsDataService);

        ResponseEntity<?> response = controller.getTopBannedHeroes(null, "AER Pro League", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void getTopBlueBannedHeroesReturnsBadRequestWhenTournamentInvalid() {
        EsportsDataService esportsDataService = mock(EsportsDataService.class);
        when(esportsDataService.getTopBlueBannedHeroes(null, "Bad League", 5))
                .thenThrow(new IllegalArgumentException("tournamentName khong hop le."));

        EsportsController controller = controller(esportsDataService);

        ResponseEntity<?> response = controller.getTopBlueBannedHeroes(null, "Bad League", 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(400));
        assertThat(response.getBody()).isEqualTo(Map.of("error", "tournamentName khong hop le."));
    }

    @Test
    void getHeroStatsReturnsOkPayload() {
        EsportsDataService esportsDataService = mock(EsportsDataService.class);
        List<EsportsHeroStatResponse> payload = List.of(
                new EsportsHeroStatResponse(
                        12L,
                        "Liliana",
                        "/images/heroes/Liliana.jpg",
                        9L,
                        6L,
                        3L,
                        66.66666666666667D,
                        4L,
                        3L,
                        1L,
                        75.0D,
                        5L,
                        3L,
                        2L,
                        60.0D,
                        7L,
                        4L,
                        3L,
                        16L
                )
        );
        when(esportsDataService.getHeroStats(null, "AER International")).thenReturn(payload);

        EsportsController controller = controller(esportsDataService);

        ResponseEntity<?> response = controller.getHeroStats(null, "AER International");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    @Test
    void getDashboardReturnsOkPayload() {
        EsportsDataService esportsDataService = mock(EsportsDataService.class);
        EsportsDashboardResponse payload = new EsportsDashboardResponse(
                new EsportsDashboardResponse.Summary(2L, 7L, 15L, 57.1D, null, 7L, 0L),
                List.of(new EsportsDashboardResponse.ActivityPoint(LocalDate.of(2026, 5, 9), 2L, 7L)),
                new EsportsDashboardResponse.SideAdvantage(4L, 3L, 7L, 57.1D, 42.9D),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(new EsportsDashboardResponse.TeamOption("FS", "Flash Wolves", "/images/teams/FS.png"))
        );
        when(esportsDataService.getDashboard(null, "AER Pro League", "FS", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9)))
                .thenReturn(payload);

        EsportsController controller = controller(esportsDataService);

        ResponseEntity<?> response = controller.getDashboard(
                null,
                "AER Pro League",
                "FS",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 9)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        assertThat(response.getBody()).isEqualTo(payload);
    }

    private EsportsController controller(EsportsDataService esportsDataService) {
        return new EsportsController(
                mock(EsportsTeamRepository.class),
                mock(EsportsMatchRepository.class),
                esportsDataService,
                mock(EsportsFranchiseService.class),
                mock(EsportsTournamentService.class)
        );
    }
}
