package be.ephec.padel.backend.controller.web;

import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.repository.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    @Test
    void post_cree_fermeture_globale_et_get_list_la_retourne() throws Exception {
        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2026-03-15", "motif": "Maintenance" }
                                """))
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
    void post_refuse_doublon_date() throws Exception {
        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-03-15\", \"motif\": \"A\" }"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": \"2026-03-15\", \"motif\": \"B\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("existe déjà")));
    }

    @Test
    void delete_supprime_et_retourne_204() throws Exception {
        String body = "{ \"date\": \"2026-03-15\", \"motif\": \"Maintenance\" }";

        String location = mvc.perform(post("/api/v1/fermetures-globales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        // Location = /api/v1/fermetures-globales/{id}
        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(delete("/api/v1/fermetures-globales/" + id))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/fermetures-globales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}