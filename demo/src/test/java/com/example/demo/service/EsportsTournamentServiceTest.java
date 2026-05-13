package com.example.demo.service;

import com.example.demo.dto.esports.EsportsTournamentRequest;
import com.example.demo.dto.esports.EsportsTournamentResponse;
import com.example.demo.entity.EsportsFranchise;
import com.example.demo.entity.EsportsTournament;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import com.example.demo.repository.EsportsTournamentTeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsTournamentServiceTest {

    @Mock
    private EsportsFranchiseService esportsFranchiseService;

    @Mock
    private EsportsTournamentRepository esportsTournamentRepository;

    @Mock
    private EsportsTournamentTeamRepository esportsTournamentTeamRepository;

    @Mock
    private EsportsTeamRepository esportsTeamRepository;

    @Mock
    private EsportsMatchRepository esportsMatchRepository;

    @Test
    void createTournamentStoresAerTierAndReturnsIt() {
        EsportsFranchise franchise = franchise(1L, "AOG", "Arena Of Glory", "T1");
        when(esportsTournamentRepository.existsBySlugIgnoreCase("aog-spring-2026")).thenReturn(false);
        when(esportsFranchiseService.findEntityById(1L)).thenReturn(franchise);
        when(esportsTournamentRepository.save(any())).thenAnswer(invocation -> {
            EsportsTournament tournament = invocation.getArgument(0);
            tournament.setId(10L);
            tournament.setCreatedAt(LocalDateTime.of(2026, 5, 13, 9, 0));
            tournament.setUpdatedAt(LocalDateTime.of(2026, 5, 13, 9, 5));
            return tournament;
        });
        when(esportsTournamentTeamRepository.countByTournamentId(10L)).thenReturn(0L);
        when(esportsMatchRepository.countByTournamentId(10L)).thenReturn(0L);

        EsportsTournamentResponse response = service().createTournament(new EsportsTournamentRequest(
                1L,
                "AOG Spring 2026",
                "aog-spring-2026",
                2026,
                "Spring",
                "T1",
                2,
                null,
                null,
                "UPCOMING",
                "Seed tournament",
                "/images/leagues/AOG_logo.png"
        ));

        ArgumentCaptor<EsportsTournament> captor = ArgumentCaptor.forClass(EsportsTournament.class);
        verify(esportsTournamentRepository).save(captor.capture());

        assertThat(captor.getValue().getAerTier()).isEqualTo(2);
        assertThat(response.aerTier()).isEqualTo(2);
        assertThat(response.franchiseCode()).isEqualTo("AOG");
    }

    @Test
    void updateTournamentDefaultsAerTierToOneWhenRequestOmitsField() {
        EsportsFranchise franchise = franchise(1L, "RPL", "RoV Pro League", "T1");
        EsportsTournament existing = tournament(11L, franchise, "RPL Summer 2026", "rpl-summer-2026", "T1", 3);

        when(esportsTournamentRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(esportsTournamentRepository.existsBySlugIgnoreCaseAndIdNot("rpl-summer-2026", 11L)).thenReturn(false);
        when(esportsFranchiseService.findEntityById(1L)).thenReturn(franchise);
        when(esportsTournamentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(esportsTournamentTeamRepository.countByTournamentId(11L)).thenReturn(2L);
        when(esportsMatchRepository.countByTournamentId(11L)).thenReturn(1L);

        EsportsTournamentResponse response = service().updateTournament(11L, new EsportsTournamentRequest(
                1L,
                "RPL Summer 2026",
                "rpl-summer-2026",
                2026,
                "Summer",
                "T1",
                null,
                null,
                null,
                "ONGOING",
                null,
                null
        ));

        assertThat(existing.getAerTier()).isEqualTo(1);
        assertThat(response.aerTier()).isEqualTo(1);
        assertThat(response.linkedMatchCount()).isEqualTo(1L);
    }

    @Test
    void createTournamentRejectsAerTierLessThanOrEqualToZero() {
        EsportsFranchise franchise = franchise(1L, "APL", "AoV Premier League", "T0");
        when(esportsTournamentRepository.existsBySlugIgnoreCase("apl-2026")).thenReturn(false);
        when(esportsFranchiseService.findEntityById(1L)).thenReturn(franchise);

        assertThatThrownBy(() -> service().createTournament(new EsportsTournamentRequest(
                1L,
                "APL 2026",
                "apl-2026",
                2026,
                null,
                "T0",
                0,
                null,
                null,
                "UPCOMING",
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aerTier phai lon hon 0");

        verify(esportsTournamentRepository, never()).save(any());
    }

    @Test
    void getPublicTournamentsKeepsAerTierInFilteredListing() {
        EsportsFranchise franchise = franchise(2L, "AOG", "Arena Of Glory", "T1");
        EsportsTournament listedTournament = tournament(20L, franchise, "AOG Winter 2026", "aog-winter-2026", "T1", 1);
        EsportsTournament seededTournament = tournament(99L, franchise, "Seed", "seed", "T1", 1);

        when(esportsTournamentRepository.findBySlugIgnoreCase(anyString())).thenReturn(Optional.of(seededTournament));
        when(esportsTournamentRepository.findAllForListing(null, "AOG")).thenReturn(List.of(listedTournament));
        when(esportsTournamentTeamRepository.countByTournamentId(20L)).thenReturn(4L);
        when(esportsMatchRepository.countByTournamentId(20L)).thenReturn(2L);

        List<EsportsTournamentResponse> response = service().getPublicTournaments(null, "aog");

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.franchiseCode()).isEqualTo("AOG");
            assertThat(item.aerTier()).isEqualTo(1);
            assertThat(item.teamCount()).isEqualTo(4L);
        });
        verify(esportsTournamentRepository).findAllForListing(null, "AOG");
    }

    private EsportsTournamentService service() {
        return new EsportsTournamentService(
                esportsFranchiseService,
                esportsTournamentRepository,
                esportsTournamentTeamRepository,
                esportsTeamRepository,
                esportsMatchRepository
        );
    }

    private EsportsFranchise franchise(Long id, String code, String name, String tierLevel) {
        EsportsFranchise franchise = new EsportsFranchise();
        franchise.setId(id);
        franchise.setCode(code);
        franchise.setName(name);
        franchise.setTierLevel(tierLevel);
        franchise.setActive(Boolean.TRUE);
        return franchise;
    }

    private EsportsTournament tournament(Long id,
                                         EsportsFranchise franchise,
                                         String name,
                                         String slug,
                                         String tierLevel,
                                         Integer aerTier) {
        EsportsTournament tournament = new EsportsTournament();
        tournament.setId(id);
        tournament.setFranchise(franchise);
        tournament.setName(name);
        tournament.setSlug(slug);
        tournament.setTierLevel(tierLevel);
        tournament.setAerTier(aerTier);
        tournament.setStatus("UPCOMING");
        return tournament;
    }
}
