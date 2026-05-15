package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.dto.response.AdminSiteMatchSummaryDto;
import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.model.enums.MatchStatut;
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

    @GetMapping
    public List<AdminSiteConsultationDto> getSites() {
        return adminSiteService.getSitesConsultables();
    }

    @GetMapping("/{siteId}/joueurs")
    public List<JoueurAdminDto> getJoueurs(@PathVariable Long siteId) {
        return adminSiteService.getJoueursBySite(siteId);
    }

    @GetMapping("/{siteId}/matchs")
    public List<AdminSiteMatchSummaryDto> getMatchs(@PathVariable Long siteId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                    LocalDate from,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                    LocalDate to,
                                                    @RequestParam(required = false) MatchStatut statut) {
        return adminSiteService.getMatchsBySite(siteId, from, to, statut);
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
