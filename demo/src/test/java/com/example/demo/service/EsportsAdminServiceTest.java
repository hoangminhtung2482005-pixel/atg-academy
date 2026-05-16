package com.example.demo.service;

import com.example.demo.dto.esports.EsportsMatchRequest;
import com.example.demo.entity.EsportsMatch;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import com.example.demo.repository.EsportsTournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsAdminServiceTest {

    @Mock
    private EsportsTeamRepository esportsTeamRepository;

    @Mock
    private EsportsMatchRepository esportsMatchRepository;

    @Mock
    private EsportsTournamentRepository esportsTournamentRepository;

    @Mock
    private EloCalculationService eloCalculationService;

    @Test
    void addMatchNormalizesFinalAliasToCk() {
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsAdminService service = service();
        service.addMatch(matchRequest("final"));

        ArgumentCaptor<EsportsMatch> captor = ArgumentCaptor.forClass(EsportsMatch.class);
        verify(esportsMatchRepository).save(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo("ck");
    }

    @Test
    void addMatchNormalizesGroupAliasToBang() {
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsAdminService service = service();
        service.addMatch(matchRequest("group"));

        ArgumentCaptor<EsportsMatch> captor = ArgumentCaptor.forClass(EsportsMatch.class);
        verify(esportsMatchRepository).save(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo("bang");
    }

    @Test
    void addMatchNormalizesQualifierAliasToVongloai() {
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsAdminService service = service();
        service.addMatch(matchRequest("qualifier"));

        ArgumentCaptor<EsportsMatch> captor = ArgumentCaptor.forClass(EsportsMatch.class);
        verify(esportsMatchRepository).save(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo("vongloai");
    }

    @Test
    void updateMatchNormalizesPlayOffAliasToPlayoff() {
        EsportsMatch existing = new EsportsMatch();
        existing.setId(10L);
        existing.setTeam1Code("FS");
        existing.setTeam2Code("SGP");
        existing.setScore1(3);
        existing.setScore2(1);
        existing.setStage("bang");

        when(esportsMatchRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(esportsMatchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EsportsAdminService service = service();
        service.updateMatch(10L, matchRequest("play-off"));

        ArgumentCaptor<EsportsMatch> captor = ArgumentCaptor.forClass(EsportsMatch.class);
        verify(esportsMatchRepository).save(captor.capture());
        assertThat(captor.getValue().getStage()).isEqualTo("playoff");
    }

    @Test
    void addMatchRejectsUnknownStageWithoutSaving() {
        EsportsAdminService service = service();

        assertThatThrownBy(() -> service.addMatch(matchRequest("abcxyz")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stage không hợp lệ: abcxyz");

        verify(esportsMatchRepository, never()).save(any());
        verify(eloCalculationService, never()).calculateAllRankings();
    }

    private EsportsAdminService service() {
        return new EsportsAdminService(
                esportsTeamRepository,
                esportsMatchRepository,
                esportsTournamentRepository,
                eloCalculationService
        );
    }

    private EsportsMatchRequest matchRequest(String stage) {
        return new EsportsMatchRequest(
                null,
                "FS",
                "SGP",
                3,
                1,
                "1",
                stage,
                null
        );
    }
}
