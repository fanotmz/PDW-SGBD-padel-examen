package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.RegisterRequest;
import be.ephec.padel.backend.dto.response.RegisterResponse;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrationService {

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               SiteRepository siteRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException("Requête d'inscription obligatoire");
        }

        String username = normalize(request.getUsername(), "Username obligatoire");
        String password = normalize(request.getPassword(), "Password obligatoire");
        String nom = normalize(request.getNom(), "Nom obligatoire");

        if (userRepository.existsByLogin(username)) {
            throw new BusinessException("Username deja utilise");
        }

        TypeJoueur requestedType = request.getTypeAbonnementDemande();
        if (requestedType == null) {
            throw new BusinessException("Type abonnement demande obligatoire");
        }

        Site requestedSite = resolveSiteForType(
                requestedType,
                request.getSiteIdDemande(),
                "typeAbonnementDemande",
                "siteIdDemande"
        );

        User user = new User();
        user.setLogin(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(false);
        user.setStatus(UserStatus.PENDING);
        user.setRequestedNom(nom);
        user.setRequestedType(requestedType);
        user.setRequestedSite(requestedSite);
        user.addRole(SecurityRole.ROLE_JOUEUR);

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getLogin(),
                savedUser.getStatus(),
                "Demande d'inscription en attente de validation administrateur."
        );
    }

    private Site resolveSiteForType(TypeJoueur type,
                                    Long siteId,
                                    String typeField,
                                    String siteField) {
        if (type == TypeJoueur.SITE) {
            if (siteId == null) {
                throw new BusinessException("Le champ '" + siteField + "' est obligatoire pour le type SITE.");
            }
            return siteRepository.findById(siteId)
                    .orElseThrow(() -> new NotFoundException("Site introuvable"));
        }

        if (siteId != null) {
            throw new BusinessException("Le champ '" + siteField + "' est interdit pour le type " + type + ".");
        }

        if (type != TypeJoueur.GLOBAL && type != TypeJoueur.LIBRE) {
            throw new BusinessException("Valeur invalide pour '" + typeField + "'.");
        }

        return null;
    }

    private String normalize(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return value.trim();
    }
}
