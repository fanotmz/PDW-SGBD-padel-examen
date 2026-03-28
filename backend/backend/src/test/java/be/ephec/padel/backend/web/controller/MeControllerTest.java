package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.MeController;
import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.type").value("GLOBAL"));
    }

    @Test
    void getMe_forbidden_si_aucun_joueur_lie() throws Exception {
        when(joueurService.getCurrentJoueurProfile())
                .thenThrow(new ForbiddenException("Aucun joueur lie a l'utilisateur authentifie."));

        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Aucun joueur lie a l'utilisateur authentifie."));
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
                false
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
                .andExpect(jsonPath("$[0].paiementJoueurEffectue").value(false));
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
}
