package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AnnulationMatchService {

    private static final BigDecimal PRIX_MATCH = Tarifs.PRIX_MATCH;
    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final MatchPadelRepository matchPadelRepository;
    private final PaiementRepository paiementRepository;
    private final PaiementService paiementService;
    private final SoldeService soldeService;
    private final SoldeImputationService soldeImputationService;
    private final PenaliteJoueurService penaliteJoueurService;
    private final Clock clock;

    public AnnulationMatchService(MatchPadelRepository matchPadelRepository,
                                  PaiementRepository paiementRepository,
                                  PaiementService paiementService,
                                  SoldeService soldeService,
                                  SoldeImputationService soldeImputationService,
                                  PenaliteJoueurService penaliteJoueurService,
                                  Clock clock) {
        this.matchPadelRepository = matchPadelRepository;
        this.paiementRepository = paiementRepository;
        this.paiementService = paiementService;
        this.soldeService = soldeService;
        this.soldeImputationService = soldeImputationService;
        this.penaliteJoueurService = penaliteJoueurService;
        this.clock = clock;
    }

    public int annulerMatchsFutursPlanifiesSite(Long siteId,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now(clock);

        List<Long> matchIds = matchPadelRepository
                .findPlannedFutureMatchesBySiteIdAndDateDebutBetween(
                        siteId,
                        MatchStatut.PLANIFIE,
                        now,
                        from,
                        to
                ).stream()
                .map(MatchPadel::getId)
                .toList();

        int nbAnnules = 0;
        for (Long matchId : matchIds) {
            if (annulerMatchSiPlanifie(matchId)) {
                nbAnnules++;
            }
        }

        return nbAnnules;
    }

    public int annulerMatchsFutursPlanifiesTousSites(LocalDateTime from,
                                                     LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now(clock);

        List<Long> matchIds = matchPadelRepository
                .findPlannedFutureMatchesByDateDebutBetween(
                        MatchStatut.PLANIFIE,
                        now,
                        from,
                        to
                ).stream()
                .map(MatchPadel::getId)
                .toList();

        int nbAnnules = 0;
        for (Long matchId : matchIds) {
            if (annulerMatchSiPlanifie(matchId)) {
                nbAnnules++;
            }
        }

        return nbAnnules;
    }

    public boolean annulerMatchSiPlanifie(Long matchId) {
        MatchPadel match = matchPadelRepository.findByIdForUpdateWithParticipations(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));

        LocalDateTime now = LocalDateTime.now(clock);
        if (match.getStatut() != MatchStatut.PLANIFIE) {
            return false;
        }
        if (match.getDateDebut() == null || !match.getDateDebut().isAfter(now)) {
            return false;
        }

        annulerMatchCharge(match, ModeAnnulationMatch.ADMIN_OU_FERMETURE);
        return true;
    }

    public void annulerMatch(Long matchId, ModeAnnulationMatch mode) {
        MatchPadel match = matchPadelRepository.findByIdForUpdateWithParticipations(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));

        if (match.getStatut() != MatchStatut.PLANIFIE) {
            throw new BusinessException("Seul un match planifié peut être annulé.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (match.getDateDebut() == null || !match.getDateDebut().isAfter(now)) {
            throw new BusinessException("Seul un match futur peut être annulé.");
        }

        annulerMatchCharge(match, mode);
    }

    private void annulerMatchCharge(MatchPadel match, ModeAnnulationMatch mode) {
        ModeAnnulationMatch modeEffectif = mode == null ? ModeAnnulationMatch.ADMIN_OU_FERMETURE : mode;

        match.setStatut(MatchStatut.ANNULE);

        if (modeEffectif == ModeAnnulationMatch.ORGANISATEUR_TARDIVE) {
            Participation participationOrganisateur = getParticipationOrganisateur(match);

            for (Participation participation : match.getParticipations()) {
                if (!isSameParticipation(participation, participationOrganisateur)) {
                    compenserParticipation(participation);
                }
            }

            appliquerChargeOrganisateurTardive(match, participationOrganisateur);
            penaliteJoueurService.appliquerPenaliteReservation(match.getOrganisateur());
        } else {
            for (Participation participation : match.getParticipations()) {
                compenserParticipation(participation);
            }
        }

        matchPadelRepository.save(match);
    }

    private void compenserParticipation(Participation participation) {
        BigDecimal montantPaye = nullSafe(paiementRepository.sumMontantByParticipationId(participation.getId()));
        BigDecimal montantRembourse = montantPaye.min(PART_JOUEUR).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal detteAAnnuler = PART_JOUEUR.subtract(montantRembourse).setScale(2, RoundingMode.HALF_UP);

        Joueur joueur = participation.getJoueur();
        BigDecimal detteActuelle = nullSafe(joueur.getSolde());
        BigDecimal montantACrediter = detteAAnnuler.min(detteActuelle).setScale(2, RoundingMode.HALF_UP);

        if (montantACrediter.signum() > 0) {
            soldeService.crediter(
                    joueur.getMatricule(),
                    montantACrediter,
                    new SoldeOriginContext(
                            OrigineMouvementSoldeType.ANNULATION_MATCH_NEUTRALISATION,
                            participation.getId(),
                            participation.getMatch() != null ? participation.getMatch().getId() : null,
                            null
                    )
            );
        }

        if (montantRembourse.signum() > 0) {
            paiementService.enregistrerRemboursementAnnulation(participation, montantRembourse);
        }
    }

    private Participation getParticipationOrganisateur(MatchPadel match) {
        if (match == null || match.getOrganisateur() == null || match.getParticipations() == null) {
            throw new BusinessException("Participation organisateur introuvable");
        }

        String matriculeOrganisateur = match.getOrganisateur().getMatricule();
        return match.getParticipations().stream()
                .filter(participation -> participation.getJoueur() != null)
                .filter(participation -> matriculeOrganisateur.equals(participation.getJoueur().getMatricule()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Participation organisateur introuvable"));
    }

    private boolean isSameParticipation(Participation left, Participation right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        return left == right;
    }

    private void appliquerChargeOrganisateurTardive(MatchPadel match, Participation participationOrganisateur) {
        Long participationId = participationOrganisateur.getId();
        Joueur organisateur = participationOrganisateur.getJoueur();

        BigDecimal encaissements = nullSafe(paiementRepository.sumMontantByParticipationIdAndType(
                participationId,
                TypePaiement.ENCAISSEMENT
        ));
        BigDecimal encaissementAffecteMatch = encaissements.min(PART_JOUEUR).setScale(2, RoundingMode.HALF_UP);

        BigDecimal detteOuverteParticipation = soldeImputationService.getMontantOuvertPourParticipation(
                organisateur.getMatricule(),
                participationId
        );

        BigDecimal chargeDejaPortee = encaissementAffecteMatch
                .add(nullSafe(detteOuverteParticipation))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal complement = PRIX_MATCH.subtract(chargeDejaPortee).setScale(2, RoundingMode.HALF_UP);

        if (complement.signum() <= 0) {
            return;
        }

        soldeService.debiter(
                organisateur.getMatricule(),
                complement,
                new SoldeOriginContext(
                        OrigineMouvementSoldeType.ANNULATION_TARDIVE_ORGANISATEUR,
                        participationId,
                        match.getId(),
                        "Annulation tardive organisateur"
                )
        );
    }

    private BigDecimal nullSafe(BigDecimal montant) {
        if (montant == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
