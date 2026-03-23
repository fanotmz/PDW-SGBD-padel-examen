package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.CreateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.dto.response.FermetureSiteDto;
import be.ephec.padel.backend.mapper.FermetureSiteMapper;
import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.service.FermetureSiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sites/{siteId}/fermetures")
public class FermetureSiteAdminController {

    private final FermetureSiteService fermetureSiteService;

    public FermetureSiteAdminController(FermetureSiteService fermetureSiteService) {
        this.fermetureSiteService = fermetureSiteService;
    }

    @PostMapping("/date")
    public ResponseEntity<FermetureSiteDto> createDate(@PathVariable Long siteId,
                                                       @RequestBody CreateFermetureSiteDateRequest request) {
        FermetureSite created = fermetureSiteService.creerDate(siteId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/admin/sites/{siteId}/fermetures/{fermetureId}")
                .buildAndExpand(siteId, created.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(FermetureSiteMapper.toDto(created));
    }

    @PostMapping("/periode")
    public ResponseEntity<FermetureSiteDto> createPeriode(@PathVariable Long siteId,
                                                          @RequestBody CreateFermetureSitePeriodeRequest request) {
        FermetureSite created = fermetureSiteService.creerPeriode(siteId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/admin/sites/{siteId}/fermetures/{fermetureId}")
                .buildAndExpand(siteId, created.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(FermetureSiteMapper.toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<FermetureSiteDto>> getAllBySite(@PathVariable Long siteId) {
        List<FermetureSiteDto> result = fermetureSiteService.listerParSite(siteId)
                .stream()
                .map(FermetureSiteMapper::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{fermetureId}")
    public ResponseEntity<FermetureSiteDto> getOne(@PathVariable Long siteId,
                                                   @PathVariable Long fermetureId) {
        FermetureSite fermeture = fermetureSiteService.getById(siteId, fermetureId);
        return ResponseEntity.ok(FermetureSiteMapper.toDto(fermeture));
    }

    @PutMapping("/{fermetureId}/date")
    public ResponseEntity<FermetureSiteDto> updateDate(@PathVariable Long siteId,
                                                       @PathVariable Long fermetureId,
                                                       @RequestBody UpdateFermetureSiteDateRequest request) {
        FermetureSite updated = fermetureSiteService.updateDate(siteId, fermetureId, request);
        return ResponseEntity.ok(FermetureSiteMapper.toDto(updated));
    }

    @PutMapping("/{fermetureId}/periode")
    public ResponseEntity<FermetureSiteDto> updatePeriode(@PathVariable Long siteId,
                                                          @PathVariable Long fermetureId,
                                                          @RequestBody UpdateFermetureSitePeriodeRequest request) {
        FermetureSite updated = fermetureSiteService.updatePeriode(siteId, fermetureId, request);
        return ResponseEntity.ok(FermetureSiteMapper.toDto(updated));
    }

    @DeleteMapping("/{fermetureId}")
    public ResponseEntity<Void> delete(@PathVariable Long siteId,
                                       @PathVariable Long fermetureId) {
        fermetureSiteService.delete(siteId, fermetureId);
        return ResponseEntity.noContent().build();
    }
}