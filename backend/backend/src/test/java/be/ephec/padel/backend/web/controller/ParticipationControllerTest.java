package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.ParticipationController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ParticipationController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(username = "joueur1", roles = "JOUEUR")
class ParticipationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ParticipationService participationService;

    private Participation participation(Long participationId, Long matchId, String joueurMatricule) {
        MatchPadel m = new MatchPadel();
        ReflectionTestUtils.setField(m, "id", matchId);

        Joueur j = new Joueur(joueurMatricule, "NomTest", TypeJoueur.GLOBAL);

        Participation p = new Participation();
        ReflectionTestUtils.setField(p, "id", participationId);
        p.setMatch(m);
        p.setJoueur(j);

        return p;
    }

    @Test
    void rejoindrePublic_ok_201_et_body_et_location() throws Exception {
        long matchId = 10L;
        Participation saved = participation(77L, matchId, "J001");

        when(participationService.rejoindreEtPayerMatchPublic(eq(matchId)))
                .thenReturn(saved);

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/public", matchId)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/matchs/" + matchId))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.matchId").value((int) matchId))
                .andExpect(jsonPath("$.joueurMatricule").value("J001"));
    }

    @Test
    void rejoindrePublic_notFound_404() throws Exception {
        long matchId = 999L;

        when(participationService.rejoindreEtPayerMatchPublic(eq(matchId)))
                .thenThrow(new NotFoundException("Match introuvable"));

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/public", matchId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Match introuvable"));
    }

    @Test
    void rejoindrePublic_business_400() throws Exception {
        long matchId = 10L;

        when(participationService.rejoindreEtPayerMatchPublic(eq(matchId)))
                .thenThrow(new BusinessException("Match dÃ©jÃ  complet"));

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/public", matchId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Match dÃ©jÃ  complet"));
    }

    @Test
    void ajouterPrive_ok_201_et_body_et_location() throws Exception {
        long matchId = 11L;
        Participation saved = participation(88L, matchId, "J009");

        when(participationService.ajouterJoueurParOrganisateur(eq(matchId), eq("J009")))
                .thenReturn(saved);

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/prive", matchId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "joueurMatriculeAAjouter": "J009" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/matchs/" + matchId))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(88))
                .andExpect(jsonPath("$.matchId").value((int) matchId))
                .andExpect(jsonPath("$.joueurMatricule").value("J009"));
    }

    @Test
    void ajouterPrive_validation_400_si_body_invalide() throws Exception {
        long matchId = 11L;

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/prive", matchId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void ajouterPrive_notFound_404() throws Exception {
        long matchId = 11L;

        when(participationService.ajouterJoueurParOrganisateur(eq(matchId), eq("J009")))
                .thenThrow(new NotFoundException("Joueur introuvable"));

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/prive", matchId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "joueurMatriculeAAjouter": "J009" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Joueur introuvable"));
    }

    @Test
    void ajouterPrive_business_400() throws Exception {
        long matchId = 11L;

        when(participationService.ajouterJoueurParOrganisateur(eq(matchId), eq("J009")))
                .thenThrow(new BusinessException("Seul l'organisateur peut ajouter des joueurs Ã  ce match."));

        mvc.perform(post("/api/v1/matchs/{matchId}/participants/prive", matchId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "joueurMatriculeAAjouter": "J009" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Seul l'organisateur peut ajouter des joueurs Ã  ce match."));
    }
}
