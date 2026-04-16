package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class AdminPerimetreSiteHorairesIntegrationTest extends SqlServerTestContainerConfig {

    @Autowired private MockMvc mockMvc;
    @Autowired private SiteRepository siteRepository;
    @Autowired private HoraireSiteRepository horaireSiteRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private HoraireSite horaireSite2;

    @BeforeEach
    void preparerDonnees() {
        jdbcTemplate.execute("DELETE FROM horaire_site");
        jdbcTemplate.execute("DELETE FROM site");
        jdbcTemplate.execute("DBCC CHECKIDENT ('horaire_site', RESEED, 0)");
        jdbcTemplate.execute("DBCC CHECKIDENT ('site', RESEED, 0)");

        Site site1 = new Site();
        site1.setNom("Site 1");
        site1.setVille("Bruxelles");
        site1 = siteRepository.save(site1);

        Site site2 = new Site();
        site2.setNom("Site 2");
        site2.setVille("Bruxelles");
        site2 = siteRepository.save(site2);

        horaireSiteRepository.save(new HoraireSite(
                site1,
                2026,
                LocalTime.of(8, 0),
                LocalTime.of(22, 0)
        ));
        horaireSite2 = horaireSiteRepository.save(new HoraireSite(
                site2,
                2026,
                LocalTime.of(9, 0),
                LocalTime.of(21, 0)
        ));
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_peut_lire_les_horaires_de_son_site() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/horaires"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/1/horaires/2026"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_ne_peut_pas_lire_les_horaires_d_un_autre_site() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/horaires"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/sites/2/horaires/2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_ne_peut_pas_creer_modifier_ou_supprimer_un_horaire_hors_perimetre() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sites/2/horaires")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "10:00:00",
                                  "heureFermeture": "20:00:00"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/sites/2/horaires/" + horaireSite2.getId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026,
                                  "heureOuverture": "10:00:00",
                                  "heureFermeture": "20:00:00"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/sites/2/horaires/" + horaireSite2.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_recoit_400_si_le_site_est_autorise_mais_pas_le_horaire_cible() throws Exception {
        mockMvc.perform(put("/api/v1/admin/sites/1/horaires/" + horaireSite2.getId())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026,
                                  "heureOuverture": "10:00:00",
                                  "heureFermeture": "20:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Horaire non")));

        mockMvc.perform(delete("/api/v1/admin/sites/1/horaires/" + horaireSite2.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Horaire non")));
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminGlobal_garde_l_acces_global_aux_horaires() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/horaires"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/sites/2/horaires")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "10:00:00",
                                  "heureFermeture": "20:00:00"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
