package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminGlobalController {

    private final AdminStatsService adminStatsService;

    public AdminGlobalController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    // ex AdminController
    @GetMapping("/info")
    public Map<String, Object> adminInfo() {
        return Map.of("status", "ok");
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