package com.example.demo.service;

import com.example.demo.dto.esports.EsportsDraftTournamentAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroPickStatAggregate;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsDataServiceTest {

    @Mock
    private EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;

    @Test
    void getTopBannedHeroesUsesAllDraftDataWhenTournamentMissing() {
        when(esportsMatchDraftActionRepository.findTopHeroBanStats(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(new EsportsHeroBanStatAggregate(7L, "Hayate", "/images/heroes/Hayate.jpg", 12L)));

        EsportsDataService service = new EsportsDataService(esportsMatchDraftActionRepository);

        List<EsportsHeroBanStatResponse> result = service.getTopBannedHeroes(null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(esportsMatchDraftActionRepository).findTopHeroBanStats(isNull(), isNull(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(result).containsExactly(new EsportsHeroBanStatResponse(
                7L,
                "Hayate",
                "/images/heroes/Hayate.jpg",
                12L,
                null
        ));
    }

    @Test
    void getTopBlueBannedHeroesFiltersBlueSideAndSanitizesLimit() {
        when(esportsMatchDraftActionRepository.findTopHeroBanStats(eq("0"), eq(BanPickTeamSide.BLUE), any(Pageable.class)))
                .thenReturn(List.of(new EsportsHeroBanStatAggregate(9L, "Aya", null, 4L)));

        EsportsDataService service = new EsportsDataService(esportsMatchDraftActionRepository);

        List<EsportsHeroBanStatResponse> result = service.getTopBlueBannedHeroes("AER International", 99);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(esportsMatchDraftActionRepository).findTopHeroBanStats(eq("0"), eq(BanPickTeamSide.BLUE), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(result).containsExactly(new EsportsHeroBanStatResponse(
                9L,
                "Aya",
                null,
                4L,
                "AER International"
        ));
    }

    @Test
    void getAvailableTournamentsMapsTournamentNamesByLatestDraftData() {
        when(esportsMatchDraftActionRepository.findDraftTournamentsOrderByLatestMatchDesc()).thenReturn(List.of(
                new EsportsDraftTournamentAggregate("2", LocalDateTime.of(2026, 5, 7, 10, 0)),
                new EsportsDraftTournamentAggregate("0", LocalDateTime.of(2026, 5, 6, 10, 0)),
                new EsportsDraftTournamentAggregate("2", LocalDateTime.of(2026, 5, 5, 10, 0))
        ));

        EsportsDataService service = new EsportsDataService(esportsMatchDraftActionRepository);

        List<EsportsTournamentOptionResponse> result = service.getAvailableTournaments();

        assertThat(result).containsExactly(
                new EsportsTournamentOptionResponse("AER Challenger", "2"),
                new EsportsTournamentOptionResponse("AER International", "0")
        );
    }

    @Test
    void getTopBannedHeroesRejectsUnknownTournamentName() {
        EsportsDataService service = new EsportsDataService(esportsMatchDraftActionRepository);

        assertThatThrownBy(() -> service.getTopBannedHeroes("Unknown League", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tournamentName khong hop le.");

        verifyNoInteractions(esportsMatchDraftActionRepository);
    }

    @Test
    void getHeroStatsMergesPickAndBanAggregatesWithoutDuplicateCounts() {
        when(esportsMatchDraftActionRepository.findHeroPickStats("1")).thenReturn(List.of(
                new EsportsHeroPickStatAggregate(7L, "Hayate", "/images/heroes/Hayate.jpg", 4L, 3L, 3L, 2L, 1L, 1L),
                new EsportsHeroPickStatAggregate(9L, "Aya", null, 1L, 0L, 0L, 0L, 1L, 0L)
        ));
        when(esportsMatchDraftActionRepository.findHeroBanStats("1")).thenReturn(List.of(
                new EsportsHeroBanStatAggregate(7L, "Hayate", "/images/heroes/Hayate.jpg", 2L),
                new EsportsHeroBanStatAggregate(11L, "Toro", "/images/heroes/Toro.jpg", 5L)
        ));

        EsportsDataService service = new EsportsDataService(esportsMatchDraftActionRepository);

        List<EsportsHeroStatResponse> result = service.getHeroStats("AER Pro League");

        verify(esportsMatchDraftActionRepository).findHeroPickStats("1");
        verify(esportsMatchDraftActionRepository).findHeroBanStats("1");

        assertThat(result).containsExactly(
                new EsportsHeroStatResponse(7L, "Hayate", "/images/heroes/Hayate.jpg",
                        4L, 3L, 1L, 75.0D,
                        3L, 2L, 1L, 66.66666666666667D,
                        1L, 1L, 0L, 100.0D,
                        2L, 6L),
                new EsportsHeroStatResponse(11L, "Toro", "/images/heroes/Toro.jpg",
                        0L, 0L, 0L, 0D,
                        0L, 0L, 0L, 0D,
                        0L, 0L, 0L, 0D,
                        5L, 5L),
                new EsportsHeroStatResponse(9L, "Aya", null,
                        1L, 0L, 1L, 0D,
                        0L, 0L, 0L, 0D,
                        1L, 0L, 1L, 0D,
                        0L, 1L)
        );
    }
}
