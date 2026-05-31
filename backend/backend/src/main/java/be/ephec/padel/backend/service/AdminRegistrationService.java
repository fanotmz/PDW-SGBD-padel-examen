package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.ValidateRegistrationRequest;
import be.ephec.padel.backend.dto.response.PendingRegistrationDto;
import be.ephec.padel.backend.dto.response.RegistrationDecisionResponse;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminRegistrationService {

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final JoueurService joueurService;
    private final MatriculeGeneratorService matriculeGeneratorService;

    public AdminRegistrationService(UserRepository userRepository,
                                    SiteRepository siteRepository,
                                    JoueurService joueurService,
                                    MatriculeGeneratorService matriculeGeneratorService) {
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.joueurService = joueurService;
        this.matriculeGeneratorService = matriculeGeneratorService;
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> listPendingRegistrations() {
        return userRepository.findByStatusOrderByIdAsc(UserStatus.PENDING).stream()
                .map(this::toPendingDto)
                .toList();
    }

    public RegistrationDecisionResponse validate(Long userId, ValidateRegistrationRequest request) {
        if (request == null) {
            throw new BusinessException("Requête de validation obligatoire");
        }

        User user = getPendingUser(userId);
        TypeJoueur finalType = request.getTypeAbonnementFinal();
        if (finalType == null) {
            throw new BusinessException("Type abonnement final obligatoire");
        }

        try {
            Site finalSite = resolveFinalSite(finalType, request.getSiteIdFinal());
            String matricule = matriculeGeneratorService.generateFor(finalType);
            Joueur joueur = joueurService.creerJoueur(
                    matricule,
                    user.getRequestedNom(),
                    finalType,
                    finalSite == null ? null : finalSite.getId()
            );

            user.setJoueur(joueur);
            user.setActive(true);
            user.setStatus(UserStatus.ACTIVE);
            user.addRole(SecurityRole.ROLE_JOUEUR);
            userRepository.save(user);

            return new RegistrationDecisionResponse(
                    user.getId(),
                    user.getLogin(),
                    user.getStatus(),
                    "Inscription validée. Le compte est maintenant actif.",
                    joueur.getMatricule(),
                    joueur.getNom(),
                    joueur.getType(),
                    joueur.getSite() == null ? null : joueur.getSite().getId()
            );
        } catch (DataIntegrityViolationException ex) {
            throw mapIntegrityViolation(ex);
        }
    }

    public RegistrationDecisionResponse reject(Long userId) {
        User user = getPendingUser(userId);
        user.setActive(false);
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);

        return new RegistrationDecisionResponse(
                user.getId(),
                user.getLogin(),
                user.getStatus(),
                "Inscription refusée. Le compte ne peut pas être utilisé.",
                null,
                null,
                null,
                null
        );
    }

    private User getPendingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("UserId obligatoire");
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Demande d'inscription introuvable"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException("Cette demande n'est plus en attente.");
        }

        return user;
    }

    private Site resolveFinalSite(TypeJoueur finalType, Long siteIdFinal) {
        if (finalType == TypeJoueur.SITE) {
            if (siteIdFinal == null) {
                throw new BusinessException("Le champ 'siteIdFinal' est obligatoire pour le type SITE.");
            }
            return siteRepository.findById(siteIdFinal)
                    .orElseThrow(() -> new NotFoundException("Site introuvable"));
        }

        if (siteIdFinal != null) {
            throw new BusinessException("Le champ 'siteIdFinal' est interdit pour le type " + finalType + ".");
        }

        return null;
    }

    private PendingRegistrationDto toPendingDto(User user) {
        Site requestedSite = user.getRequestedSite();
        return new PendingRegistrationDto(
                user.getId(),
                user.getLogin(),
                user.getRequestedNom(),
                user.getRequestedType(),
                requestedSite == null ? null : requestedSite.getId(),
                requestedSite == null ? null : requestedSite.getNom(),
                user.getStatus()
        );
    }

    private BusinessException mapIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        String normalized = message == null ? "" : message.toLowerCase();

        if (normalized.contains("joueur") && normalized.contains("primary key")) {
            return new BusinessException("Collision de matricule détectée. Veuillez relancer la validation.");
        }

        if (normalized.contains("joueur_id")) {
            return new BusinessException("Un joueur est déjà lié à un autre utilisateur.");
        }

        return new BusinessException("Erreur d'intégrité lors de la validation de l'inscription.");
    }
}
