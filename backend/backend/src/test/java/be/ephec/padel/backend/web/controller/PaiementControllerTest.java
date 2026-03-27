package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.PaiementController;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.service.PaiementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaiementController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@WithMockUser(username = "joueur1", roles = "JOUEUR")
class PaiementControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaiementService paiementService;

    private Paiement paiement(Long paiementId, Long participationId, BigDecimal montant, LocalDateTime date) {
        Participation p = new Participation();
        ReflectionTestUtils.setField(p, "id", participationId);

        Paiement pay = new Paiement();
        ReflectionTestUtils.setField(pay, "id", paiementId);
        pay.setParticipation(p);
        pay.setMontant(montant);
        pay.setDatePaiement(date);
        return pay;
    }

    @Test
    void pay_ok_201_location_et_body() throws Exception {
        long participationId = 12L;

        // Peu importe le format Java, on sait que Jackson renvoie "…:00"
        LocalDateTime date = LocalDateTime.of(2026, 2, 24, 16, 0);
        Paiement saved = paiement(99L, participationId, new BigDecimal("7.50"), date);

        when(paiementService.payerParticipation(eq(participationId), eq(new BigDecimal("7.50"))))
                .thenReturn(saved);

        mvc.perform(post("/api/v1/participations/{participationId}/paiements", participationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "montant": 7.50 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/participations/" + participationId + "/paiements/99"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.participationId").value((int) participationId))
                .andExpect(jsonPath("$.montant").value(7.5))
                // ✅ Assertion exacte sur le JSON renvoyé
                .andExpect(jsonPath("$.datePaiement").value("2026-02-24T16:00:00"));
    }

    @Test
    void pay_validation_400_si_body_invalide() throws Exception {
        long participationId = 12L;

        mvc.perform(post("/api/v1/participations/{participationId}/paiements", participationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void pay_notFound_404_si_participation_introuvable() throws Exception {
        long participationId = 404L;

        when(paiementService.payerParticipation(eq(participationId), eq(new BigDecimal("5.00"))))
                .thenThrow(new NotFoundException("Participation introuvable"));

        mvc.perform(post("/api/v1/participations/{participationId}/paiements", participationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "montant": 5.00 }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Participation introuvable"));
    }

    @Test
    void pay_business_400_si_regle_metier() throws Exception {
        long participationId = 12L;

        when(paiementService.payerParticipation(eq(participationId), eq(new BigDecimal("50.00"))))
                .thenThrow(new BusinessException("Paiement trop élevé. Reste à payer = 15.00"));

        mvc.perform(post("/api/v1/participations/{participationId}/paiements", participationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "montant": 50.00 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Paiement trop élevé. Reste à payer = 15.00"));
    }
}
