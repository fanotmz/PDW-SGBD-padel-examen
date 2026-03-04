package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminGlobalController;
import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminGlobalController.class)
@Import(SecurityConfig.class)
class AdminGlobalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminStatsService adminStatsService;

    // -----------------------------
    // /api/v1/admin/info
    // -----------------------------

    @Test
    void adminInfo_sansAuth_401() throws Exception {
        mvc.perform(get("/api/v1/admin/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminInfo_adminSite_200() throws Exception {
        mvc.perform(get("/api/v1/admin/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminInfo_adminGlobal_200() throws Exception {
        mvc.perform(get("/api/v1/admin/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // -----------------------------
    // /api/v1/admin/stats/** (GLOBAL only)
    // -----------------------------

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void statsCa_adminSite_403() throws Exception {
        mvc.perform(get("/api/v1/admin/stats/ca")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminStatsService);
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void statsCa_adminGlobal_200_et_json() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(adminStatsService.getCa(from, to))
                .thenReturn(new AdminCaStatsDto(new BigDecimal("123.45"), from, to));

        mvc.perform(get("/api/v1/admin/stats/ca")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.caTotal").value(123.45))
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"));

        verify(adminStatsService, times(1)).getCa(from, to);
        verifyNoMoreInteractions(adminStatsService);
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void statsMatchs_adminGlobal_200() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(adminStatsService.getNbMatchs(from, to))
                .thenReturn(new AdminMatchsStatsDto(7L, from, to));

        mvc.perform(get("/api/v1/admin/stats/matchs")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbMatchs").value(7))
                .andExpect(jsonPath("$.from").value("2026-01-01"))
                .andExpect(jsonPath("$.to").value("2026-01-31"));

        verify(adminStatsService, times(1)).getNbMatchs(from, to);
        verifyNoMoreInteractions(adminStatsService);
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void statsDettes_adminGlobal_200() throws Exception {
        when(adminStatsService.getDettes())
                .thenReturn(new AdminDettesStatsDto(new BigDecimal("50.00"), 3L));

        mvc.perform(get("/api/v1/admin/stats/dettes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detteTotale").value(50.00))
                .andExpect(jsonPath("$.nbJoueursEnDette").value(3));

        verify(adminStatsService, times(1)).getDettes();
        verifyNoMoreInteractions(adminStatsService);
    }
}