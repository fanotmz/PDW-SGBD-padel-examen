package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateTerrainRequest;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.mapper.TerrainMapper;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.service.TerrainService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/terrains")
public class TerrainController {

    private final TerrainService terrainService;

    public TerrainController(TerrainService terrainService) {
        this.terrainService = terrainService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<TerrainDto> getOne(@PathVariable Long id) {
        Terrain terrain = terrainService.getTerrain(id);
        return ResponseEntity.ok(TerrainMapper.toDto(terrain));
    }

    @SecurityRequirement(name = "basicAuth")
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