package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.PayRequest;
import be.ephec.padel.backend.dto.response.PaiementDto;
import be.ephec.padel.backend.mapper.PaiementMapper;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.service.PaiementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/participations")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

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