package be.ephec.padel.backend.unit.security;

import be.ephec.padel.backend.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "cle-secrete-de-test-jwt-ephec-padel-2026-123456789";

    @Test
    void genere_un_token_valide_et_extrait_le_login() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        User user = new User("adminGlobal", "hash",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN_GLOBAL")));

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("adminGlobal");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void token_expire_declenche_une_exception() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 5);
        User user = new User("adminGlobal", "hash", java.util.List.of());

        String token = jwtService.generateToken(user);
        Thread.sleep(20);

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void token_signe_avec_un_autre_secret_est_invalide() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        JwtService otherJwtService = new JwtService(
                "autre-cle-secrete-de-test-jwt-ephec-padel-2026-987654321",
                60_000
        );
        User user = new User("adminGlobal", "hash", java.util.List.of());

        String token = jwtService.generateToken(user);

        assertThatThrownBy(() -> otherJwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}
