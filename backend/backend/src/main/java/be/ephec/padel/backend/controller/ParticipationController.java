package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.AddPlayerToPrivateMatchRequest;
import be.ephec.padel.backend.dto.response.MontantAttenduResponse;
import be.ephec.padel.backend.dto.response.ParticipationDto;
import be.ephec.padel.backend.mapper.ParticipationMapper;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/matchs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Participations", description = "Inscription des joueurs aux matchs")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @Operation(summary = "Rejoindre un match public")
    @PostMapping("/{matchId}/participants/public")
    public ResponseEntity<ParticipationDto> rejoindreMatchPublic(@PathVariable Long matchId) {
        Participation participation = participationService.rejoindreEtPayerMatchPublic(matchId);

        URI location = URI.create("/api/v1/matchs/" + matchId);
        return ResponseEntity.created(location).body(ParticipationMapper.toDto(participation));
    }

    @Operation(summary = "Ajouter un joueur à un match privé")
    @PostMapping("/{matchId}/participants/prive")
    public ResponseEntity<ParticipationDto> ajouterJoueurPrive(
            @PathVariable Long matchId,
            @Valid @RequestBody AddPlayerToPrivateMatchRequest req) {

        Participation participation = participationService.ajouterJoueurParOrganisateur(
                matchId,
                req.getJoueurMatriculeAAjouter()
        );

        URI location = URI.create("/api/v1/matchs/" + matchId);
        return ResponseEntity.created(location).body(ParticipationMapper.toDto(participation));
    }

    @Operation(summary = "Consulter le montant attendu pour rejoindre un match public")
    @GetMapping("/{matchId}/participants/public/montant-attendu")
    public ResponseEntity<MontantAttenduResponse> getMontantAttenduPourMatchPublic(@PathVariable Long matchId) {
        return ResponseEntity.ok(
                new MontantAttenduResponse(participationService.calculerMontantAttenduPourMatchPublic(matchId))
        );
    }
}
