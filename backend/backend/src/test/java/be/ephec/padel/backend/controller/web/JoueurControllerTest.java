package be.ephec.padel.backend.controller.web;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.JoueurController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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

    @Test
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
    void create_validation_400_si_body_invalide() throws Exception {
        mvc.perform(post("/api/v1/joueurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}