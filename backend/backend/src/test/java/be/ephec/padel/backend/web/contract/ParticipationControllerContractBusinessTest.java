package be.ephec.padel.backend.web.contract;

import be.ephec.padel.backend.controller.ParticipationController;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParticipationController.class)
class ParticipationControllerContractBusinessTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ParticipationService participationService;

    @Test
    @WithMockUser
    void rejoindreMatchPublic_businessException_returns400_withApiErrorFormat() throws Exception {
        when(participationService.rejoindreEtPayerMatchPublic(eq(1L), anyString()))
                .thenThrow(new BusinessException("Match complet"));

        String json = """
        { "joueurMatricule": "G0001" }
        """;

        mockMvc.perform(post("/api/v1/matchs/1/participants/public")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Match complet"))
                .andExpect(jsonPath("$.path").value("/api/v1/matchs/1/participants/public"));
    }
}