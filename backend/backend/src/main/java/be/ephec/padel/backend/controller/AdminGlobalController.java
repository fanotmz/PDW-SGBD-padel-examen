package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminInfoDto;
import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminInfoService;
import be.ephec.padel.backend.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administration globale", description = "Informations et statistiques globales d'administration")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminGlobalController {

    private final AdminStatsService adminStatsService;
    private final AdminInfoService adminInfoService;

    public AdminGlobalController(AdminStatsService adminStatsService,
                                 AdminInfoService adminInfoService) {
        this.adminStatsService = adminStatsService;
        this.adminInfoService = adminInfoService;
    }

    @Operation(summary = "Récupérer les informations administrateur")
    @GetMapping("/info")
    public AdminInfoDto adminInfo() {
        return adminInfoService.getAdminInfo();
    }

    @Operation(summary = "Consulter le chiffre d'affaires global")
    @GetMapping("/stats/ca")
    public AdminCaStatsDto ca(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminStatsService.getCa(from, to);
    }

    @Operation(summary = "Consulter les statistiques globales des matchs")
    @GetMapping("/stats/matchs")
    public AdminMatchsStatsDto matchs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminStatsService.getNbMatchs(from, to);
    }

    @Operation(summary = "Consulter les dettes globales")
    @GetMapping("/stats/dettes")
    public AdminDettesStatsDto dettes() {
        return adminStatsService.getDettes();
    }
}
