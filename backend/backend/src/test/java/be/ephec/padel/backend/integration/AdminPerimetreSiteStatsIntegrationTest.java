package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.model.entities.Site;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // IMPORTANT : c’est cette propriété que lit ServiceAutorisationAdmin
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class AdminPerimetreSiteStatsIntegrationTest extends SqlServerTestContainerConfig {

    @Autowired private MockMvc mockMvc;
    @Autowired private SiteRepository siteRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void preparerDonnees() {
        // Nettoyage dans l'ordre des FK (enfants -> parents)
        jdbcTemplate.execute("DELETE FROM participation");
        jdbcTemplate.execute("DELETE FROM paiement");
        jdbcTemplate.execute("DELETE FROM match_padel");
        jdbcTemplate.execute("DELETE FROM joueur");
        jdbcTemplate.execute("DELETE FROM terrain");
        jdbcTemplate.execute("DELETE FROM site");

        // Reset identity (si tables en IDENTITY)
        jdbcTemplate.execute("DBCC CHECKIDENT ('site', RESEED, 0)");
        jdbcTemplate.execute("DBCC CHECKIDENT ('terrain', RESEED, 0)");
        jdbcTemplate.execute("DBCC CHECKIDENT ('match_padel', RESEED, 0)");
        jdbcTemplate.execute("DBCC CHECKIDENT ('paiement', RESEED, 0)");
        jdbcTemplate.execute("DBCC CHECKIDENT ('participation', RESEED, 0)");

        Site site1 = new Site();
        site1.setNom("Site 1");
        site1.setVille("Bruxelles");
        siteRepository.save(site1);

        Site site2 = new Site();
        site2.setNom("Site 2");
        site2.setVille("Bruxelles");
        siteRepository.save(site2);
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_peutAcceder_stats_de_son_site_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/stats/dettes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/1/stats/ca")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/1/stats/matchs")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_nePeutPasAcceder_stats_autre_site_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/stats/dettes"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/sites/2/stats/ca")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/sites/2/stats/matchs")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminGlobal_peutAcceder_stats_de_tous_les_sites_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/stats/dettes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/2/stats/dettes"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/2/stats/ca")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/sites/2/stats/matchs")
                        .param("from", "2026-03-02")
                        .param("to", "2026-03-03"))
                .andExpect(status().isOk());
    }
}