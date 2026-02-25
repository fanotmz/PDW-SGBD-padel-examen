package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.service.MatchPadelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatchController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class MatchControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MatchPadelService matchPadelService;

    private MatchDto sampleDto(Long id) {
        return new MatchDto(
                id,
                10L,
                "T1",
                5L,
                "G0001",
                LocalDateTime.now().plusDays(1),
                MatchVisibilite.PUBLIC,
                2,
                new BigDecimal("60.00"),
                new BigDecimal("15.00"),
                new BigDecimal("45.00")
        );
    }

    // ----------------
    // GET /api/v1/matchs/{id}  (PUBLIC car /api/v1/** permitAll)
    // ----------------

    @Test
    void getOne_sansAuth_200_et_json() throws Exception {
        when(matchPadelService.getMatchDto(1L)).thenReturn(sampleDto(1L));

        mvc.perform(get("/api/v1/matchs/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.terrainId").value(10))
                .andExpect(jsonPath("$.terrainNom").value("T1"))
                .andExpect(jsonPath("$.siteId").value(5))
                .andExpect(jsonPath("$.organisateurMatricule").value("G0001"))
                .andExpect(jsonPath("$.visibilite").value("PUBLIC"))
                .andExpect(jsonPath("$.nbParticipants").value(2))
                // ✅ plus robuste: on compare la valeur numérique, pas le format (60 vs 60.0 vs 60.00)
                .andExpect(jsonPath("$.montantTotal").value(60.0))
                .andExpect(jsonPath("$.montantPaye").value(15.0))
                .andExpect(jsonPath("$.resteAPayer").value(45.0));
    }

    @Test
    void getOne_notFound_404() throws Exception {
        when(matchPadelService.getMatchDto(99L)).thenThrow(new NotFoundException("Match introuvable"));

        mvc.perform(get("/api/v1/matchs/99"))
                .andExpect(status().isNotFound());
    }

    // ----------------
    // POST /api/v1/matchs  (PUBLIC car /api/v1/** permitAll)
    // ----------------

    @Test
    void create_sansAuth_201_location_et_body() throws Exception {
        // mock création -> renvoie MatchPadel avec id
        MatchPadel created = org.mockito.Mockito.mock(MatchPadel.class);
        when(created.getId()).thenReturn(123L);

        when(matchPadelService.creerMatch(any(), any(), any(), any())).thenReturn(created);
        when(matchPadelService.getMatchDto(123L)).thenReturn(sampleDto(123L));

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "terrainId": 1,
                                  "organisateurMatricule": "G0001",
                                  "dateDebut": "2030-01-01T10:00:00",
                                  "visibilite": "PUBLIC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/matchs/123"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(123));
    }

    @Test
    void create_validation_400_si_body_invalide() throws Exception {
        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}