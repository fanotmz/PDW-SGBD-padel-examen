package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
public class PaiementService {

    private static final BigDecimal PRIX_MATCH = new BigDecimal("60.00");
    private static final BigDecimal PART_JOUEUR = PRIX_MATCH.divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP); // 15.00

    private final PaiementRepository paiementRepository;
    private final ParticipationRepository participationRepository;
    private final SoldeService soldeService;

    public PaiementService(PaiementRepository paiementRepository,
                           ParticipationRepository participationRepository,
                           SoldeService soldeService) {
        this.paiementRepository = paiementRepository;
        this.participationRepository = participationRepository;
        this.soldeService = soldeService;
    }

    public Paiement payerParticipation(Long participationId, BigDecimal montant) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException("Participation introuvable"));

        BigDecimal m = validerMontant(montant);

        // Total déjà payé pour cette participation (0 si aucun paiement)
        BigDecimal dejaPaye = paiementRepository.sumMontantByParticipationId(participationId);
        if (dejaPaye == null) dejaPaye = BigDecimal.ZERO;
        dejaPaye = dejaPaye.setScale(2, RoundingMode.HALF_UP);

        BigDecimal reste = PART_JOUEUR.subtract(dejaPaye).setScale(2, RoundingMode.HALF_UP);

        if (reste.signum() <= 0) {
            throw new BusinessException("Participation déjà payée en totalité.");
        }
        if (m.compareTo(reste) > 0) {
            throw new BusinessException("Paiement trop élevé. Reste à payer = " + reste);
        }

        // 1) Enregistrer le paiement (trace)
        Paiement saved = paiementRepository.save(new Paiement(participation, m, LocalDateTime.now()));

        // 2) Réduire la dette du joueur
        String matricule = participation.getJoueur().getMatricule();
        soldeService.crediter(matricule, m);

        return saved;
    }

    /**
     * Paiement en donnant matchId + matricule.
     */
    public Paiement payerPourMatch(Long matchId, String joueurMatricule, BigDecimal montant) {
        Participation participation = participationRepository
                .findByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)
                .orElseThrow(() -> new NotFoundException("Participation introuvable pour ce match/joueur"));

        return payerParticipation(participation.getId(), montant);
    }

    private BigDecimal validerMontant(BigDecimal montant) {
        if (montant == null) {
            throw new BusinessException("Montant obligatoire");
        }
        if (montant.signum() <= 0) {
            throw new BusinessException("Montant invalide");
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
