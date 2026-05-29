package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.request.RegisterRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.dto.response.RegisterResponse;
import be.ephec.padel.backend.service.AuthenticationService;
import be.ephec.padel.backend.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Authentification", description = "Connexion et demande d'inscription des utilisateurs")
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
    @Operation(summary = "Se connecter")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @SecurityRequirements
    @Operation(summary = "Créer une demande d'inscription")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registrationService.register(request);
        URI location = URI.create("/api/v1/admin/inscriptions/" + response.getUserId());
        return ResponseEntity.created(location).body(response);
    }
}
