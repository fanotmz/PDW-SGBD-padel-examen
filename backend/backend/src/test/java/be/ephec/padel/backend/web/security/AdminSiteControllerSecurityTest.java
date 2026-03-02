package be.ephec.padel.backend.web.security;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminSiteController;
import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.AdminSiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminSiteController.class)
@Import(SecurityConfig.class)
class AdminSiteControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSiteService adminSiteService;

    @Test
    void sansAuth_retourne401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void nonAdmin_retourne403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_retourne200_etListe() throws Exception {
        when(adminSiteService.getJoueursBySite(1L)).thenReturn(List.of(
                new JoueurAdminDto("J001", "Dupont", TypeJoueur.SITE, new BigDecimal("15.00"))
        ));

        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].matricule").value("J001"))
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[0].type").value("SITE"))
                .andExpect(jsonPath("$[0].solde").value(15.00));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void admin_siteInexistant_retourne404() throws Exception {
        when(adminSiteService.getJoueursBySite(eq(999L)))
                .thenThrow(new NotFoundException("Site introuvable: 999"));

        mockMvc.perform(get("/api/v1/admin/sites/999/joueurs"))
                .andExpect(status().isNotFound());
    }
}