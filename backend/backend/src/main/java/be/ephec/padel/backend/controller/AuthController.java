package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.request.RegisterRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.dto.response.RegisterResponse;
import be.ephec.padel.backend.service.AuthenticationService;
import be.ephec.padel.backend.service.RegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    public AuthController(AuthenticationService authenticationService,
                          RegistrationService registrationService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
    }

    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        URI location = URI.create("/api/v1/admin/inscriptions/" + response.getUserId());
        return ResponseEntity.created(location).body(response);
    }
}
