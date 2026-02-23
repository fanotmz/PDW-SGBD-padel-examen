package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class MatchPadelService {

    private final MatchPadelRepository matchPadelRepository;
    private final TerrainRepository terrainRepository;
    private final JoueurRepository joueurRepository;
    private final SoldeService soldeService;
    private final ParticipationRepository participationRepository;
    private final PaiementService paiementService;
    private final PaiementRepository paiementRepository;

    public MatchPadelService(MatchPadelRepository matchPadelRepository,
                             TerrainRepository terrainRepository,
                             JoueurRepository joueurRepository,
                             SoldeService soldeService,
                             ParticipationRepository participationRepository,
                             PaiementService paiementService,
                             PaiementRepository paiementRepository) {
        this.matchPadelRepository = matchPadelRepository;
        this.terrainRepository = terrainRepository;
        this.joueurRepository = joueurRepository;
        this.soldeService = soldeService;
        this.participationRepository = participationRepository;
        this.paiementService = paiementService;
        this.paiementRepository = paiementRepository;
    }

    @Transactional(readOnly = true)
    public MatchPadel getMatch(Long id) {
        return matchPadelRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    @Transactional(readOnly = true)
    public MatchDto getMatchDto(Long id) {
        MatchPadel m = getMatch(id);

        BigDecimal montantTotal = Tarifs.PRIX_MATCH;

        BigDecimal montantPaye = paiementRepository.sumMontantByMatchId(id);
        if (montantPaye == null) montantPaye = BigDecimal.ZERO;

        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);
        if (resteAPayer.signum() < 0) resteAPayer = BigDecimal.ZERO;

        Terrain terrain = m.getTerrain();
        Long terrainId = (terrain != null) ? terrain.getId() : null;
        String terrainNom = (terrain != null) ? terrain.getNom() : null;
        Long siteId = (terrain != null && terrain.getSite() != null) ? terrain.getSite().getId() : null;

        String organisateurMatricule = (m.getOrganisateur() != null) ? m.getOrganisateur().getMatricule() : null;

        int nbParticipants = (m.getParticipations() != null) ? m.getParticipations().size() : 0;

        return new MatchDto(
                m.getId(),
                terrainId,
                terrainNom,
                siteId,
                organisateurMatricule,
                m.getDateDebut(),
                m.getVisibilite(),
                nbParticipants,
                montantTotal,
                montantPaye,
                resteAPayer
        );
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

        LocalDateTime now = LocalDateTime.now();
        if (!dateDebut.isAfter(now)) {
            throw new BusinessException("La date du match doit être dans le futur.");
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));

        Joueur organisateur = joueurRepository.findById(organisateurMatricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        if (organisateur.getSolde() != null && organisateur.getSolde().signum() > 0) {
            throw new BusinessException("Réservation impossible : dette en cours (" + organisateur.getSolde() + ").");
        }

        verifierDroitReservation(organisateur, terrain, dateDebut, now);

        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, visibilite);
        MatchPadel saved = matchPadelRepository.save(match);

        // organisateur = participant
        Participation pOrg = participationRepository.save(new Participation(saved, organisateur));

        // payé à l’avance : dette puis paiement immédiat
        BigDecimal part = Tarifs.PART_PAR_JOUEUR;
        soldeService.debiter(organisateurMatricule, part);
        paiementService.payerParticipation(pOrg.getId(), part);

        return saved;
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