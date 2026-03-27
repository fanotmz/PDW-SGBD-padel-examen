package be.ephec.padel.backend.web.security;

import be.ephec.padel.backend.config.SecurityConfig;
import be.ephec.padel.backend.controller.AdminGlobalController;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.security.JwtService;
import be.ephec.padel.backend.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminGlobalController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.swagger.permit-all=true",
        "app.security.jwt.secret=cle-secrete-de-test-jwt-ephec-padel-2026-123456789",
        "app.security.jwt.expiration-ms=3600000"
})
class JwtAuthenticationFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    AdminStatsService adminStatsService;

    @MockitoBean
    UserRepository userRepository;

    @Test
    void route_protegee_sans_token_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/dettes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification requise"));
    }

    @Test
    void route_protegee_avec_token_invalide_401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/dettes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalide"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification requise"));
    }

    @Test
    void route_protegee_avec_token_valide_et_role_autorise_200() throws Exception {
        User user = new User();
        user.setLogin("adminGlobal");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.addRole(SecurityRole.ROLE_ADMIN_GLOBAL);
        when(userRepository.findByLogin("adminGlobal")).thenReturn(Optional.of(user));
        when(adminStatsService.getDettes())
                .thenReturn(new AdminDettesStatsDto(new BigDecimal("10.00"), 2L));

        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                .username("adminGlobal")
                .password("hash")
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_GLOBAL"))
                .build();
        String token = jwtService.generateToken(principal);

        mockMvc.perform(get("/api/v1/admin/stats/dettes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detteTotale").value(10.0))
                .andExpect(jsonPath("$.nbJoueursEnDette").value(2));
    }

    @Test
    void route_protegee_avec_token_valide_mais_role_insuffisant_403() throws Exception {
        User user = new User();
        user.setLogin("joueur1");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.addRole(SecurityRole.ROLE_JOUEUR);
        when(userRepository.findByLogin("joueur1")).thenReturn(Optional.of(user));

        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                .username("joueur1")
                .password("hash")
                .authorities(new SimpleGrantedAuthority("ROLE_JOUEUR"))
                .build();
        String token = jwtService.generateToken(principal);

        mockMvc.perform(get("/api/v1/admin/stats/dettes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
    }
}
