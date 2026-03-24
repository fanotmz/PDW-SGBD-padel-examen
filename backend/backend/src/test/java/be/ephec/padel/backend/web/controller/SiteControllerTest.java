package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.SiteController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.service.SiteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SiteController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class SiteControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SiteService siteService;

    @Test
    void list_ok_200_jsonArray() throws Exception {
        Site s1 = org.mockito.Mockito.mock(Site.class);
        when(s1.getId()).thenReturn(1L);
        when(s1.getNom()).thenReturn("Site A");
        when(s1.getVille()).thenReturn("Bruxelles");

        Site s2 = org.mockito.Mockito.mock(Site.class);
        when(s2.getId()).thenReturn(2L);
        when(s2.getNom()).thenReturn("Site B");
        when(s2.getVille()).thenReturn("Liège");

        when(siteService.lister()).thenReturn(java.util.List.of(s1, s2));

        mvc.perform(get("/api/v1/sites"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Site A"))
                .andExpect(jsonPath("$[0].ville").value("Bruxelles"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nom").value("Site B"))
                .andExpect(jsonPath("$[1].ville").value("Liège"));
    }

    @Test
    void getOne_ok_200_json() throws Exception {
        Site s = org.mockito.Mockito.mock(Site.class);
        when(s.getId()).thenReturn(10L);
        when(s.getNom()).thenReturn("Central");
        when(s.getVille()).thenReturn("Namur");

        when(siteService.getSite(10L)).thenReturn(s);

        mvc.perform(get("/api/v1/sites/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nom").value("Central"))
                .andExpect(jsonPath("$.ville").value("Namur"));
    }

    @Test
    void getOne_notFound_404() throws Exception {
        when(siteService.getSite(999L)).thenThrow(new NotFoundException("Site introuvable"));

        mvc.perform(get("/api/v1/sites/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Site introuvable"));
    }

    @Test
    void public_ne_peut_pas_creer_site_401() throws Exception {
        mvc.perform(post("/api/v1/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Nouveau Site",
                                  "ville": "Charleroi",
                                  "annee": 2026,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_ok_201_location_et_body() throws Exception {
        Site created = org.mockito.Mockito.mock(Site.class);
        when(created.getId()).thenReturn(123L);
        when(created.getNom()).thenReturn("Nouveau Site");
        when(created.getVille()).thenReturn("Charleroi");

        when(siteService.creerSite(
                anyString(),
                anyString(),
                anyInt(),
                any(LocalTime.class),
                any(LocalTime.class)
        )).thenReturn(created);

        mvc.perform(post("/api/v1/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Nouveau Site",
                                  "ville": "Charleroi",
                                  "annee": 2026,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sites/123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.nom").value("Nouveau Site"))
                .andExpect(jsonPath("$.ville").value("Charleroi"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_validation_400_nom_blank() throws Exception {
        mvc.perform(post("/api/v1/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "",
                                  "ville": "Mons",
                                  "annee": 2026,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_business_400_nom_deja_utilise() throws Exception {
        when(siteService.creerSite(
                "Dup",
                "Bruxelles",
                2026,
                LocalTime.of(9, 0),
                LocalTime.of(21, 0)
        )).thenThrow(new BusinessException("Nom de site déjà utilisé"));

        mvc.perform(post("/api/v1/sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Dup",
                                  "ville": "Bruxelles",
                                  "annee": 2026,
                                  "heureOuverture": "09:00:00",
                                  "heureFermeture": "21:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Nom de site déjà utilisé"));
    }
}