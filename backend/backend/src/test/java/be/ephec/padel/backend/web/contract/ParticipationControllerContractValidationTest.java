package be.ephec.padel.backend.web.contract;

import be.ephec.padel.backend.controller.ParticipationController;
import be.ephec.padel.backend.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParticipationController.class)
class ParticipationControllerContractValidationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ParticipationService participationService;

    @Test
    @WithMockUser
    void rejoindrePublic_invalidBody_returns400_withDetails() throws Exception {
        // joueurMatricule vide + montant null (ou absent) -> doit déclencher @Valid
        String invalidJson = """
        { "joueurMatricule": "" }
        """;

        mockMvc.perform(post("/api/v1/matchs/1/participants/public")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/matchs/1/participants/public"))
                .andExpect(jsonPath("$.details").exists());
    }
}