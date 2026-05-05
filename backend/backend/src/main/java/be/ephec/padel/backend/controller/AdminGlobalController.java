package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminInfoDto;
import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminInfoService;
import be.ephec.padel.backend.service.AdminStatsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@SecurityRequirement(name = "bearerAuth")
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

    // ex AdminController
    @GetMapping("/info")
    public AdminInfoDto adminInfo() {
        return adminInfoService.getAdminInfo();
    }

    // ex AdminStatsController (on garde les routes pour éviter de casser)
    @GetMapping("/stats/ca")
    public AdminCaStatsDto ca(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminStatsService.getCa(from, to);
    }

    @GetMapping("/stats/matchs")
    public AdminMatchsStatsDto matchs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return adminStatsService.getNbMatchs(from, to);
    }

    @GetMapping("/stats/dettes")
    public AdminDettesStatsDto dettes() {
        return adminStatsService.getDettes();
    }
}
