package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateTerrainRequest;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.mapper.TerrainMapper;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.service.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Terrains", description = "Consultation et création des terrains")
@RestController
@RequestMapping("/api/v1/terrains")
public class TerrainController {

    private final TerrainService terrainService;

    public TerrainController(TerrainService terrainService) {
        this.terrainService = terrainService;
    }

    @Operation(summary = "Lister les terrains")
    @GetMapping
    public ResponseEntity<List<TerrainDto>> list(
            @RequestParam(required = false) Long siteId) {

        List<Terrain> terrains = (siteId == null)
                ? terrainService.lister()
                : terrainService.listerParSite(siteId);

        List<TerrainDto> dtos = terrains.stream()
                .map(TerrainMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Récupérer un terrain")
    @GetMapping("/{id}")
    public ResponseEntity<TerrainDto> getOne(@PathVariable Long id) {
        Terrain terrain = terrainService.getTerrain(id);
        return ResponseEntity.ok(TerrainMapper.toDto(terrain));
    }

    @Operation(summary = "Créer un terrain")
    @PostMapping
    public ResponseEntity<TerrainDto> create(
            @Valid @RequestBody CreateTerrainRequest req) {

        Terrain created = terrainService.creerTerrain(
                req.getNom(),
                req.getSiteId()
        );

        URI location = URI.create("/api/v1/terrains/" + created.getId());

        return ResponseEntity
                .created(location)
                .body(TerrainMapper.toDto(created));
    }
}
