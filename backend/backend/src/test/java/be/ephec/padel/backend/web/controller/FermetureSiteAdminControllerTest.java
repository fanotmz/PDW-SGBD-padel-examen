package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.FermetureSiteRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class FermetureSiteAdminControllerTest extends SqlServerTestContainerConfig {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Autowired SiteRepository siteRepository;
    @Autowired FermetureSiteRepository fermetureSiteRepository;

    @Autowired TerrainRepository terrainRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaiementRepository paiementRepository;
    @Autowired MouvementSoldeRepository mouvementSoldeRepository;

    private Site site;

    @BeforeEach
    void cleanAndSeed() {
        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        fermetureSiteRepository.deleteAll();
        terrainRepository.deleteAll();
        siteRepository.deleteAll();

        try {
            jdbcTemplate.execute("DBCC CHECKIDENT ('fermeture_site', RESEED, 0)");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("DBCC CHECKIDENT ('site', RESEED, 0)");
        } catch (Exception ignored) {
        }

        site = new Site("Site Admin Test", "Bruxelles");
        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);
    }

    @Test
    void public_ne_peut_pas_lister_401() throws Exception {
        mvc.perform(get("/api/v1/admin/sites/" + site.getId() + "/fermetures"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_creer_date_401() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_creer_periode_401() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-05-01",
                                  "dateFin": "2026-05-03",
                                  "motif": "Travaux"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_cree_fermeture_date_unique_201() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        containsString("/api/v1/admin/sites/" + site.getId() + "/fermetures/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.siteId").value(site.getId()))
                .andExpect(jsonPath("$.date").value("2026-04-10"))
                .andExpect(jsonPath("$.dateDebut").doesNotExist())
                .andExpect(jsonPath("$.dateFin").doesNotExist())
                .andExpect(jsonPath("$.motif").value("Congé exceptionnel"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SITE", username = "adminSite1")
    void post_cree_fermeture_periode_201() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-05-01",
                                  "dateFin": "2026-05-03",
                                  "motif": "Travaux"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.siteId").value(site.getId()))
                .andExpect(jsonPath("$.date").doesNotExist())
                .andExpect(jsonPath("$.dateDebut").value("2026-05-01"))
                .andExpect(jsonPath("$.dateFin").value("2026-05-03"))
                .andExpect(jsonPath("$.motif").value("Travaux"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void get_list_retourne_les_fermetures_du_site() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-05-01",
                                  "dateFin": "2026-05-03",
                                  "motif": "Travaux"
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/admin/sites/" + site.getId() + "/fermetures"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void get_one_retourne_la_fermeture() throws Exception {
        String location = mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.siteId").value(site.getId()))
                .andExpect(jsonPath("$.date").value("2026-04-10"))
                .andExpect(jsonPath("$.motif").value("Congé exceptionnel"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void put_modifie_la_fermeture_en_periode_200() throws Exception {
        String location = mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(put(location + "/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-06-10",
                                  "dateFin": "2026-06-12",
                                  "motif": "Travaux prolongés"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").doesNotExist())
                .andExpect(jsonPath("$.dateDebut").value("2026-06-10"))
                .andExpect(jsonPath("$.dateFin").value("2026-06-12"))
                .andExpect(jsonPath("$.motif").value("Travaux prolongés"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void put_modifie_la_fermeture_en_date_200() throws Exception {
        String location = mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-05-01",
                                  "dateFin": "2026-05-03",
                                  "motif": "Travaux"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(put(location + "/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-07-15",
                                  "motif": "Jour unique"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date").value("2026-07-15"))
                .andExpect(jsonPath("$.dateDebut").doesNotExist())
                .andExpect(jsonPath("$.dateFin").doesNotExist())
                .andExpect(jsonPath("$.motif").value("Jour unique"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void delete_supprime_la_fermeture_204() throws Exception {
        String location = mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "Congé exceptionnel"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/admin/sites/" + site.getId() + "/fermetures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_date_refuse_si_date_absente() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motif": "Invalide"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("Date obligatoire")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_periode_refuse_si_dates_absentes() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "motif": "Invalide"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("date de début et une date de fin")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_periode_refuse_si_date_fin_avant_date_debut() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateDebut": "2026-05-10",
                                  "dateFin": "2026-05-01",
                                  "motif": "Invalide"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("date de fin doit être après ou égale")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_date_refuse_doublon_date_unique() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "A"
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-04-10",
                                  "motif": "B"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("existe déjà")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void get_one_404_si_introuvable() throws Exception {
        mvc.perform(get("/api/v1/admin/sites/" + site.getId() + "/fermetures/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("introuvable")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SITE", username = "adminSite1")
    void admin_site_hors_perimetre_refuse_403() throws Exception {
        Site autreSite = new Site("Autre site", "Liège");
        autreSite.setJoursFermeture(Set.of());
        autreSite = siteRepository.save(autreSite);

        mvc.perform(post("/api/v1/admin/sites/" + autreSite.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-08-01",
                                  "motif": "Interdit"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_periode_refuse_doublon_exact() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-01",
                              "dateFin": "2026-05-03",
                              "motif": "Travaux"
                            }
                            """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-01",
                              "dateFin": "2026-05-03",
                              "motif": "Travaux bis"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("existe déjà pour cette période")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_date_refuse_si_date_couverte_par_periode() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-01",
                              "dateFin": "2026-05-03",
                              "motif": "Travaux"
                            }
                            """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "date": "2026-05-02",
                              "motif": "Jour isolé"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("déjà couverte par une période")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_periode_refuse_si_chevauche_une_autre_periode() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-01",
                              "dateFin": "2026-05-05",
                              "motif": "Travaux 1"
                            }
                            """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-04",
                              "dateFin": "2026-05-08",
                              "motif": "Travaux 2"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("chevauche une autre fermeture")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_periode_refuse_si_contient_date_unique_existante() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "date": "2026-05-02",
                              "motif": "Jour unique"
                            }
                            """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/periode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "dateDebut": "2026-05-01",
                              "dateFin": "2026-05-03",
                              "motif": "Travaux"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("contient déjà une date de fermeture")));
    }
}