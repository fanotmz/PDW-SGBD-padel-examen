package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.JoueurController;
import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JoueurController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class JoueurControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JoueurService joueurService;

    private Joueur realJoueur(String matricule, String nom, TypeJoueur type) {
        return new Joueur(matricule, nom, type);
    }


    // ===== Issue 62 : public doit être refusé sur listing / création =====

    @Test
    void public_ne_peut_pas_lister_401() throws Exception {
        mvc.perform(get("/api/v1/joueurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_creer_401() throws Exception {
        mvc.perform(post("/api/v1/joueurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matricule": "G0001",
                                  "nom": "Alice",
                                  "type": "GLOBAL",
                                  "siteId": null
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ===== Admin global : listing / création autorisés =====

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void list_ok_200_jsonArray() throws Exception {
        when(joueurService.lister()).thenReturn(List.of(
                realJoueur("G0001", "Alice", TypeJoueur.GLOBAL),
                realJoueur("L0002", "Bob", TypeJoueur.LIBRE)
        ));

        mvc.perform(get("/api/v1/joueurs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].matricule").value("G0001"))
                .andExpect(jsonPath("$[0].nom").value("Alice"))
                .andExpect(jsonPath("$[0].type").value("GLOBAL"))
                .andExpect(jsonPath("$[1].matricule").value("L0002"))
                .andExpect(jsonPath("$[1].nom").value("Bob"))
                .andExpect(jsonPath("$[1].type").value("LIBRE"));
    }

    // ===== Endpoints publics conservés (pas d'auth "joueur" dans le projet) =====

    @Test
    void getOne_ok_200() throws Exception {
        when(joueurService.getJoueur("G0001"))
                .thenReturn(realJoueur("G0001", "Alice", TypeJoueur.GLOBAL));

        mvc.perform(get("/api/v1/joueurs/G0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matricule").value("G0001"))
                .andExpect(jsonPath("$.nom").value("Alice"))
                .andExpect(jsonPath("$.type").value("GLOBAL"));
    }

    @Test
    void getOne_notFound_404() throws Exception {
        when(joueurService.getJoueur("G9999"))
                .thenThrow(new NotFoundException("Joueur introuvable"));

        mvc.perform(get("/api/v1/joueurs/G9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dette_false_200() throws Exception {
        when(joueurService.aDette("G0001")).thenReturn(false);

        mvc.perform(get("/api/v1/joueurs/G0001/dette"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dette").value(false));
    }

    @Test
    void dette_true_200() throws Exception {
        when(joueurService.aDette("G0001")).thenReturn(true);

        mvc.perform(get("/api/v1/joueurs/G0001/dette"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dette").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_ok_201_location_et_body() throws Exception {
        Joueur created = realJoueur("G0001", "Alice", TypeJoueur.GLOBAL);

        when(joueurService.creerJoueur(eq("G0001"), eq("Alice"), eq(TypeJoueur.GLOBAL), isNull()))
                .thenReturn(created);

        mvc.perform(post("/api/v1/joueurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "matricule": "G0001",
                                  "nom": "Alice",
                                  "type": "GLOBAL",
                                  "siteId": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/joueurs/G0001"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matricule").value("G0001"))
                .andExpect(jsonPath("$.nom").value("Alice"))
                .andExpect(jsonPath("$.type").value("GLOBAL"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void create_validation_400_si_body_invalide() throws Exception {
        mvc.perform(post("/api/v1/joueurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void getPlayerMatches_retourne200_etListeVide() throws Exception {
        when(joueurService.getPlayerMatches("G0001")).thenReturn(List.of());

        mvc.perform(get("/api/v1/joueurs/G0001/matchs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void getPlayerMatches_retourne200_etListeDeMatchs() throws Exception {
        PlayerMatchSummaryDto dto = new PlayerMatchSummaryDto(
                1L,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                100L,
                "Site Delta",
                200L,
                "Terrain 1",
                MatchVisibilite.PUBLIC,
                PlayerMatchRoleDto.PARTICIPANT,
                MatchTemporalStatusDto.FUTUR,
                5,
                false
        );

        when(joueurService.getPlayerMatches("G0001")).thenReturn(List.of(dto));

        mvc.perform(get("/api/v1/joueurs/G0001/matchs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].siteId").value(100))
                .andExpect(jsonPath("$[0].siteNom").value("Site Delta"))
                .andExpect(jsonPath("$[0].terrainId").value(200))
                .andExpect(jsonPath("$[0].terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$[0].visibilite").value("PUBLIC"))
                .andExpect(jsonPath("$[0].roleJoueur").value("PARTICIPANT"))
                .andExpect(jsonPath("$[0].statutTemporel").value("FUTUR"))
                .andExpect(jsonPath("$[0].joursAvantMatch").value(5))
                .andExpect(jsonPath("$[0].paiementJoueurEffectue").value(false));
    }
    @Test
    void getPlayerMatches_joueurIntrouvable_retourne404() throws Exception {
        when(joueurService.getPlayerMatches("G9999"))
                .thenThrow(new NotFoundException("Joueur introuvable"));

        mvc.perform(get("/api/v1/joueurs/G9999/matchs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}