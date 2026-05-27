package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.MeController;
import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.RegularisationDto;
import be.ephec.padel.backend.dto.response.RegularisationsResponseDto;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.JoueurService;
import be.ephec.padel.backend.service.MeStatsService;
import be.ephec.padel.backend.service.RegularisationService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MeController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(username = "joueur1", roles = "JOUEUR")
class MeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JoueurService joueurService;

    @MockitoBean
    MeStatsService meStatsService;

    @MockitoBean
    RegularisationService regularisationService;

    @Test
    @WithAnonymousUser
    void me_sans_auth_401() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_ok_200() throws Exception {
        when(joueurService.getCurrentJoueurProfile())
                .thenReturn(new Joueur("G0001", "Alice", TypeJoueur.GLOBAL));

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matricule").value("G0001"))
                .andExpect(jsonPath("$.nom").value("Alice"))
                .andExpect(jsonPath("$.type").value("GLOBAL"))
                .andExpect(jsonPath("$.penaliteJusqua").value(nullValue()));
    }

    @Test
    void getMe_expose_penalite_jusqua_si_presente() throws Exception {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        joueur.setPenaliteJusqua(LocalDateTime.of(2030, 1, 10, 12, 30));

        when(joueurService.getCurrentJoueurProfile())
                .thenReturn(joueur);

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.matricule").value("G0001"))
                .andExpect(jsonPath("$.penaliteJusqua").value("2030-01-10T12:30:00"));
    }

    @Test
    void getMe_forbidden_si_aucun_joueur_lie() throws Exception {
        when(joueurService.getCurrentJoueurProfile())
                .thenThrow(new ForbiddenException("Aucun joueur lié à l'utilisateur authentifié."));

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Aucun joueur lié à l'utilisateur authentifié."));
    }

    @Test
    void getMyMatches_ok_200() throws Exception {
        PlayerMatchSummaryDto dto = new PlayerMatchSummaryDto(
                1L,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                100L,
                "Site Delta",
                200L,
                "Terrain 1",
                MatchVisibilite.PUBLIC,
                MatchStatut.PLANIFIE,
                PlayerMatchRoleDto.PARTICIPANT,
                MatchTemporalStatusDto.FUTUR,
                5,
                false,
                77L,
                new BigDecimal("0.00"),
                new BigDecimal("7.50"),
                true
        );

        when(joueurService.getCurrentPlayerMatches()).thenReturn(List.of(dto));

        mvc.perform(get("/api/v1/me/matchs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].siteId").value(100))
                .andExpect(jsonPath("$[0].siteNom").value("Site Delta"))
                .andExpect(jsonPath("$[0].terrainId").value(200))
                .andExpect(jsonPath("$[0].terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$[0].visibilite").value("PUBLIC"))
                .andExpect(jsonPath("$[0].roleJoueur").value("PARTICIPANT"))
                .andExpect(jsonPath("$[0].statutTemporel").value("FUTUR"))
                .andExpect(jsonPath("$[0].joursAvantMatch").value(5))
                .andExpect(jsonPath("$[0].paiementJoueurEffectue").value(false))
                .andExpect(jsonPath("$[0].participationId").value(77))
                .andExpect(jsonPath("$[0].montantPayeJoueur").value(0.00))
                .andExpect(jsonPath("$[0].montantRestantJoueur").value(7.50))
                .andExpect(jsonPath("$[0].peutPayerParticipation").value(true));
    }

    @Test
    void getMyOrganizedMatches_ok_200() throws Exception {
        OrganizerMatchSummaryDto dto = new OrganizerMatchSummaryDto(
                1L,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                100L,
                "Site Delta",
                200L,
                "Terrain 1",
                MatchVisibilite.PRIVE,
                MatchStatut.PLANIFIE,
                2,
                2,
                false,
                MatchTemporalStatusDto.FUTUR,
                1,
                true
        );

        when(joueurService.getCurrentOrganizedMatches()).thenReturn(List.of(dto));

        mvc.perform(get("/api/v1/me/matchs/organises"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].siteId").value(100))
                .andExpect(jsonPath("$[0].siteNom").value("Site Delta"))
                .andExpect(jsonPath("$[0].terrainId").value(200))
                .andExpect(jsonPath("$[0].terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$[0].visibilite").value("PRIVE"))
                .andExpect(jsonPath("$[0].nbParticipants").value(2))
                .andExpect(jsonPath("$[0].placesRestantes").value(2))
                .andExpect(jsonPath("$[0].complet").value(false))
                .andExpect(jsonPath("$[0].statutTemporel").value("FUTUR"))
                .andExpect(jsonPath("$[0].joursAvantMatch").value(1))
                .andExpect(jsonPath("$[0].risquePenaliteJ1").value(true));
    }

    @Test
    void getMyDette_ok_200() throws Exception {
        when(joueurService.currentUserADette()).thenReturn(true);

        mvc.perform(get("/api/v1/me/dette"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dette").value(true));
    }

    @Test
    void getMyRegularisations_ok_200() throws Exception {
        RegularisationDto item = new RegularisationDto(
                77L,
                1L,
                LocalDateTime.of(2030, 1, 10, 10, 0),
                "Site Delta",
                "Terrain 1",
                MatchVisibilite.PUBLIC,
                PlayerMatchRoleDto.PARTICIPANT,
                be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION,
                new BigDecimal("15.00"),
                new BigDecimal("5.00"),
                new BigDecimal("10.00"),
                null,
                true
        );
        when(regularisationService.getCurrentUserRegularisations())
                .thenReturn(new RegularisationsResponseDto(new BigDecimal("10.00"), List.of(item)));

        mvc.perform(get("/api/v1/me/regularisations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalTracable").value(10.00))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].participationId").value(77))
                .andExpect(jsonPath("$.items[0].matchId").value(1))
                .andExpect(jsonPath("$.items[0].siteNom").value("Site Delta"))
                .andExpect(jsonPath("$.items[0].terrainNom").value("Terrain 1"))
                .andExpect(jsonPath("$.items[0].roleJoueur").value("PARTICIPANT"))
                .andExpect(jsonPath("$.items[0].montantInitial").value(15.00))
                .andExpect(jsonPath("$.items[0].montantDejaPaye").value(5.00))
                .andExpect(jsonPath("$.items[0].montantRestant").value(10.00))
                .andExpect(jsonPath("$.items[0].payable").value(true));
    }

    @Test
    void payRegularisation_ok_204() throws Exception {
        mvc.perform(post("/api/v1/me/regularisations/77/paiement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":45.00}"))
                .andExpect(status().isNoContent());

        verify(regularisationService).payerAnnulationTardiveOrganisateur(77L, new BigDecimal("45.00"));
    }

    @Test
    @WithAnonymousUser
    void getMyStats_sans_auth_401() throws Exception {
        mvc.perform(get("/api/v1/me/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyStats_ok_200() throws Exception {
        when(meStatsService.getCurrentUserStats()).thenReturn(new MeStatsDto(
                5L,
                2L,
                3L,
                1L,
                1L,
                new BigDecimal("42.00"),
                new BigDecimal("7.50")
        ));

        mvc.perform(get("/api/v1/me/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nbMatchsParticipes").value(5))
                .andExpect(jsonPath("$.nbMatchsOrganises").value(2))
                .andExpect(jsonPath("$.nbMatchsPasses").value(3))
                .andExpect(jsonPath("$.nbMatchsFuturs").value(1))
                .andExpect(jsonPath("$.nbMatchsAnnules").value(1))
                .andExpect(jsonPath("$.montantTotalPaye").value(42.00))
                .andExpect(jsonPath("$.detteActuelle").value(7.50));
    }

    @Test
    void getMyStats_forbidden_si_aucun_joueur_lie() throws Exception {
        when(meStatsService.getCurrentUserStats())
                .thenThrow(new ForbiddenException("Aucun joueur lié à l'utilisateur authentifié."));

        mvc.perform(get("/api/v1/me/stats"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Aucun joueur lié à l'utilisateur authentifié."));
    }
}
