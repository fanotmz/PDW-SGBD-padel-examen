package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @Mock
    UserRepository userRepository;

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
        when(userRepository.findByLogin("adminGlobal")).thenReturn(java.util.Optional.empty());
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

    @Test
    void login_refuse_avec_message_clair_si_compte_pending() {
        LoginRequest request = new LoginRequest();
        request.setUsername("futurePlayer");
        request.setPassword("secret");

        be.ephec.padel.backend.model.entities.User user = new be.ephec.padel.backend.model.entities.User();
        user.setLogin("futurePlayer");
        user.setStatus(UserStatus.PENDING);
        when(userRepository.findByLogin("futurePlayer")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Compte en attente de validation administrateur.");
    }

    @Test
    void login_refuse_avec_message_clair_si_compte_rejected() {
        LoginRequest request = new LoginRequest();
        request.setUsername("rejectedPlayer");
        request.setPassword("secret");

        be.ephec.padel.backend.model.entities.User user = new be.ephec.padel.backend.model.entities.User();
        user.setLogin("rejectedPlayer");
        user.setStatus(UserStatus.REJECTED);
        when(userRepository.findByLogin("rejectedPlayer")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Demande d'inscription refusée.");
    }
}
