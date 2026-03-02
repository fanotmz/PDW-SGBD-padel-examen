package be.ephec.padel.backend.web.security;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminStatsController;
import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminStatsController.class)
@Import(SecurityConfig.class)
class AdminStatsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    // ---------- /ca ----------

    @Test
    void ca_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/ca")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void ca_avecRoleNonAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/ca")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void ca_avecRoleAdmin_retourne200_etPayloadOk() throws Exception {
        when(adminStatsService.getCa(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new AdminCaStatsDto(new BigDecimal("30.00"),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)));

        mockMvc.perform(get("/api/v1/admin/stats/ca")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.caTotal").value(30.00))
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"));
    }

    // ---------- /matchs ----------

    @Test
    void matchs_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/matchs")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void matchs_avecRoleNonAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/matchs")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void matchs_avecRoleAdmin_retourne200_etPayloadOk() throws Exception {
        when(adminStatsService.getNbMatchs(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new AdminMatchsStatsDto(12,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 31)));

        mockMvc.perform(get("/api/v1/admin/stats/matchs")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.nbMatchs").value(12))
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"));
    }

    // ---------- /dettes ----------

    @Test
    void dettes_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/dettes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void dettes_avecRoleNonAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/dettes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void dettes_avecRoleAdmin_retourne200_etPayloadOk() throws Exception {
        when(adminStatsService.getDettes())
                .thenReturn(new AdminDettesStatsDto(new BigDecimal("45.00"), 3));

        mockMvc.perform(get("/api/v1/admin/stats/dettes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.detteTotale").value(45.00))
                .andExpect(jsonPath("$.nbJoueursEnDette").value(3));
    }
}