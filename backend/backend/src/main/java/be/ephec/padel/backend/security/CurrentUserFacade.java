package be.ephec.padel.backend.security;

import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurrentUserFacade {

    private final UserRepository userRepository;

    public CurrentUserFacade(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ForbiddenException("Utilisateur authentifie requis.");
        }

        String login = authentication.getName();
        if (login == null || login.isBlank()) {
            throw new ForbiddenException("Utilisateur authentifie introuvable.");
        }

        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ForbiddenException("Utilisateur authentifie introuvable."));
    }

    public Joueur getCurrentJoueur() {
        Joueur joueur = getCurrentUser().getJoueur();
        if (joueur == null) {
            throw new ForbiddenException("Aucun joueur lie a l'utilisateur authentifie.");
        }
        return joueur;
    }

    public boolean hasRole(SecurityRole role) {
        if (role == null) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.name().equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    public boolean isAdmin() {
        return hasRole(SecurityRole.ROLE_ADMIN_GLOBAL) || hasRole(SecurityRole.ROLE_ADMIN_SITE);
    }
}
