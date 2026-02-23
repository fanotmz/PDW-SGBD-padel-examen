package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.request.CreateMatchRequest;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.service.MatchPadelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/matchs")
public class MatchController {

    private final MatchPadelService matchPadelService;

    public MatchController(MatchPadelService matchPadelService) {
        this.matchPadelService = matchPadelService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(matchPadelService.getMatchDto(id));
    }

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
}