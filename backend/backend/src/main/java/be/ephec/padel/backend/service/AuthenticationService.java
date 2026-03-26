package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        return new LoginResponse(null, "Bearer");
    }
}
