package be.ephec.padel.backend.controller;

import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.service.AdminSiteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sites")
@Tag(name = "Admin Sites")
public class AdminSiteController {

    private final AdminSiteService adminSiteService;

    public AdminSiteController(AdminSiteService adminSiteService) {
        this.adminSiteService = adminSiteService;
    }

    @GetMapping("/{siteId}/joueurs")
    public List<JoueurAdminDto> getJoueurs(@PathVariable Long siteId) {
        return adminSiteService.getJoueursBySite(siteId);
    }
}