package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.PayRequest;
import be.ephec.padel.backend.dto.response.DetteDto;
import be.ephec.padel.backend.dto.response.JoueurDto;
import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.RegularisationsResponseDto;
import be.ephec.padel.backend.mapper.JoueurMapper;
import be.ephec.padel.backend.service.JoueurService;
import be.ephec.padel.backend.service.MeStatsService;
import be.ephec.padel.backend.service.RegularisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Espace joueur", description = "Profil, matchs, dettes et statistiques du joueur connecté")
public class MeController {

    private final JoueurService joueurService;
    private final MeStatsService meStatsService;
    private final RegularisationService regularisationService;

    public MeController(JoueurService joueurService,
                        MeStatsService meStatsService,
                        RegularisationService regularisationService) {
        this.joueurService = joueurService;
        this.meStatsService = meStatsService;
        this.regularisationService = regularisationService;
    }

    @Operation(summary = "Récupérer mon profil")
    @GetMapping
    public ResponseEntity<JoueurDto> getMe() {
        return ResponseEntity.ok(JoueurMapper.toDto(joueurService.getCurrentJoueurProfile()));
    }

    @Operation(summary = "Lister mes matchs")
    @GetMapping("/matchs")
    public ResponseEntity<List<PlayerMatchSummaryDto>> getMyMatches() {
        return ResponseEntity.ok(joueurService.getCurrentPlayerMatches());
    }

    @Operation(summary = "Lister mes matchs organisés")
    @GetMapping("/matchs/organises")
    public ResponseEntity<List<OrganizerMatchSummaryDto>> getMyOrganizedMatches() {
        return ResponseEntity.ok(joueurService.getCurrentOrganizedMatches());
    }

    @Operation(summary = "Vérifier ma dette")
    @GetMapping("/dette")
    public ResponseEntity<DetteDto> getMyDette() {
        return ResponseEntity.ok(new DetteDto(joueurService.currentUserADette()));
    }

    @Operation(summary = "Lister mes régularisations")
    @GetMapping("/regularisations")
    public ResponseEntity<RegularisationsResponseDto> getMyRegularisations() {
        return ResponseEntity.ok(regularisationService.getCurrentUserRegularisations());
    }

    @Operation(summary = "Payer une régularisation")
    @PostMapping("/regularisations/{participationId}/paiement")
    public ResponseEntity<Void> payRegularisation(@PathVariable Long participationId,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody PayRequest request) {
        regularisationService.payerAnnulationTardiveOrganisateur(participationId, request.getMontant());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulter mes statistiques")
    @GetMapping("/stats")
    public ResponseEntity<MeStatsDto> getMyStats() {
        return ResponseEntity.ok(meStatsService.getCurrentUserStats());
    }
}
