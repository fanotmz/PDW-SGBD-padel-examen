package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.MatchController;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private PublicMatchSummaryDto samplePublicSummary(Long id) {
        return new PublicMatchSummaryDto(
                id,
                LocalDate.of(2030, 1, 1),
                LocalTime.of(10, 0),
                5L,
                "Site Delta",
                10L,
                "Terrain 1",
                "G0001",
                2,
                2,
                false,
                new BigDecimal("15.00")
        );
    }

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
                .andExpect(jsonPath("$.montantTotal").value(60.0))
                .andExpect(jsonPath("$.montantPaye").value(15.0))
                .andExpect(jsonPath("$.resteAPayer").value(45.0));
    }

    @Test
    void getOne_notFound_404() throws Exception {
        when(matchPadelService.getMatchDto(99L))
                .thenThrow(new NotFoundException("Match introuvable"));

        mvc.perform(get("/api/v1/matchs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_sansAuth_201_location_et_body() throws Exception {
        MatchPadel created = mock(MatchPadel.class);
        when(created.getId()).thenReturn(123L);

        when(matchPadelService.creerMatch(anyLong(), anyString(), any(), any()))
                .thenReturn(created);
        when(matchPadelService.getMatchDto(123L))
                .thenReturn(sampleDto(123L));

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

    @Test
    void getPublicMatches_sansAuth_200_et_json() throws Exception {
        when(matchPadelService.getPublicMatchSummaries(null, null, null))
                .thenReturn(List.of(samplePublicSummary(1L), samplePublicSummary(2L)));

        mvc.perform(get("/api/v1/matchs/public"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].dateDebut").value("2030-01-01"))
                .andExpect(jsonPath("$[0].heureDebut").value("10:00:00"))
                .andExpect(jsonPath("$[0].siteNom").value("Site Delta"))
                .andExpect(jsonPath("$[0].terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$[0].nbParticipants").value(2))
                .andExpect(jsonPath("$[0].placesRestantes").value(2))
                .andExpect(jsonPath("$[0].complet").value(false))
                .andExpect(jsonPath("$[0].montantParJoueur").value(15.0));
    }

    @Test
    void getPublicMatches_avecFiltres_transmis_au_service() throws Exception {
        LocalDate from = LocalDate.of(2030, 1, 1);
        LocalDate to = LocalDate.of(2030, 1, 31);

        when(matchPadelService.getPublicMatchSummaries(from, to, 5L))
                .thenReturn(List.of());

        mvc.perform(get("/api/v1/matchs/public")
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-31")
                        .param("siteId", "5"))
                .andExpect(status().isOk());

        verify(matchPadelService).getPublicMatchSummaries(from, to, 5L);
    }

    @Test
    void getPublicMatches_badRequest_si_intervalle_invalide() throws Exception {
        when(matchPadelService.getPublicMatchSummaries(
                eq(LocalDate.of(2030, 2, 1)),
                eq(LocalDate.of(2030, 1, 1)),
                eq(null)
        )).thenThrow(new BusinessException(
                "Le paramètre 'from' doit être antérieur ou égal à 'to'."
        ));

        mvc.perform(get("/api/v1/matchs/public")
                        .param("from", "2030-02-01")
                        .param("to", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }
}