package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.JoueurCreateRequest;
import be.ephec.padel.backend.dto.response.JoueurDto;
import be.ephec.padel.backend.mapper.JoueurMapper;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.service.JoueurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Joueurs", description = "Consultation et création des joueurs")
@RestController
@RequestMapping("/api/v1/joueurs")
public class JoueurController {

    private final JoueurService joueurService;

    public JoueurController(JoueurService joueurService) {
        this.joueurService = joueurService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lister les joueurs")
    @GetMapping
    public ResponseEntity<List<JoueurDto>> list() {
        List<JoueurDto> dtos = joueurService.lister().stream()
                .map(JoueurMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Créer un joueur")
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
