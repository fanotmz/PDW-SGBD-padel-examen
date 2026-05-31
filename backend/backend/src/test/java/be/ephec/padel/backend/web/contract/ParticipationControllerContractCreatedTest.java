package be.ephec.padel.backend.web.contract;

import be.ephec.padel.backend.controller.ParticipationController;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipationController.class)
class ParticipationControllerContractCreatedTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ParticipationService participationService;

    @Test
    @WithMockUser
    void rejoindrePublic_returns201_andLocationHeader() throws Exception {
        Participation p = new Participation();
        when(participationService.rejoindreEtPayerMatchPublic(eq(1L)))
                .thenReturn(p);

        mockMvc.perform(post("/api/v1/matchs/1/participants/public")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/matchs/1"));
    }
}
