package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.AdminInfoDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminInfoService {

    private final CurrentUserFacade currentUserFacade;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;
    private final SiteRepository siteRepository;

    public AdminInfoService(CurrentUserFacade currentUserFacade,
                            ServiceAutorisationAdmin serviceAutorisationAdmin,
                            SiteRepository siteRepository) {
        this.currentUserFacade = currentUserFacade;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
        this.siteRepository = siteRepository;
    }

    public AdminInfoDto getAdminInfo() {
        if (currentUserFacade.hasRole(SecurityRole.ROLE_ADMIN_GLOBAL)) {
            return new AdminInfoDto("ok", "GLOBAL", null, null);
        }

        Long siteId = serviceAutorisationAdmin.getSiteAdministreId();
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("Site admin introuvable: " + siteId));

        return new AdminInfoDto("ok", "SITE", site.getId(), site.getNom());
    }
}
