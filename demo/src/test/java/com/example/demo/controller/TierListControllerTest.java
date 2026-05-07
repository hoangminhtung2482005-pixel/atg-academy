package com.example.demo.controller;

import com.example.demo.entity.TierList;
import com.example.demo.entity.User;
import com.example.demo.repository.TierListRepository;
import com.example.demo.security.GoogleUserPrincipal;
import com.example.demo.service.HeroContentDataService;
import com.example.demo.service.TierListCommunityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TierListControllerTest {

    @Test
    void createTierListUsesPrimaryRoleImportNormalizationForOfficialImport() throws Exception {
        TierListRepository tierListRepository = mock(TierListRepository.class);
        HeroContentDataService heroContentDataService = mock(HeroContentDataService.class);
        TierListCommunityService communityService = mock(TierListCommunityService.class);
        Authentication authentication = mock(Authentication.class);

        TierListController controller = new TierListController(
                tierListRepository,
                new ObjectMapper(),
                heroContentDataService,
                communityService
        );

        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        User author = new User();
        author.setEmail(principal.email());
        author.setName("ATG Admin");
        author.setRole("ADMIN");

        Map<String, Object> rawContentData = new LinkedHashMap<>();
        rawContentData.put("rows", java.util.List.of());
        Map<String, Object> normalizedContentData = new LinkedHashMap<>();
        normalizedContentData.put("columns", java.util.List.of());
        normalizedContentData.put("rows", java.util.List.of());

        when(authentication.getPrincipal()).thenReturn(principal);
        when(communityService.findOrCreateUser(principal)).thenReturn(author);
        when(communityService.buildTierListResponse(any(TierList.class), eq(authentication))).thenReturn(Map.of("id", 1L));
        when(tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(heroContentDataService.normalizeOfficialImportForStorage(rawContentData)).thenReturn(normalizedContentData);

        controller.createTierList(authentication, Map.of(
                "title", "Tier List Meta",
                "isOfficial", true,
                "importMode", "PRIMARY_ROLE",
                "contentData", rawContentData
        ));

        verify(heroContentDataService).normalizeOfficialImportForStorage(rawContentData);
        verify(heroContentDataService, never()).normalizeForStorage(rawContentData);
        verify(tierListRepository).save(any(TierList.class));
    }

    @Test
    void createTierListUsesTierListMetaAsDefaultOfficialTitle() throws Exception {
        TierListRepository tierListRepository = mock(TierListRepository.class);
        HeroContentDataService heroContentDataService = mock(HeroContentDataService.class);
        TierListCommunityService communityService = mock(TierListCommunityService.class);
        Authentication authentication = mock(Authentication.class);

        TierListController controller = new TierListController(
                tierListRepository,
                new ObjectMapper(),
                heroContentDataService,
                communityService
        );

        GoogleUserPrincipal principal = new GoogleUserPrincipal("admin@atg.test", "Admin", "", "ADMIN");
        User author = new User();
        author.setEmail(principal.email());
        author.setName("ATG Admin");
        author.setRole("ADMIN");

        Map<String, Object> rawContentData = new LinkedHashMap<>();
        rawContentData.put("rows", java.util.List.of());

        when(authentication.getPrincipal()).thenReturn(principal);
        when(communityService.findOrCreateUser(principal)).thenReturn(author);
        when(communityService.buildTierListResponse(any(TierList.class), eq(authentication))).thenReturn(Map.of("id", 1L));
        when(tierListRepository.findFirstByIsOfficialTrueOrderByUpdatedAtDesc()).thenReturn(Optional.empty());
        when(heroContentDataService.normalizeForStorage(rawContentData)).thenReturn(rawContentData);

        controller.createTierList(authentication, Map.of(
                "isOfficial", true,
                "contentData", rawContentData
        ));

        ArgumentCaptor<TierList> savedTierList = ArgumentCaptor.forClass(TierList.class);
        verify(tierListRepository).save(savedTierList.capture());
        assertEquals("Tier List Meta", savedTierList.getValue().getTitle());
    }

    @Test
    void getMyCommunityTierListsRequiresAuthentication() {
        TierListController controller = new TierListController(
                mock(TierListRepository.class),
                new ObjectMapper(),
                mock(HeroContentDataService.class),
                mock(TierListCommunityService.class)
        );

        assertThrows(ResponseStatusException.class, () -> controller.getMyCommunityTierLists(null));
    }

    @Test
    void getMyCommunityTierListsDelegatesToService() {
        TierListRepository tierListRepository = mock(TierListRepository.class);
        HeroContentDataService heroContentDataService = mock(HeroContentDataService.class);
        TierListCommunityService communityService = mock(TierListCommunityService.class);
        Authentication authentication = mock(Authentication.class);

        TierListController controller = new TierListController(
                tierListRepository,
                new ObjectMapper(),
                heroContentDataService,
                communityService
        );

        GoogleUserPrincipal principal = new GoogleUserPrincipal("user@atg.test", "User", "", "USER");
        List<Map<String, Object>> tierLists = List.of(Map.of("id", 7L));

        when(authentication.getPrincipal()).thenReturn(principal);
        when(communityService.getCurrentUserCommunityTierLists(principal, authentication)).thenReturn(tierLists);

        assertEquals(tierLists, controller.getMyCommunityTierLists(authentication).getBody());
        verify(communityService).getCurrentUserCommunityTierLists(principal, authentication);
    }
}
