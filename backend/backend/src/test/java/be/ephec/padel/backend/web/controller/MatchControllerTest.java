package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.MatchController;
import be.ephec.padel.backend.dto.response.CreneauxMatchResponseDto;
import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.ParticipantDto;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.service.MatchPadelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
@WithMockUser(username = "joueur1", roles = "JOUEUR")
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
                MatchStatut.PLANIFIE,
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

    private MatchDetailDto sampleDetailDto(Long id, MatchVisibilite visibilite) {
        return new MatchDetailDto(
                id,
                LocalDate.of(2030, 1, 1),
                LocalTime.of(10, 0),
                5L,
                "Site Delta",
                10L,
                "Terrain 1",
                "G0001",
                "Organisateur",
                visibilite,
                MatchStatut.PLANIFIE,
                2,
                2,
                false,
                visibilite == MatchVisibilite.PRIVE,
                new BigDecimal("60.00"),
                new BigDecimal("15.00"),
                new BigDecimal("45.00"),
                BigDecimal.ZERO,
                List.of(
                        new ParticipantDto("G0001", "Organisateur"),
                        new ParticipantDto("J0001", "Alice")
                )
        );
    }

    @Test
    @WithAnonymousUser
    void create_sansAuth_401() throws Exception {
        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "terrainId": 1,
                                  "dateDebut": "2030-01-01T10:00:00",
                                  "visibilite": "PUBLIC"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_auth_201_location_et_body() throws Exception {
        MatchPadel created = mock(MatchPadel.class);
        when(created.getId()).thenReturn(123L);

        when(matchPadelService.creerMatch(anyLong(), any(), any()))
                .thenReturn(created);
        when(matchPadelService.getMatchDto(123L))
                .thenReturn(sampleDto(123L));

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "terrainId": 1,
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
    @WithAnonymousUser
    void getCreneaux_sansAuth_401() throws Exception {
        mvc.perform(get("/api/v1/matchs/creneaux")
                        .param("terrainId", "1")
                        .param("date", "2030-01-01"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCreneaux_auth_200_et_json() throws Exception {
        LocalDate date = LocalDate.of(2030, 1, 1);
        when(matchPadelService.getCreneauxDisponibles(1L, date))
                .thenReturn(new CreneauxMatchResponseDto(List.of("08:00", "08:15", "20:15"), null));

        mvc.perform(get("/api/v1/matchs/creneaux")
                        .param("terrainId", "1")
                        .param("date", "2030-01-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.creneaux[0]").value("08:00"))
                .andExpect(jsonPath("$.creneaux[2]").value("20:15"))
                .andExpect(jsonPath("$.message").value(nullValue()));

        verify(matchPadelService).getCreneauxDisponibles(1L, date);
    }

    @Test
    void getCreneaux_horaire_manquant_200_liste_vide_message() throws Exception {
        LocalDate date = LocalDate.of(2030, 1, 1);
        when(matchPadelService.getCreneauxDisponibles(1L, date))
                .thenReturn(new CreneauxMatchResponseDto(
                        List.of(),
                        "Aucun horaire n'est configure pour ce site et cette annee."
                ));

        mvc.perform(get("/api/v1/matchs/creneaux")
                        .param("terrainId", "1")
                        .param("date", "2030-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creneaux").isArray())
                .andExpect(jsonPath("$.creneaux").isEmpty())
                .andExpect(jsonPath("$.message").value("Aucun horaire n'est configure pour ce site et cette annee."));
    }

    @Test
    void getPublicMatches_200_et_json() throws Exception {
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
                "Le paramÃ¨tre 'from' doit Ãªtre antÃ©rieur ou Ã©gal Ã  'to'."
        ));

        mvc.perform(get("/api/v1/matchs/public")
                        .param("from", "2030-02-01")
                        .param("to", "2030-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOne_public_auth_200() throws Exception {
        when(matchPadelService.getMatchDetailDto(1L))
                .thenReturn(sampleDetailDto(1L, MatchVisibilite.PUBLIC));

        mvc.perform(get("/api/v1/matchs/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dateDebut").value("2030-01-01"))
                .andExpect(jsonPath("$.heureDebut").value("10:00:00"))
                .andExpect(jsonPath("$.siteNom").value("Site Delta"))
                .andExpect(jsonPath("$.terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$.organisateurMatricule").value("G0001"))
                .andExpect(jsonPath("$.visibilite").value("PUBLIC"))
                .andExpect(jsonPath("$.participants[0].matricule").value("G0001"))
                .andExpect(jsonPath("$.participants[1].matricule").value("J0001"))
                .andExpect(jsonPath("$.organisateurNom").value("Organisateur"))
                .andExpect(jsonPath("$.nbParticipants").value(2))
                .andExpect(jsonPath("$.placesRestantes").value(2))
                .andExpect(jsonPath("$.complet").value(false))
                .andExpect(jsonPath("$.peutAjouterJoueurPrive").value(false))
                .andExpect(jsonPath("$.montantTotal").value(60.0))
                .andExpect(jsonPath("$.montantPaye").value(15.0))
                .andExpect(jsonPath("$.resteAPayer").value(45.0));
    }

    @Test
    void getOne_prive_refuse_403() throws Exception {
        when(matchPadelService.getMatchDetailDto(1L))
                .thenThrow(new ForbiddenException("AccÃ¨s refusÃ© Ã  ce match privÃ©."));

        mvc.perform(get("/api/v1/matchs/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOne_prive_autorise_200() throws Exception {
        when(matchPadelService.getMatchDetailDto(1L))
                .thenReturn(sampleDetailDto(1L, MatchVisibilite.PRIVE));

        mvc.perform(get("/api/v1/matchs/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.visibilite").value("PRIVE"))
                .andExpect(jsonPath("$.organisateurMatricule").value("G0001"))
                .andExpect(jsonPath("$.peutAjouterJoueurPrive").value(true));
    }

    @Test
    void getOne_notFound_404_detail() throws Exception {
        when(matchPadelService.getMatchDetailDto(99L))
                .thenThrow(new NotFoundException("Match introuvable"));

        mvc.perform(get("/api/v1/matchs/99"))
                .andExpect(status().isNotFound());
    }
}
