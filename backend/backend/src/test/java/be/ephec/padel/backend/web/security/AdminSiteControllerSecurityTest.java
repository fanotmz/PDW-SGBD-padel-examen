package be.ephec.padel.backend.web.security;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminSiteController;
import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.dto.response.HoraireSiteDto;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.service.AdminSiteService;
import be.ephec.padel.backend.service.AdminSiteStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminSiteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.admin.global.username=adminGlobal",
        "app.security.admin.global.password=test123",
        "app.security.admin.site.password=test123",
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class AdminSiteControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminSiteService adminSiteService;

    @MockitoBean
    AdminSiteStatsService adminSiteStatsService;

    @Test
    void sansAuth_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sitesSansAuth_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "joueur1", roles = {"JOUEUR"})
    void sitesJoueurNonAdmin_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites"))
                .andExpect(status().isForbidden());
    }

    @Test
    void statsSansAuth_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/stats/dettes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite_auth_200_mapping_ok() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminGlobal_auth_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/joueurs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void sitesAdminGlobal_auth_200_json_avec_terrains_et_horaires() throws Exception {
        when(adminSiteService.getSitesConsultables()).thenReturn(List.of(
                siteDto(1L, "Site 1", "Bruxelles"),
                siteDto(2L, "Site 2", "Liege")
        ));

        mockMvc.perform(get("/api/v1/admin/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Site 1"))
                .andExpect(jsonPath("$[0].ville").value("Bruxelles"))
                .andExpect(jsonPath("$[0].joursFermeture[0]").value("MONDAY"))
                .andExpect(jsonPath("$[0].terrains[0].nom").value("Terrain A"))
                .andExpect(jsonPath("$[0].horaires[0].annee").value(2026))
                .andExpect(jsonPath("$[0].horaires[0].heureOuverture").value("08:00:00"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void sitesAdminSite_auth_200_uniquement_sites_retournes_par_service() throws Exception {
        when(adminSiteService.getSitesConsultables()).thenReturn(List.of(siteDto(1L, "Site 1", "Bruxelles")));

        mockMvc.perform(get("/api/v1/admin/sites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].terrains[0].siteId").value(1))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    private AdminSiteConsultationDto siteDto(Long id, String nom, String ville) {
        return new AdminSiteConsultationDto(
                id,
                nom,
                ville,
                Set.of(DayOfWeek.MONDAY),
                List.of(new TerrainDto(id * 10, "Terrain A", id)),
                List.of(new HoraireSiteDto(id * 100, id, 2026, LocalTime.of(8, 0), LocalTime.of(22, 0)))
        );
    }
}
