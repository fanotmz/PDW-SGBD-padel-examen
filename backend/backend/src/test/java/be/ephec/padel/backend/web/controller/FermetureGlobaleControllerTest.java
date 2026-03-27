package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FermetureGlobaleControllerTest extends SqlServerTestContainerConfig {

    @Autowired MockMvc mvc;
    @Autowired FermetureGlobaleRepository fermetureGlobaleRepository;

    @BeforeEach
    void clean() {
        fermetureGlobaleRepository.deleteAll();
    }

    // ===== Issue 62 : public refusé sur WRITE =====

    @Test
    void public_ne_peut_pas_creer_401() throws Exception {
        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-15\",\"motif\":\"Maintenance\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void public_ne_peut_pas_supprimer_401() throws Exception {
        mvc.perform(delete("/api/v1/fermetures-globales/1"))
                .andExpect(status().isUnauthorized());
    }

    // GET reste public
    @Test
    @WithMockUser(username = "joueur1", roles = "JOUEUR")
    void get_list_public_ok_200() throws Exception {
        mvc.perform(get("/api/v1/fermetures-globales"))
                .andExpect(status().isOk());
    }

    // ===== Admin global : WRITE autorisé =====

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_cree_fermeture_globale_et_get_list_la_retourne() throws Exception {
        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-15\",\"motif\":\"Maintenance\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/fermetures-globales/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.date").value("2026-03-15"))
                .andExpect(jsonPath("$.motif").value("Maintenance"));

        mvc.perform(get("/api/v1/fermetures-globales"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].date").value("2026-03-15"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void post_refuse_doublon_date() throws Exception {
        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-15\",\"motif\":\"A\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-15\",\"motif\":\"B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("existe déjà")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void delete_supprime_et_retourne_204() throws Exception {
        String location = mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-03-15\",\"motif\":\"Maintenance\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(delete("/api/v1/fermetures-globales/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/fermetures-globales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
