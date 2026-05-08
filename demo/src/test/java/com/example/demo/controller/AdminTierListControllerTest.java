package com.example.demo.controller;

import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.TierListCommunityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTierListControllerTest {

    @Test
    void regenerateOfficialTierListFromHeroScoresRequiresAuthentication() {
        AdminTierListController controller = new AdminTierListController(mock(TierListCommunityService.class));

        assertThrows(ResponseStatusException.class, () -> controller.regenerateOfficialTierListFromHeroScores(null));
    }

    @Test
    void regenerateOfficialTierListFromHeroScoresDelegatesToService() {
        TierListCommunityService communityService = mock(TierListCommunityService.class);
        Authentication authentication = mock(Authentication.class);
        AdminTierListController controller = new AdminTierListController(communityService);

        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        Map<String, Object> payload = Map.of("id", 1L, "isOfficial", true);

        when(authentication.getPrincipal()).thenReturn(principal);
        when(communityService.regenerateOfficialTierListFromHeroScores(principal, authentication)).thenReturn(payload);

        assertEquals(payload, controller.regenerateOfficialTierListFromHeroScores(authentication).getBody());
        verify(communityService).regenerateOfficialTierListFromHeroScores(principal, authentication);
    }

    @Test
    void setAdminRatingRequiresAuthentication() {
        AdminTierListController controller = new AdminTierListController(mock(TierListCommunityService.class));

        assertThrows(ResponseStatusException.class, () -> controller.setAdminRating(7L, null, Map.of("ratingValue", 4)));
    }

    @Test
    void setAdminRatingDelegatesCombinedSummaryPayloadToService() {
        TierListCommunityService communityService = mock(TierListCommunityService.class);
        Authentication authentication = mock(Authentication.class);
        AdminTierListController controller = new AdminTierListController(communityService);

        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        Map<String, Object> payload = Map.of(
                "average", 3.5,
                "count", 2L,
                "averageUserRating", 3.5,
                "userRatingCount", 2L,
                "adminRating", 3.0
        );

        when(authentication.getPrincipal()).thenReturn(principal);
        when(communityService.setAdminRating(7L, principal, 3.0, "note")).thenReturn(payload);

        assertEquals(payload, controller.setAdminRating(7L, authentication, Map.of(
                "ratingValue", 3.0,
                "note", "note"
        )).getBody());
        verify(communityService).setAdminRating(7L, principal, 3.0, "note");
    }
}
