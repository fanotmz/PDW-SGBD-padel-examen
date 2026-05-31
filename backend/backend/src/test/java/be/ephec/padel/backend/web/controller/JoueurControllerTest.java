package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.JoueurController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = JoueurController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(username = "joueur1", roles = "JOUEUR")
class JoueurControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JoueurService joueurService;

    private Joueur realJoueur(String matricule, String nom, TypeJoueur type) {
        return new Joueur(matricule, nom, type);
    }

    @Test
    @WithAnonymousUser
    void public_ne_peut_pas_lister_401() throws Exception {
        mvc.perform(get("/api/v1/joueurs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
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
}
