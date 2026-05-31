package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.ValidateRegistrationRequest;
import be.ephec.padel.backend.dto.response.PendingRegistrationDto;
import be.ephec.padel.backend.dto.response.RegistrationDecisionResponse;
import be.ephec.padel.backend.service.AdminRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Validation des inscriptions", description = "Validation et refus des demandes d'inscription")
@RestController
@RequestMapping("/api/v1/admin/inscriptions")
public class AdminRegistrationController {

    private final AdminRegistrationService adminRegistrationService;

    public AdminRegistrationController(AdminRegistrationService adminRegistrationService) {
        this.adminRegistrationService = adminRegistrationService;
    }

    @Operation(summary = "Lister les inscriptions en attente")
    @GetMapping
    public ResponseEntity<List<PendingRegistrationDto>> listPending() {
        return ResponseEntity.ok(adminRegistrationService.listPendingRegistrations());
    }

    @Operation(summary = "Valider une inscription")
    @PostMapping("/{userId}/validate")
    public ResponseEntity<RegistrationDecisionResponse> validate(@PathVariable Long userId,
                                                                 @Valid @RequestBody ValidateRegistrationRequest request) {
        return ResponseEntity.ok(adminRegistrationService.validate(userId, request));
    }

    @Operation(summary = "Refuser une inscription")
    @PostMapping("/{userId}/reject")
    public ResponseEntity<RegistrationDecisionResponse> reject(@PathVariable Long userId) {
        return ResponseEntity.ok(adminRegistrationService.reject(userId));
    }
}
