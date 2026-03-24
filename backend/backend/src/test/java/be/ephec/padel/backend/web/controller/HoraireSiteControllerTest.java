package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.HoraireSiteController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.service.HoraireSiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HoraireSiteController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class HoraireSiteControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    HoraireSiteService horaireSiteService;

    private HoraireSite horaire(Long id, Long siteId, int annee, int ouverture, int fermeture) {
        Site site = mock(Site.class);
        when(site.getId()).thenReturn(siteId);

        HoraireSite h = mock(HoraireSite.class);
        when(h.getId()).thenReturn(id);
        when(h.getSite()).thenReturn(site);
        when(h.getAnnee()).thenReturn(annee);
        when(h.getHeureOuverture()).thenReturn(LocalTime.of(ouverture, 0));
        when(h.getHeureFermeture()).thenReturn(LocalTime.of(fermeture, 0));

        return h;
    }

    @Test
    void public_ne_peut_pas_lister_401() throws Exception {
        mvc.perform(get("/api/v1/admin/sites/1/horaires"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_creer_401() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/1/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026,
                                  "heureOuverture": "08:00:00",
                                  "heureFermeture": "22:00:00"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_modifier_401() throws Exception {
        mvc.perform(put("/api/v1/admin/sites/1/horaires/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_supprimer_401() throws Exception {
        mvc.perform(delete("/api/v1/admin/sites/1/horaires/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void list_ok_200() throws Exception {
        HoraireSite h1 = horaire(10L, 1L, 2026, 8, 22);
        HoraireSite h2 = horaire(11L, 1L, 2027, 9, 21);

        when(horaireSiteService.listBySite(1L)).thenReturn(List.of(h1, h2));

        mvc.perform(get("/api/v1/admin/sites/1/horaires"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].siteId").value(1))
                .andExpect(jsonPath("$[0].annee").value(2026))
                .andExpect(jsonPath("$[0].heureOuverture").value("08:00:00"))
                .andExpect(jsonPath("$[0].heureFermeture").value("22:00:00"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].siteId").value(1))
                .andExpect(jsonPath("$[1].annee").value(2027))
                .andExpect(jsonPath("$[1].heureOuverture").value("09:00:00"))
                .andExpect(jsonPath("$[1].heureFermeture").value("21:00:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void getByAnnee_ok_200() throws Exception {
        HoraireSite h = horaire(10L, 1L, 2026, 8, 22);
        when(horaireSiteService.getBySiteAndAnnee(1L, 2026)).thenReturn(h);

        mvc.perform(get("/api/v1/admin/sites/1/horaires/2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.siteId").value(1))
                .andExpect(jsonPath("$.annee").value(2026))
                .andExpect(jsonPath("$.heureOuverture").value("08:00:00"))
                .andExpect(jsonPath("$.heureFermeture").value("22:00:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void getByAnnee_notFound_404() throws Exception {
        when(horaireSiteService.getBySiteAndAnnee(1L, 2026))
                .thenThrow(new NotFoundException("Aucun horaire trouvé pour le site 1 en 2026."));

        mvc.perform(get("/api/v1/admin/sites/1/horaires/2026"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Aucun horaire trouvé pour le site 1 en 2026."));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_ok_201() throws Exception {
        HoraireSite created = horaire(10L, 1L, 2026, 8, 22);
        when(horaireSiteService.create(anyLong(), any())).thenReturn(created);

        mvc.perform(post("/api/v1/admin/sites/1/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026,
                                  "heureOuverture": "08:00:00",
                                  "heureFermeture": "22:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/admin/sites/1/horaires/2026"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.siteId").value(1))
                .andExpect(jsonPath("$.annee").value(2026))
                .andExpect(jsonPath("$.heureOuverture").value("08:00:00"))
                .andExpect(jsonPath("$.heureFermeture").value("22:00:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_validation_400_si_champs_manquants() throws Exception {
        mvc.perform(post("/api/v1/admin/sites/1/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_business_400_si_doublon() throws Exception {
        when(horaireSiteService.create(anyLong(), any()))
                .thenThrow(new BusinessException("Un horaire existe déjà pour ce site et cette année."));

        mvc.perform(post("/api/v1/admin/sites/1/horaires")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2026,
                                  "heureOuverture": "08:00:00",
                                  "heureFermeture": "22:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Un horaire existe déjà pour ce site et cette année."));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void update_ok_200() throws Exception {
        HoraireSite updated = horaire(10L, 1L, 2027, 9, 21);
        when(horaireSiteService.update(anyLong(), anyLong(), any())).thenReturn(updated);

        mvc.perform(put("/api/v1/admin/sites/1/horaires/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.siteId").value(1))
                .andExpect(jsonPath("$.annee").value(2027))
                .andExpect(jsonPath("$.heureOuverture").value("09:00:00"))
                .andExpect(jsonPath("$.heureFermeture").value("21:00:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void update_validation_400_si_champs_manquants() throws Exception {
        mvc.perform(put("/api/v1/admin/sites/1/horaires/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void update_notFound_404() throws Exception {
        when(horaireSiteService.update(anyLong(), anyLong(), any()))
                .thenThrow(new NotFoundException("Horaire introuvable"));

        mvc.perform(put("/api/v1/admin/sites/1/horaires/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Horaire introuvable"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void update_business_400_si_collision() throws Exception {
        when(horaireSiteService.update(anyLong(), anyLong(), any()))
                .thenThrow(new BusinessException("Un autre horaire existe déjà pour ce site et cette année."));

        mvc.perform(put("/api/v1/admin/sites/1/horaires/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "annee": 2027,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Un autre horaire existe déjà pour ce site et cette année."));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void delete_ok_204() throws Exception {
        doNothing().when(horaireSiteService).delete(1L, 10L);

        mvc.perform(delete("/api/v1/admin/sites/1/horaires/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void delete_notFound_404() throws Exception {
        doThrow(new NotFoundException("Horaire introuvable"))
                .when(horaireSiteService).delete(1L, 10L);

        mvc.perform(delete("/api/v1/admin/sites/1/horaires/10"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Horaire introuvable"));
    }
}