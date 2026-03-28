package be.ephec.padel.backend.security;

import be.ephec.padel.backend.model.enums.SecurityRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceAutorisationAdmin {

    private final Map<String, Long> perimetreAdminsSiteParLogin;
    private final CurrentUserFacade currentUserFacade;

    public ServiceAutorisationAdmin(
            CurrentUserFacade currentUserFacade,
            @Value("${app.security.admin.site.users:adminSite1:1,adminSite2:2}") String adminsSite
    ) {
        this.currentUserFacade = currentUserFacade;
        this.perimetreAdminsSiteParLogin = parserAdminsSite(adminsSite);
    }

    public void verifierAccesAuSite(Long siteDemandeId) {
        if (currentUserFacade.hasRole(SecurityRole.ROLE_ADMIN_GLOBAL)) {
            return;
        }

        if (currentUserFacade.hasRole(SecurityRole.ROLE_ADMIN_SITE)) {
            String login = currentUserFacade.getCurrentUser().getLogin();
            Long siteAutoriseId = perimetreAdminsSiteParLogin.get(login);

            if (siteAutoriseId == null) {
                throw new AccessDeniedException("ADMIN_SITE sans site associe");
            }
            if (!siteAutoriseId.equals(siteDemandeId)) {
                throw new AccessDeniedException("Acces interdit : site non autorise");
            }
            return;
        }

        throw new AccessDeniedException("Acces refuse");
    }

    private Map<String, Long> parserAdminsSite(String adminsSite) {
        Map<String, Long> map = new HashMap<>();
        if (adminsSite == null || adminsSite.isBlank()) {
            return map;
        }

        String[] entrees = adminsSite.split(",");
        for (String entree : entrees) {
            String nettoyee = entree.trim();
            if (nettoyee.isEmpty()) {
                continue;
            }

            String[] parties = nettoyee.split(":");
            if (parties.length < 2) {
                continue;
            }

            String login = parties[0].trim();
            String siteIdTexte = parties[1].trim();

            try {
                map.put(login, Long.parseLong(siteIdTexte));
            } catch (NumberFormatException ignore) {
                // MVP : on ignore les entrees invalides
            }
        }

        return map;
    }
}
