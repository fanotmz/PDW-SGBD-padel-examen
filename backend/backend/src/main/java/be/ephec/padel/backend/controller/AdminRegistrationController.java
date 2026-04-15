package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.ValidateRegistrationRequest;
import be.ephec.padel.backend.dto.response.PendingRegistrationDto;
import be.ephec.padel.backend.dto.response.RegistrationDecisionResponse;
import be.ephec.padel.backend.service.AdminRegistrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/inscriptions")
public class AdminRegistrationController {

    private final AdminRegistrationService adminRegistrationService;

    public AdminRegistrationController(AdminRegistrationService adminRegistrationService) {
        this.adminRegistrationService = adminRegistrationService;
    }

    @GetMapping
    public ResponseEntity<List<PendingRegistrationDto>> listPending() {
        return ResponseEntity.ok(adminRegistrationService.listPendingRegistrations());
    }

    @PostMapping("/{userId}/validate")
    public ResponseEntity<RegistrationDecisionResponse> validate(@PathVariable Long userId,
                                                                 @Valid @RequestBody ValidateRegistrationRequest request) {
        return ResponseEntity.ok(adminRegistrationService.validate(userId, request));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<RegistrationDecisionResponse> reject(@PathVariable Long userId) {
        return ResponseEntity.ok(adminRegistrationService.reject(userId));
    }
}
