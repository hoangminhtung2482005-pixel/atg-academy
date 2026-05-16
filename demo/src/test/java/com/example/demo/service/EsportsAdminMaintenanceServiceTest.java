package com.example.demo.service;

import com.example.demo.dto.esports.EsportsResetDataRequest;
import com.example.demo.dto.esports.EsportsResetDataResponse;
import com.example.demo.repository.EsportsGameDraftRepository;
import com.example.demo.repository.EsportsMatchRepository;
import com.example.demo.repository.EsportsTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsportsAdminMaintenanceServiceTest {

    @Mock
    private EsportsGameDraftRepository esportsGameDraftRepository;

    @Mock
    private EsportsMatchRepository esportsMatchRepository;

    @Mock
    private EsportsTeamRepository esportsTeamRepository;

    @Mock
    private EsportsDatabaseBackupService esportsDatabaseBackupService;

    @Mock
    private Environment environment;

    private EsportsAdminMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new EsportsAdminMaintenanceService(
                esportsGameDraftRepository,
                esportsMatchRepository,
                esportsTeamRepository,
                esportsDatabaseBackupService,
                environment
        );
    }

    @Test
    void resetEsportsDataRejectsWrongConfirmationText() {
        assertThatThrownBy(() -> service.resetEsportsData(new EsportsResetDataRequest("WRONG", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confirmationText");

        verifyNoInteractions(esportsDatabaseBackupService);
        verifyNoInteractions(esportsGameDraftRepository);
        verifyNoInteractions(esportsMatchRepository);
        verifyNoInteractions(esportsTeamRepository);
    }

    @Test
    void resetEsportsDataStopsWhenBackupFails() {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(esportsGameDraftRepository.count()).thenReturn(850L);
        when(esportsMatchRepository.count()).thenReturn(209L);
        when(esportsDatabaseBackupService.backupBeforeResetEsportsData())
                .thenThrow(new IllegalStateException("Không thể backup DB, reset bị hủy."));

        assertThatThrownBy(() -> service.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể backup DB");

        verify(esportsDatabaseBackupService).backupBeforeResetEsportsData();
        verify(esportsGameDraftRepository, never()).deleteAllInBatch();
        verify(esportsMatchRepository, never()).deleteAllInBatch();
        verify(esportsTeamRepository, never()).resetAllRankingFields();
        verify(esportsTeamRepository, never()).deleteAllInBatch();
    }

    @Test
    void resetEsportsDataDeletesDraftsBeforeMatchesAndPreservesPlayerStats() {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(esportsGameDraftRepository.count()).thenReturn(850L, 0L);
        when(esportsMatchRepository.count()).thenReturn(209L, 0L);
        when(esportsDatabaseBackupService.backupBeforeResetEsportsData())
                .thenReturn("sql/backups/aov_tactics_before_reset_esports_data_20260514_140000.sql");
        when(esportsTeamRepository.resetAllRankingFields()).thenReturn(24);

        EsportsResetDataResponse response =
                service.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", true));

        InOrder inOrder = inOrder(esportsDatabaseBackupService, esportsGameDraftRepository, esportsMatchRepository, esportsTeamRepository);
        inOrder.verify(esportsDatabaseBackupService).backupBeforeResetEsportsData();
        inOrder.verify(esportsGameDraftRepository).deleteAllInBatch();
        inOrder.verify(esportsMatchRepository).deleteAllInBatch();
        inOrder.verify(esportsTeamRepository).resetAllRankingFields();

        assertThat(response.reset()).isTrue();
        assertThat(response.backupFile()).isEqualTo("sql/backups/aov_tactics_before_reset_esports_data_20260514_140000.sql");
        assertThat(response.deletedGameDrafts()).isEqualTo(850L);
        assertThat(response.deletedMatches()).isEqualTo(209L);
        assertThat(response.deletedPlayerStats()).isZero();
        assertThat(response.remainingGameDrafts()).isZero();
        assertThat(response.remainingMatches()).isZero();
        assertThat(response.playerStatsCleared()).isFalse();
        assertThat(response.playerStatsRetainedReason()).contains("Ban/Pick leaderboard");

        verify(esportsTeamRepository, never()).deleteAllInBatch();
    }

    @Test
    void resetEsportsDataBlocksProductionProfileWithoutExplicitFlag() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});

        assertThatThrownBy(() -> service.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", true)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("allow-production=true");

        verifyNoInteractions(esportsDatabaseBackupService);
        verifyNoInteractions(esportsGameDraftRepository);
        verifyNoInteractions(esportsMatchRepository);
        verifyNoInteractions(esportsTeamRepository);
    }

    @Test
    void resetEsportsDataRequiresBackupFlag() {
        assertThatThrownBy(() -> service.resetEsportsData(new EsportsResetDataRequest("RESET ESPORTS DATA", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backupBeforeReset");

        verifyNoInteractions(esportsDatabaseBackupService);
    }
}
