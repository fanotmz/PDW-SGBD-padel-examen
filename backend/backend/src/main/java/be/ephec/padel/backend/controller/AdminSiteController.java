package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.service.AdminSiteService;
import be.ephec.padel.backend.service.AdminSiteStatsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/sites")
@Tag(name = "Admin Site")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;
    private final AdminSiteStatsService adminSiteStatsService;

    public AdminSiteController(AdminSiteService adminSiteService,
                               AdminSiteStatsService adminSiteStatsService) {
        this.adminSiteService = adminSiteService;
        this.adminSiteStatsService = adminSiteStatsService;
    }

    @GetMapping("/{siteId}/joueurs")
    public List<JoueurAdminDto> getJoueurs(@PathVariable Long siteId) {
        return adminSiteService.getJoueursBySite(siteId);
    }

    @GetMapping("/{siteId}/stats/ca")
    public AdminCaStatsDto ca(@PathVariable Long siteId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminSiteStatsService.getCa(siteId, from, to);
    }

    @GetMapping("/{siteId}/stats/matchs")
    public AdminMatchsStatsDto matchs(@PathVariable Long siteId,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminSiteStatsService.getNbMatchs(siteId, from, to);
    }

    @GetMapping("/{siteId}/stats/dettes")
    public AdminDettesStatsDto dettes(@PathVariable Long siteId) {
        return adminSiteStatsService.getDettes(siteId);
    }
}
