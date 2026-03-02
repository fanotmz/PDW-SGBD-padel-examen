package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.service.AdminStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/v1/admin/stats")
@Tag(name = "Admin Stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/ca")
    public AdminCaStatsDto ca(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                              @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return adminStatsService.getCa(from, to);
    }

    @GetMapping("/matchs")
    public AdminMatchsStatsDto matchs(@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return adminStatsService.getNbMatchs(from, to);
    }

    @GetMapping("/dettes")
    public AdminDettesStatsDto dettes() {
        return adminStatsService.getDettes();
    }
}