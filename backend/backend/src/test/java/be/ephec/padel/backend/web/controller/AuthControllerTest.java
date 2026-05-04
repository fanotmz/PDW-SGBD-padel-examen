package be.ephec.padel.backend.web.controller;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AuthController;
import be.ephec.padel.backend.dto.response.RegisterResponse;
import be.ephec.padel.backend.error.ApiExceptionHandler;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.service.AuthenticationService;
import be.ephec.padel.backend.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthenticationService authenticationService;

    @MockitoBean
    RegistrationService registrationService;

    @Test
    void login_ok_200_et_json() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new LoginResponse("jwt-test", "Bearer", List.of("ROLE_ADMIN_GLOBAL"), false));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "adminGlobal",
                                  "password": "test123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-test"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN_GLOBAL"))
                .andExpect(jsonPath("$.hasPlayerProfile").value(false));
    }

    @Test
    void login_validation_400_si_body_invalide() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_401_si_credentials_invalides() throws Exception {
        when(authenticationService.login(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "adminGlobal",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void login_403_si_compte_pending() throws Exception {
        when(authenticationService.login(any()))
                .thenThrow(new ForbiddenException("Compte en attente de validation administrateur."));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "futurePlayer",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Compte en attente de validation administrateur."));
    }

    @Test
    void login_403_si_compte_rejected() throws Exception {
        when(authenticationService.login(any()))
                .thenThrow(new ForbiddenException("Demande d'inscription refusée."));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "rejectedPlayer",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Demande d'inscription refusée."));
    }

    @Test
    void register_ok_201_et_message_clair() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(new RegisterResponse(
                        12L,
                        "alice",
                        UserStatus.PENDING,
                        "Demande d'inscription en attente de validation administrateur."
                ));

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "secret123",
                                  "nom": "Alice",
                                  "typeAbonnementDemande": "GLOBAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(12))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("Demande d'inscription en attente de validation administrateur."));
    }
}
