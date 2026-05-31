package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class AdminPerimetreSiteIntegrationTest extends SqlServerTestContainerConfig {

    @Autowired private MockMvc mockMvc;
    @Autowired private SiteRepository siteRepository;
    @Autowired private JoueurRepository joueurRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void preparerDonnees() {
        jdbcTemplate.execute("DELETE FROM horaire_site");
        joueurRepository.deleteAll();
        siteRepository.deleteAll();

        try {
            jdbcTemplate.execute("DBCC CHECKIDENT ('horaire_site', RESEED, 0)");
            jdbcTemplate.execute("DBCC CHECKIDENT ('site', RESEED, 0)");
        } catch (Exception ignored) {
        }

        Site site1 = new Site();
        site1.setNom("Site 1");
        site1.setVille("Bruxelles");
        site1 = siteRepository.save(site1);

        Site site2 = new Site();
        site2.setNom("Site 2");
        site2.setVille("Bruxelles");
        site2 = siteRepository.save(site2);

        Joueur j1 = new Joueur("J001", "Dupont", TypeJoueur.SITE, site1);
        j1.setSolde(new BigDecimal("15.00"));
        j1.setPenaliteJusqua(LocalDateTime.of(2030, 1, 10, 12, 30));
        joueurRepository.save(j1);

        Joueur j2 = new Joueur("J002", "Martin", TypeJoueur.SITE, site2);
        j2.setSolde(new BigDecimal("0.00"));
        joueurRepository.save(j2);
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_peutAcceder_aSonSite_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/1/joueurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matricule").value("J001"))
                .andExpect(jsonPath("$[0].penaliteJusqua").value("2030-01-10T12:30:00"));
    }

    @Test
    @WithMockUser(username = "adminSite1", roles = {"ADMIN_SITE"})
    void adminSite1_nePeutPasAcceder_autreSite_403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/joueurs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminGlobal", roles = {"ADMIN_GLOBAL"})
    void adminGlobal_peutAcceder_aTousLesSites_200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sites/2/joueurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matricule").value("J002"))
                .andExpect(jsonPath("$[0].penaliteJusqua").value(nullValue()));
    }
}
