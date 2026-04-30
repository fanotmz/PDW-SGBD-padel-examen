package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.MatchStatut;
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

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final MatchPadelRepository matchPadelRepository;
    private final PaiementRepository paiementRepository;
    private final PaiementService paiementService;
    private final SoldeService soldeService;
    private final Clock clock;

    public AnnulationMatchService(MatchPadelRepository matchPadelRepository,
                                  PaiementRepository paiementRepository,
                                  PaiementService paiementService,
                                  SoldeService soldeService,
                                  Clock clock) {
        this.matchPadelRepository = matchPadelRepository;
        this.paiementRepository = paiementRepository;
        this.paiementService = paiementService;
        this.soldeService = soldeService;
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

        match.setStatut(MatchStatut.ANNULE);

        for (Participation participation : match.getParticipations()) {
            compenserParticipation(participation);
        }

        matchPadelRepository.save(match);
        return true;
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

    private BigDecimal nullSafe(BigDecimal montant) {
        if (montant == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
