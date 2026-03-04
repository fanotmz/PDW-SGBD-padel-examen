package be.ephec.padel.backend.service.securite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceAutorisationAdmin {

    private final Map<String, Long> perimetreAdminsSiteParLogin;

    public ServiceAutorisationAdmin(
            @Value("${app.security.admin.site.users:adminSite1:1,adminSite2:2}") String adminsSite
    ) {
        this.perimetreAdminsSiteParLogin = parserAdminsSite(adminsSite);
    }

    public void verifierAccesAuSite(Long siteDemandeId) {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null || authentification.getAuthorities() == null) {
            throw new AccessDeniedException("Accès refusé");
        }

        // ADMIN_GLOBAL => accès à tous les sites
        if (aLeRole(authentification, "ROLE_ADMIN_GLOBAL")) {
            return;
        }

        // ADMIN_SITE => accès uniquement à son site
        if (aLeRole(authentification, "ROLE_ADMIN_SITE")) {
            String login = authentification.getName();
            Long siteAutoriseId = perimetreAdminsSiteParLogin.get(login);

            if (siteAutoriseId == null) {
                throw new AccessDeniedException("ADMIN_SITE sans site associé");
            }
            if (!siteAutoriseId.equals(siteDemandeId)) {
                throw new AccessDeniedException("Accès interdit : site non autorisé");
            }
            return;
        }

        throw new AccessDeniedException("Accès refusé");
    }

    private boolean aLeRole(Authentication authentification, String role) {
        for (GrantedAuthority autorite : authentification.getAuthorities()) {
            if (role.equals(autorite.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Long> parserAdminsSite(String adminsSite) {
        Map<String, Long> map = new HashMap<>();
        if (adminsSite == null || adminsSite.isBlank()) return map;

        String[] entrees = adminsSite.split(",");
        for (String entree : entrees) {
            String nettoyee = entree.trim();
            if (nettoyee.isEmpty()) continue;

            String[] parties = nettoyee.split(":");
            if (parties.length < 2) continue;

            String login = parties[0].trim();
            String siteIdTexte = parties[1].trim();

            try {
                map.put(login, Long.parseLong(siteIdTexte));
            } catch (NumberFormatException ignore) {
                // MVP : on ignore les entrées invalides
            }
        }

        return map;
    }
}