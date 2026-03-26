package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    AuthenticationService authenticationService;

    @Test
    void login_utilise_authenticationManager_et_renvoie_contrat_compatible_jwt() {
        LoginRequest request = new LoginRequest();
        request.setUsername("adminGlobal");
        request.setPassword("test123");

        LoginResponse response = authenticationService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());

        UsernamePasswordAuthenticationToken token = captor.getValue();
        assertThat(token.getPrincipal()).isEqualTo("adminGlobal");
        assertThat(token.getCredentials()).isEqualTo("test123");
        assertThat(response.getToken()).isNull();
        assertThat(response.getType()).isEqualTo("Bearer");
    }
}
