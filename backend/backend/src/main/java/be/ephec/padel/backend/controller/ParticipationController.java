package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.AddPlayerToPrivateMatchRequest;
import be.ephec.padel.backend.dto.request.JoinPublicMatchRequest;
import be.ephec.padel.backend.dto.response.MontantAttenduResponse;
import be.ephec.padel.backend.dto.response.ParticipationDto;
import be.ephec.padel.backend.mapper.ParticipationMapper;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.service.ParticipationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/matchs")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @PostMapping("/{matchId}/participants/public")
    public ResponseEntity<ParticipationDto> rejoindreMatchPublic(
            @PathVariable Long matchId,
            @Valid @RequestBody JoinPublicMatchRequest req) {

        Participation participation = participationService.rejoindreEtPayerMatchPublic(
                matchId,
                req.getJoueurMatricule()
        );

        URI location = URI.create("/api/v1/matchs/" + matchId);
        return ResponseEntity.created(location).body(ParticipationMapper.toDto(participation));
    }

    @PostMapping("/{matchId}/participants/prive")
    public ResponseEntity<ParticipationDto> ajouterJoueurPrive(
            @PathVariable Long matchId,
            @Valid @RequestBody AddPlayerToPrivateMatchRequest req) {

        Participation participation = participationService.ajouterJoueurParOrganisateur(
                matchId,
                req.getOrganisateurMatricule(),
                req.getJoueurMatriculeAAjouter()
        );

        URI location = URI.create("/api/v1/matchs/" + matchId);
        return ResponseEntity.created(location).body(ParticipationMapper.toDto(participation));
    }

    @GetMapping("/{matchId}/participants/public/montant-attendu")
    public ResponseEntity<MontantAttenduResponse> getMontantAttenduPourMatchPublic(
            @PathVariable Long matchId,
            @RequestParam String joueurMatricule) {

        return ResponseEntity.ok(
                new MontantAttenduResponse(
                        participationService.calculerMontantAttenduPourMatchPublic(matchId, joueurMatricule)
                )
        );
    }
}