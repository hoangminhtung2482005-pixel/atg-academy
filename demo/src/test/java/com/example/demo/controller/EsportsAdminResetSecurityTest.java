package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import com.example.demo.security.ApiAccessDeniedHandler;
import com.example.demo.security.ApiAuthenticationEntryPoint;
import com.example.demo.security.GoogleJwtAuthenticationFilter;
import com.example.demo.security.GoogleJwtAuthenticator;
import com.example.demo.security.SecurityConfig;
import com.example.demo.service.EsportsAdminMaintenanceService;
import com.example.demo.service.EsportsAdminService;
import com.example.demo.service.EsportsDraftService;
import com.example.demo.service.EsportsFranchiseService;
import com.example.demo.service.EsportsTournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EsportsAdminController.class)
@Import({
        SecurityConfig.class,
        GoogleJwtAuthenticationFilter.class,
        GoogleJwtAuthenticator.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "app.security.google-client-id=test-google-client",
        "app.security.admin-emails=admin@example.com",
        "app.cors.allowed-origins=http://localhost:8080"
})
class EsportsAdminResetSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EsportsAdminService esportsAdminService;

    @MockitoBean
    private EsportsAdminMaintenanceService esportsAdminMaintenanceService;

    @MockitoBean
    private EsportsDraftService esportsDraftService;

    @MockitoBean
    private EsportsFranchiseService esportsFranchiseService;

    @MockitoBean
    private EsportsTournamentService esportsTournamentService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void resetDataRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/esports/reset-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmationText":"RESET ESPORTS DATA","backupBeforeReset":true}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void resetDataRejectsAuthenticatedUserWithoutAdminRole() throws Exception {
        mockMvc.perform(post("/api/admin/esports/reset-data")
                        .with(user("user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmationText":"RESET ESPORTS DATA","backupBeforeReset":true}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
