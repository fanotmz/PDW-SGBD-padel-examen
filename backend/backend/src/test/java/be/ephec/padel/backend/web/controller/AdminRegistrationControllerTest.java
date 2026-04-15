package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminRegistrationController;
import be.ephec.padel.backend.dto.response.PendingRegistrationDto;
import be.ephec.padel.backend.dto.response.RegistrationDecisionResponse;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.service.AdminRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminRegistrationController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AdminRegistrationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdminRegistrationService adminRegistrationService;

    @Test
    @WithAnonymousUser
    void listPending_sans_auth_401() throws Exception {
        mvc.perform(get("/api/v1/admin/inscriptions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_SITE")
    void listPending_adminSite_refuse_403() throws Exception {
        mvc.perform(get("/api/v1/admin/inscriptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void listPending_adminGlobal_ok_200() throws Exception {
        when(adminRegistrationService.listPendingRegistrations()).thenReturn(List.of(
                new PendingRegistrationDto(12L, "alice", "Alice", TypeJoueur.SITE, 3L, "Site Delta", UserStatus.PENDING)
        ));

        mvc.perform(get("/api/v1/admin/inscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(12))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].nomDemande").value("Alice"))
                .andExpect(jsonPath("$[0].typeAbonnementDemande").value("SITE"))
                .andExpect(jsonPath("$[0].siteIdDemande").value(3))
                .andExpect(jsonPath("$[0].siteNomDemande").value("Site Delta"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void validate_ok_200_et_reponse_explicite() throws Exception {
        when(adminRegistrationService.validate(eq(12L), any()))
                .thenReturn(new RegistrationDecisionResponse(
                        12L,
                        "alice",
                        UserStatus.ACTIVE,
                        "Inscription validée. Le compte est maintenant actif.",
                        "S0007",
                        "Alice",
                        TypeJoueur.SITE,
                        7L
                ));

        mvc.perform(post("/api/v1/admin/inscriptions/12/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeAbonnementFinal": "SITE",
                                  "siteIdFinal": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("Inscription validée. Le compte est maintenant actif."))
                .andExpect(jsonPath("$.joueurMatricule").value("S0007"))
                .andExpect(jsonPath("$.joueurType").value("SITE"))
                .andExpect(jsonPath("$.joueurSiteId").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMIN_GLOBAL")
    void reject_ok_200_et_reponse_explicite() throws Exception {
        when(adminRegistrationService.reject(13L))
                .thenReturn(new RegistrationDecisionResponse(
                        13L,
                        "bob",
                        UserStatus.REJECTED,
                        "Inscription refusée. Le compte ne peut pas être utilisé.",
                        null,
                        null,
                        null,
                        null
                ));

        mvc.perform(post("/api/v1/admin/inscriptions/13/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Inscription refusée. Le compte ne peut pas être utilisé."));
    }
}
