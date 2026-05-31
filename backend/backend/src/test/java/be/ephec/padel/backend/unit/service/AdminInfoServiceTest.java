package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminInfoDto;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.AdminInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInfoServiceTest {

    @Mock
    CurrentUserFacade currentUserFacade;

    @Mock
    ServiceAutorisationAdmin serviceAutorisationAdmin;

    @Mock
    SiteRepository siteRepository;

    @InjectMocks
    AdminInfoService adminInfoService;

    @Test
    void getAdminInfo_retourne_le_contexte_global() {
        when(currentUserFacade.hasRole(SecurityRole.ROLE_ADMIN_GLOBAL)).thenReturn(true);

        AdminInfoDto result = adminInfoService.getAdminInfo();

        assertThat(result.getStatus()).isEqualTo("ok");
        assertThat(result.getAdminType()).isEqualTo("GLOBAL");
        assertThat(result.getSiteId()).isNull();
        assertThat(result.getSiteNom()).isNull();
    }

    @Test
    void getAdminInfo_retourne_le_contexte_site() throws Exception {
        Site site = site(7L, "Site Nord");

        when(currentUserFacade.hasRole(SecurityRole.ROLE_ADMIN_GLOBAL)).thenReturn(false);
        when(serviceAutorisationAdmin.getSiteAdministreId()).thenReturn(7L);
        when(siteRepository.findById(7L)).thenReturn(Optional.of(site));

        AdminInfoDto result = adminInfoService.getAdminInfo();

        assertThat(result.getStatus()).isEqualTo("ok");
        assertThat(result.getAdminType()).isEqualTo("SITE");
        assertThat(result.getSiteId()).isEqualTo(7L);
        assertThat(result.getSiteNom()).isEqualTo("Site Nord");
    }

    private Site site(Long id, String nom) throws Exception {
        Site site = new Site(nom, "Bruxelles");
        Field field = Site.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(site, id);
        return site;
    }
}
