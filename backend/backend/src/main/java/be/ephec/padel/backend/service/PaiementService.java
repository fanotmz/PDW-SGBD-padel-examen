package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
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

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

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

        Paiement saved = paiementRepository.save(new Paiement(participation, m, LocalDateTime.now()));

        String matricule = participation.getJoueur().getMatricule();
        soldeService.crediter(matricule, m);

        return saved;
    }

    public Paiement payerPourMatch(Long matchId, String joueurMatricule, BigDecimal montant) {
        Participation participation = participationRepository
                .findByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)
                .orElseThrow(() -> new NotFoundException("Participation introuvable pour ce match/joueur"));

        return payerParticipation(participation.getId(), montant);
    }

    public Paiement payerParticipationAvecRattrapageDette(Long participationId, BigDecimal montant) {
        Participation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new NotFoundException("Participation introuvable"));

        BigDecimal m = validerMontant(montant);

        String matricule = participation.getJoueur().getMatricule();

        // Dette actuelle du joueur (dans votre modèle: solde > 0 = dette)
        BigDecimal dette = participation.getJoueur().getSolde();
        if (dette == null) dette = BigDecimal.ZERO;
        dette = dette.setScale(2, RoundingMode.HALF_UP);

        // Ce qu'il reste à payer pour cette participation (max 15 au total)
        BigDecimal dejaPaye = paiementRepository.sumMontantByParticipationId(participationId);
        if (dejaPaye == null) dejaPaye = BigDecimal.ZERO;
        dejaPaye = dejaPaye.setScale(2, RoundingMode.HALF_UP);

        BigDecimal restePart = PART_JOUEUR.subtract(dejaPaye).setScale(2, RoundingMode.HALF_UP);
        if (restePart.signum() <= 0) {
            throw new BusinessException("Participation déjà payée en totalité.");
        }

        // Montant total dû = reste de la part (<=15) + dette existante
        BigDecimal totalDu = restePart.add(dette).setScale(2, RoundingMode.HALF_UP);

        // Si pas de dette, on reste strict (comme avant): impossible de payer plus que la part restante
        // Si dette > 0, on autorise de payer jusqu'à (restePart + dette)
        if (m.compareTo(totalDu) > 0) {
            throw new BusinessException("Paiement trop élevé. Total dû (part + dette) = " + totalDu);
        }

        // On enregistre le paiement tel quel (peut être > 15 si dette > 0)
        Paiement saved = paiementRepository.save(new Paiement(participation, m, LocalDateTime.now()));

        // On crédite la totalité: cela rembourse d'abord la dette puis la part (selon votre logique soldeService)
        soldeService.crediter(matricule, m);

        return saved;
    }
    public Paiement payerPourMatchAvecRattrapageDette(Long matchId, String joueurMatricule, BigDecimal montant) {
        Participation participation = participationRepository
                .findByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)
                .orElseThrow(() -> new NotFoundException("Participation introuvable pour ce match/joueur"));

        return payerParticipationAvecRattrapageDette(participation.getId(), montant);
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