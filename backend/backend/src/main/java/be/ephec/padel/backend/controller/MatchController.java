package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateMatchRequest;
import be.ephec.padel.backend.dto.response.ApiErrorDto;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.service.MatchPadelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;

import java.time.LocalDate;
import java.util.List;

import java.net.URI;

@Tag(name = "Matchs", description = "Gestion des matchs de padel")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/matchs")
public class MatchController {

    private final MatchPadelService matchPadelService;

    public MatchController(MatchPadelService matchPadelService) {
        this.matchPadelService = matchPadelService;
    }

    @Operation(
            summary = "Récupérer le détail d’un match",
            description = "Retourne le détail d’un match. Un match PUBLIC est visible par tous. "
                    + "Un match PRIVE est visible uniquement par l’organisateur et les participants."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match trouvé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé à ce match privé",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Match introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MatchDetailDto> getOne(
            @PathVariable Long id,
            @RequestParam(required = false) String matricule) {
        return ResponseEntity.ok(matchPadelService.getMatchDetailDto(id, matricule));
    }

    @Operation(summary = "Créer un match", description = "Crée un match (PUBLIC ou PRIVE) si les règles métier sont respectées.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Match créé"),
            @ApiResponse(responseCode = "400", description = "Validation ou règle métier non respectée",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
            @ApiResponse(responseCode = "404", description = "Terrain / site / organisateur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    @PostMapping
    public ResponseEntity<MatchDto> create(@Valid @RequestBody CreateMatchRequest req) {
        MatchPadel created = matchPadelService.creerMatch(
                req.getTerrainId(),
                req.getOrganisateurMatricule(),
                req.getDateDebut(),
                req.getVisibilite()
        );

        MatchDto dto = matchPadelService.getMatchDto(created.getId());
        URI location = URI.create("/api/v1/matchs/" + created.getId());
        return ResponseEntity.created(location).body(dto);
    }
    @Operation(
            summary = "Lister les matchs publics",
            description = "Retourne la liste des matchs PUBLIC avec un résumé utile pour le frontend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste récupérée"),
            @ApiResponse(responseCode = "400", description = "Paramètres invalides",
                    content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    @GetMapping("/public")
    public ResponseEntity<List<PublicMatchSummaryDto>> getPublicMatches(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false) Long siteId) {

        return ResponseEntity.ok(matchPadelService.getPublicMatchSummaries(from, to, siteId));
    }
}
