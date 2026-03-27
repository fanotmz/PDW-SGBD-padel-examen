package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.JoueurCreateRequest;
import be.ephec.padel.backend.dto.response.DetteDto;
import be.ephec.padel.backend.dto.response.JoueurDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.mapper.JoueurMapper;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.service.JoueurService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/joueurs")
public class JoueurController {

    private final JoueurService joueurService;

    public JoueurController(JoueurService joueurService) {
        this.joueurService = joueurService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<JoueurDto>> list() {
        List<JoueurDto> dtos = joueurService.lister().stream()
                .map(JoueurMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{matricule}")
    public ResponseEntity<JoueurDto> getOne(@PathVariable String matricule) {
        Joueur joueur = joueurService.getJoueur(matricule);
        return ResponseEntity.ok(JoueurMapper.toDto(joueur));
    }

    @GetMapping("/{matricule}/dette")
    public ResponseEntity<DetteDto> hasDette(@PathVariable String matricule) {
        boolean dette = joueurService.aDette(matricule);
        return ResponseEntity.ok(new DetteDto(dette));
    }

    @GetMapping("/{matricule}/matchs")
    public ResponseEntity<List<PlayerMatchSummaryDto>> getPlayerMatches(@PathVariable String matricule) {
        return ResponseEntity.ok(joueurService.getPlayerMatches(matricule));
    }

    @GetMapping("/{matricule}/matchs/organises")
    public ResponseEntity<List<OrganizerMatchSummaryDto>> getOrganizedMatches(@PathVariable String matricule) {
        return ResponseEntity.ok(joueurService.getOrganizedMatches(matricule));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<JoueurDto> create(@Valid @RequestBody JoueurCreateRequest req) {
        Joueur created = joueurService.creerJoueur(
                req.getMatricule(),
                req.getNom(),
                req.getType(),
                req.getSiteId()
        );

        URI location = URI.create("/api/v1/joueurs/" + created.getMatricule());
        return ResponseEntity.created(location).body(JoueurMapper.toDto(created));
    }
}
