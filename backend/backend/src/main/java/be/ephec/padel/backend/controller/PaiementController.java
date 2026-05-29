package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.PayRequest;
import be.ephec.padel.backend.dto.response.PaiementDto;
import be.ephec.padel.backend.mapper.PaiementMapper;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.service.PaiementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/participations")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Paiements", description = "Paiement des participations")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @Operation(summary = "Payer une participation")
    @PostMapping("/{participationId}/paiements")
    public ResponseEntity<PaiementDto> pay(
            @PathVariable Long participationId,
            @Valid @RequestBody PayRequest req
    ) {
        Paiement saved = paiementService.payerParticipation(participationId, req.getMontant());

        URI location = URI.create("/api/v1/participations/" + participationId + "/paiements/" + saved.getId());
        return ResponseEntity.created(location).body(PaiementMapper.toDto(saved));
    }
}
