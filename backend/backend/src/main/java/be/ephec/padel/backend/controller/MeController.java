package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.DetteDto;
import be.ephec.padel.backend.dto.response.JoueurDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.mapper.JoueurMapper;
import be.ephec.padel.backend.service.JoueurService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    private final JoueurService joueurService;

    public MeController(JoueurService joueurService) {
        this.joueurService = joueurService;
    }

    @GetMapping
    public ResponseEntity<JoueurDto> getMe() {
        return ResponseEntity.ok(JoueurMapper.toDto(joueurService.getCurrentJoueurProfile()));
    }

    @GetMapping("/matchs")
    public ResponseEntity<List<PlayerMatchSummaryDto>> getMyMatches() {
        return ResponseEntity.ok(joueurService.getCurrentPlayerMatches());
    }

    @GetMapping("/matchs/organises")
    public ResponseEntity<List<OrganizerMatchSummaryDto>> getMyOrganizedMatches() {
        return ResponseEntity.ok(joueurService.getCurrentOrganizedMatches());
    }

    @GetMapping("/dette")
    public ResponseEntity<DetteDto> getMyDette() {
        return ResponseEntity.ok(new DetteDto(joueurService.currentUserADette()));
    }
}
