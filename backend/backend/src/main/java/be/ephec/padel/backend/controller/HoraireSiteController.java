package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.UpsertHoraireSiteRequest;
import be.ephec.padel.backend.dto.response.HoraireSiteDto;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.service.HoraireSiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/horaires")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Horaires des sites", description = "Gestion des horaires d'ouverture des sites")
public class HoraireSiteController {

    private final HoraireSiteService horaireSiteService;

    public HoraireSiteController(HoraireSiteService horaireSiteService) {
        this.horaireSiteService = horaireSiteService;
    }

    @Operation(summary = "Lister les horaires d'un site")
    @GetMapping
    public ResponseEntity<List<HoraireSiteDto>> list(@PathVariable Long siteId) {
        List<HoraireSiteDto> dtos = horaireSiteService.listBySite(siteId)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Récupérer l'horaire d'un site pour une année")
    @GetMapping("/{annee}")
    public ResponseEntity<HoraireSiteDto> getByAnnee(@PathVariable Long siteId,
                                                     @PathVariable Integer annee) {
        HoraireSite horaire = horaireSiteService.getBySiteAndAnnee(siteId, annee);
        return ResponseEntity.ok(toDto(horaire));
    }

    @Operation(summary = "Créer un horaire pour un site")
    @PostMapping
    public ResponseEntity<HoraireSiteDto> create(@PathVariable Long siteId,
                                                 @Valid @RequestBody UpsertHoraireSiteRequest req) {
        HoraireSite created = horaireSiteService.create(siteId, req);

        URI location = URI.create("/api/v1/admin/sites/" + siteId + "/horaires/" + created.getAnnee());
        return ResponseEntity.created(location).body(toDto(created));
    }

    @Operation(summary = "Modifier un horaire de site")
    @PutMapping("/{horaireId}")
    public ResponseEntity<HoraireSiteDto> update(@PathVariable Long siteId,
                                                 @PathVariable Long horaireId,
                                                 @Valid @RequestBody UpsertHoraireSiteRequest req) {
        HoraireSite updated = horaireSiteService.update(siteId, horaireId, req);
        return ResponseEntity.ok(toDto(updated));
    }

    @Operation(summary = "Supprimer un horaire de site")
    @DeleteMapping("/{horaireId}")
    public ResponseEntity<Void> delete(@PathVariable Long siteId,
                                       @PathVariable Long horaireId) {
        horaireSiteService.delete(siteId, horaireId);
        return ResponseEntity.noContent().build();
    }

    private HoraireSiteDto toDto(HoraireSite horaire) {
        return new HoraireSiteDto(
                horaire.getId(),
                horaire.getSite().getId(),
                horaire.getAnnee(),
                horaire.getHeureOuverture(),
                horaire.getHeureFermeture()
        );
    }
}
