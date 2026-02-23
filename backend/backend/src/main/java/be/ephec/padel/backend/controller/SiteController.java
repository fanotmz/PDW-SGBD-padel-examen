package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateSiteRequest;
import be.ephec.padel.backend.dto.response.SiteDto;
import be.ephec.padel.backend.mapper.SiteMapper;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public ResponseEntity<List<SiteDto>> list() {
        List<SiteDto> dtos = siteService.lister().stream()
                .map(SiteMapper::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteDto> getOne(@PathVariable Long id) {
        Site site = siteService.getSite(id);
        return ResponseEntity.ok(SiteMapper.toDto(site));
    }

    @PostMapping
    public ResponseEntity<SiteDto> create(@Valid @RequestBody CreateSiteRequest req) {
        Site created = siteService.creerSite(req.getNom(), req.getVille());

        URI location = URI.create("/api/v1/sites/" + created.getId());
        return ResponseEntity.created(location)
                .body(SiteMapper.toDto(created));
    }

    @GetMapping("/_whoami")
    public String whoami() {
        return "SITE_CONTROLLER_V2";
    }
}