package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class JoueurService {

    private final JoueurRepository joueurRepository;
    private final SiteRepository siteRepository;

    public JoueurService(JoueurRepository joueurRepository, SiteRepository siteRepository) {
        this.joueurRepository = joueurRepository;
        this.siteRepository = siteRepository;
    }

    public Joueur creerJoueur(String matricule, String nom, TypeJoueur type, Long siteId) {
        if (matricule == null || matricule.isBlank()) throw new BusinessException("Matricule obligatoire");
        if (nom == null || nom.isBlank()) throw new BusinessException("Nom obligatoire");
        if (type == null) throw new BusinessException("Type joueur obligatoire");

        verifierMatricule(type, matricule);

        if (joueurRepository.existsById(matricule)) {
            throw new BusinessException("Matricule déjà utilisé");
        }

        Site site = null;

        if (type == TypeJoueur.SITE) {
            if (siteId == null) throw new BusinessException("Un joueur SITE doit être lié à un site");
            site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new NotFoundException("Site introuvable"));
        } else {
            // Correction : éviter GLOBAL/LIBRE rattachés par erreur à un site
            if (siteId != null) {
                throw new BusinessException("Seul un joueur SITE peut avoir un site.");
            }
        }

        Joueur joueur = new Joueur(matricule, nom, type, site);
        joueur.setSolde(BigDecimal.ZERO);
        return joueurRepository.save(joueur);
    }

    public Joueur getJoueur(String matricule) {
        return joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));
    }

    public List<Joueur> lister() {
        return joueurRepository.findAll();
    }

    public boolean aDette(String matricule) {
        Joueur joueur = getJoueur(matricule);
        return joueur.getSolde() != null && joueur.getSolde().compareTo(BigDecimal.ZERO) > 0;
    }

    public void verifierPasDeDette(String matricule) {
        if (aDette(matricule)) {
            throw new BusinessException("Action impossible : solde dû (dette) non réglé.");
        }
    }

    private void verifierMatricule(TypeJoueur type, String matricule) {
        String pattern;
        switch (type) {
            case GLOBAL -> pattern = "^G\\d{4}$";
            case SITE -> pattern = "^S\\d{4}$";
            case LIBRE -> pattern = "^L\\d{4}$";
            default -> throw new BusinessException("Type joueur inconnu");
        }

        if (!matricule.matches(pattern)) {
            throw new BusinessException("Matricule invalide pour le type " + type + " : " + matricule);
        }
    }
}
