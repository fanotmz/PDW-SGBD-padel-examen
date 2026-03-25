package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class PaiementService {

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final PaiementRepository paiementRepository;
    private final ParticipationRepository participationRepository;
    private final SoldeService soldeService;
    private final Clock clock;

    public PaiementService(PaiementRepository paiementRepository,
                           ParticipationRepository participationRepository,
                           SoldeService soldeService,
                           Clock clock) {
        this.paiementRepository = paiementRepository;
        this.participationRepository = participationRepository;
        this.soldeService = soldeService;
        this.clock = clock;
    }

    public Paiement payerParticipation(Long participationId, BigDecimal montant) {
        return payerParticipationInterne(participationId, montant, TypePaiement.ENCAISSEMENT, false);
    }

    public Paiement payerPourMatch(Long matchId, String joueurMatricule, BigDecimal montant) {
        Participation participation = getParticipationOrThrow(matchId, joueurMatricule);
        return payerParticipation(participation.getId(), montant);
    }

    public Paiement payerParticipationAvecRattrapageDette(Long participationId, BigDecimal montant) {
        return payerParticipationInterne(participationId, montant, TypePaiement.ENCAISSEMENT, true);
    }

    public Paiement payerPourMatchAvecRattrapageDette(Long matchId, String joueurMatricule, BigDecimal montant) {
        Participation participation = getParticipationOrThrow(matchId, joueurMatricule);
        return payerParticipationAvecRattrapageDette(participation.getId(), montant);
    }

    Paiement enregistrerRemboursementAnnulation(Participation participation, BigDecimal montantRembourse) {
        if (participation == null) {
            throw new BusinessException("Participation obligatoire");
        }
        if (participation.getMatch() == null || participation.getMatch().getStatut() != MatchStatut.ANNULE) {
            throw new BusinessException("Remboursement impossible : le match doit être annulé");
        }

        BigDecimal montant = validerMontant(
                montantRembourse == null ? null : montantRembourse.negate(),
                TypePaiement.REMBOURSEMENT
        );

        return paiementRepository.save(new Paiement(
                participation,
                montant,
                TypePaiement.REMBOURSEMENT,
                LocalDateTime.now(clock)
        ));
    }

    private Paiement payerParticipationInterne(Long participationId,
                                               BigDecimal montant,
                                               TypePaiement typePaiement,
                                               boolean autoriserRattrapageDette) {

        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException("Participation introuvable"));

        if (participation.getMatch() != null && participation.getMatch().getStatut() == MatchStatut.ANNULE) {
            throw new BusinessException("Match annulé : aucun paiement n'est possible.");
        }

        BigDecimal m = validerMontant(montant, typePaiement);

        BigDecimal dejaPaye = getDejaPaye(participationId);
        BigDecimal restePart = PART_JOUEUR.subtract(dejaPaye).setScale(2, RoundingMode.HALF_UP);

        if (restePart.signum() <= 0) {
            throw new BusinessException("Participation déjà payée en totalité.");
        }

        BigDecimal montantMaximumAutorise = restePart;

        if (autoriserRattrapageDette) {
            BigDecimal dette = getDetteJoueur(participation);
            montantMaximumAutorise = restePart.add(dette).setScale(2, RoundingMode.HALF_UP);
        }

        if (m.compareTo(montantMaximumAutorise) > 0) {
            if (autoriserRattrapageDette) {
                throw new BusinessException(
                        "Paiement trop élevé. Total dû (part + dette) = " + montantMaximumAutorise
                );
            }
            throw new BusinessException(
                    "Paiement trop élevé. Reste à payer = " + montantMaximumAutorise
            );
        }

        Paiement saved = paiementRepository.save(new Paiement(participation, m, typePaiement, LocalDateTime.now(clock)));

        String matricule = participation.getJoueur().getMatricule();
        soldeService.crediter(matricule, m);

        return saved;
    }

    private Participation getParticipationOrThrow(Long matchId, String joueurMatricule) {
        return participationRepository
                .findByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)
                .orElseThrow(() -> new NotFoundException("Participation introuvable pour ce match/joueur"));
    }

    private BigDecimal getDejaPaye(Long participationId) {
        BigDecimal dejaPaye = paiementRepository.sumMontantByParticipationId(participationId);
        if (dejaPaye == null) {
            dejaPaye = BigDecimal.ZERO;
        }
        return dejaPaye.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDetteJoueur(Participation participation) {
        BigDecimal dette = participation.getJoueur().getSolde();
        if (dette == null) {
            dette = BigDecimal.ZERO;
        }
        return dette.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal validerMontant(BigDecimal montant, TypePaiement typePaiement) {
        if (montant == null) {
            throw new BusinessException("Montant obligatoire");
        }
        if (typePaiement == null) {
            throw new BusinessException("Type de paiement obligatoire");
        }
        if (typePaiement == TypePaiement.ENCAISSEMENT && montant.signum() <= 0) {
            throw new BusinessException("Montant invalide");
        }
        if (typePaiement == TypePaiement.REMBOURSEMENT && montant.signum() >= 0) {
            throw new BusinessException("Montant de remboursement invalide");
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
