package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.security.JwtService;
import be.ephec.padel.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthenticationService authenticationService;

    @Test
    void login_utilise_authenticationManager_et_renvoie_contrat_compatible_jwt() {
        LoginRequest request = new LoginRequest();
        request.setUsername("adminGlobal");
        request.setPassword("test123");

        User principal = new User(
                "adminGlobal",
                "hash",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN_GLOBAL"))
        );
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt-test");

        LoginResponse response = authenticationService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken token = captor.getValue();
        assertThat(token.getPrincipal()).isEqualTo("adminGlobal");
        assertThat(token.getCredentials()).isEqualTo("test123");
        assertThat(response.getToken()).isEqualTo("jwt-test");
        assertThat(response.getType()).isEqualTo("Bearer");
    }
}
