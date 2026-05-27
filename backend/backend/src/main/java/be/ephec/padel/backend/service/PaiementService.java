package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CurrentUserFacade currentUserFacade;

    @Autowired
    public PaiementService(PaiementRepository paiementRepository,
                           ParticipationRepository participationRepository,
                           SoldeService soldeService,
                           Clock clock,
                           CurrentUserFacade currentUserFacade) {
        this.paiementRepository = paiementRepository;
        this.participationRepository = participationRepository;
        this.soldeService = soldeService;
        this.clock = clock;
        this.currentUserFacade = currentUserFacade;
    }

    public Paiement payerParticipation(Long participationId, BigDecimal montant) {
        Participation participation = getParticipationByIdOrThrow(participationId);
        verifierPaiementAutorise(participation);
        return payerParticipationInterne(participationId, montant, TypePaiement.ENCAISSEMENT, false);
    }

    public Paiement payerParticipationAvecRattrapageDette(Long participationId, BigDecimal montant) {
        return payerParticipationInterne(participationId, montant, TypePaiement.ENCAISSEMENT, true);
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

        Participation participation = getParticipationByIdOrThrow(participationId);

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
        if (autoriserRattrapageDette) {
            crediterParticipationEtRattrapage(participation, m, restePart);
        } else {
            Long matchId = participation.getMatch() != null ? participation.getMatch().getId() : null;
            soldeService.crediter(
                    matricule,
                    m,
                    new SoldeOriginContext(
                            OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION,
                            participation.getId(),
                            matchId,
                            null
                    )
            );
        }

        return saved;
    }

    private void crediterParticipationEtRattrapage(Participation participation,
                                                   BigDecimal montantTotal,
                                                   BigDecimal restePart) {
        String matricule = participation.getJoueur().getMatricule();
        Long matchId = participation.getMatch() != null ? participation.getMatch().getId() : null;

        BigDecimal montantParticipation = montantTotal.min(restePart).setScale(2, RoundingMode.HALF_UP);
        if (montantParticipation.signum() > 0) {
            soldeService.crediter(
                    matricule,
                    montantParticipation,
                    new SoldeOriginContext(
                            OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION,
                            participation.getId(),
                            matchId,
                            null
                    )
            );
        }

        BigDecimal montantRattrapage = montantTotal.subtract(montantParticipation).setScale(2, RoundingMode.HALF_UP);
        if (montantRattrapage.signum() > 0) {
            soldeService.crediter(
                    matricule,
                    montantRattrapage,
                    new SoldeOriginContext(
                            OrigineMouvementSoldeType.RATTRAPAGE_DETTE,
                            null,
                            null,
                            null
                    )
            );
        }
    }

    private Participation getParticipationByIdOrThrow(Long participationId) {
        return participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException("Participation introuvable"));
    }

    private void verifierPaiementAutorise(Participation participation) {
        String joueurCourantMatricule = currentUserFacade.getCurrentJoueur().getMatricule();
        String joueurParticipationMatricule = participation.getJoueur().getMatricule();
        if (!joueurCourantMatricule.equals(joueurParticipationMatricule)) {
            throw new ForbiddenException("Seul le participant concerné peut payer sa participation.");
        }
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
