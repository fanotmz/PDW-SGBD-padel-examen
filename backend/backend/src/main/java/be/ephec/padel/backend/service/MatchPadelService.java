package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class MatchPadelService {

    private final MatchPadelRepository matchPadelRepository;
    private final TerrainRepository terrainRepository;
    private final JoueurRepository joueurRepository;

    public MatchPadelService(MatchPadelRepository matchPadelRepository,
                             TerrainRepository terrainRepository,
                             JoueurRepository joueurRepository) {
        this.matchPadelRepository = matchPadelRepository;
        this.terrainRepository = terrainRepository;
        this.joueurRepository = joueurRepository;
    }

    public MatchPadel creerMatch(Long terrainId,
                                 String organisateurMatricule,
                                 LocalDateTime dateDebut,
                                 MatchVisibilite visibilite) {

        if (terrainId == null) throw new BusinessException("Terrain obligatoire");
        if (organisateurMatricule == null || organisateurMatricule.isBlank()) {
            throw new BusinessException("Organisateur obligatoire");
        }
        if (dateDebut == null) throw new BusinessException("Date début obligatoire");
        if (visibilite == null) throw new BusinessException("Visibilité obligatoire");

        // ✅ un seul "now" pour toute la méthode
        LocalDateTime now = LocalDateTime.now();

        if (!dateDebut.isAfter(now)) {
            throw new BusinessException("La date du match doit être dans le futur.");
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));

        Joueur organisateur = joueurRepository.findById(organisateurMatricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        // Règle: pas de réservation si dette > 0
        if (organisateur.getSolde() != null && organisateur.getSolde().signum() > 0) {
            throw new BusinessException("Réservation impossible : dette en cours (" + organisateur.getSolde() + ").");
        }

        verifierDroitReservation(organisateur, terrain, dateDebut, now);

        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, visibilite);
        return matchPadelRepository.save(match);
    }

    private void verifierDroitReservation(Joueur orga,
                                          Terrain terrain,
                                          LocalDateTime dateDebut,
                                          LocalDateTime now) {
        TypeJoueur type = orga.getType();
        if (type == null) throw new BusinessException("Type joueur manquant.");

        switch (type) {
            case GLOBAL -> {
                if (dateDebut.isAfter(now.plusWeeks(3))) {
                    throw new BusinessException("Un membre GLOBAL peut réserver au maximum 3 semaines à l'avance.");
                }
            }
            case SITE -> {
                if (dateDebut.isAfter(now.plusWeeks(2))) {
                    throw new BusinessException("Un membre SITE peut réserver au maximum 2 semaines à l'avance.");
                }
                if (orga.getSite() == null) {
                    throw new BusinessException("Joueur SITE sans site associé.");
                }
                Long siteJoueur = orga.getSite().getId();
                Long siteTerrain = terrain.getSite().getId();

                if (!siteJoueur.equals(siteTerrain)) {
                    throw new BusinessException("Un membre SITE ne peut réserver que sur son site.");
                }
            }
            case LIBRE -> {
                if (dateDebut.isAfter(now.plusDays(5))) {
                    throw new BusinessException("Un membre LIBRE peut réserver au maximum 5 jours à l'avance.");
                }
            }
            default -> throw new BusinessException("Type joueur inconnu.");
        }
    }

}

