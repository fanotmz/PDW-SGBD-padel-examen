package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.TerrainController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.service.TerrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TerrainController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(username = "joueur1", roles = "JOUEUR")
class TerrainControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TerrainService terrainService;

    private Terrain terrain(Long id, String nom, Long siteId) {
        Site s = new Site("SiteTest", "VilleTest");
        ReflectionTestUtils.setField(s, "id", siteId);

        Terrain t = new Terrain(nom, s);
        ReflectionTestUtils.setField(t, "id", id);

        return t;
    }

    @Test
    void list_sansSiteId_200() throws Exception {
        when(terrainService.lister()).thenReturn(List.of(
                terrain(1L, "T1", 10L),
                terrain(2L, "T2", 10L)
        ));

        mvc.perform(get("/api/v1/terrains"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("T1"))
                .andExpect(jsonPath("$[0].siteId").value(10))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nom").value("T2"))
                .andExpect(jsonPath("$[1].siteId").value(10));
    }

    @Test
    void list_avecSiteId_200() throws Exception {
        when(terrainService.listerParSite(10L)).thenReturn(List.of(
                terrain(3L, "T3", 10L)
        ));

        mvc.perform(get("/api/v1/terrains").param("siteId", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].nom").value("T3"))
                .andExpect(jsonPath("$[0].siteId").value(10));
    }

    @Test
    void getOne_ok_200() throws Exception {
        when(terrainService.getTerrain(5L)).thenReturn(terrain(5L, "Central-1", 99L));

        mvc.perform(get("/api/v1/terrains/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nom").value("Central-1"))
                .andExpect(jsonPath("$.siteId").value(99));
    }

    @Test
    void getOne_notFound_404() throws Exception {
        when(terrainService.getTerrain(999L)).thenThrow(new NotFoundException("Terrain introuvable"));

        mvc.perform(get("/api/v1/terrains/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Terrain introuvable"));
    }

    @Test
    @WithAnonymousUser
    void public_ne_peut_pas_creer_terrain_401() throws Exception {
        mvc.perform(post("/api/v1/terrains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"T1\",\"siteId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_ok_201_location_et_body() throws Exception {
        when(terrainService.creerTerrain(eq("T1"), eq(10L)))
                .thenReturn(terrain(123L, "T1", 10L));

        mvc.perform(post("/api/v1/terrains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"T1\",\"siteId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/terrains/123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.nom").value("T1"))
                .andExpect(jsonPath("$.siteId").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_validation_400_si_body_invalide() throws Exception {
        mvc.perform(post("/api/v1/terrains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_businessException_400() throws Exception {
        when(terrainService.creerTerrain(eq("T1"), eq(10L)))
                .thenThrow(new BusinessException("Terrain déjà existant"));

        mvc.perform(post("/api/v1/terrains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"T1\",\"siteId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Terrain déjà existant"));
    }
}
