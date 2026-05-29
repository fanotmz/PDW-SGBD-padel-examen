package be.ephec.padel.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Diagnostic", description = "Vérification rapide de disponibilité de l'API")
public class PingController {
    @Operation(summary = "Vérifier que l'API répond")
    @GetMapping("/ping")
    public String ping() { return "ok"; }
}
