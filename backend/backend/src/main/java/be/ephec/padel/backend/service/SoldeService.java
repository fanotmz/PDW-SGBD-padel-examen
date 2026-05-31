package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MouvementSolde;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypeMouvement;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
public class SoldeService {

    private final JoueurRepository joueurRepository;
    private final MouvementSoldeRepository mouvementSoldeRepository;

    public SoldeService(JoueurRepository joueurRepository,
                        MouvementSoldeRepository mouvementSoldeRepository) {
        this.joueurRepository = joueurRepository;
        this.mouvementSoldeRepository = mouvementSoldeRepository;
    }

    public void crediter(String matricule, BigDecimal montant) {
        crediter(matricule, montant, SoldeOriginContext.legacy());
    }

    public void crediter(String matricule, BigDecimal montant, SoldeOriginContext context) {
        BigDecimal m = validateMontant(montant);
        SoldeOriginContext originContext = safeContext(context);

        Joueur joueur = joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        BigDecimal detteAvant = nullSafe(joueur.getSolde());

        if (m.compareTo(detteAvant) > 0) {
            throw new BusinessException("Paiement trop élevé. Dette actuelle = " + detteAvant);
        }

        joueur.setSolde(detteAvant.subtract(m));
        joueurRepository.save(joueur);

        mouvementSoldeRepository.save(buildMouvement(TypeMouvement.CREDIT, joueur, m, originContext));
    }

    public void debiter(String matricule, BigDecimal montant) {
        debiter(matricule, montant, SoldeOriginContext.legacy());
    }

    public void debiter(String matricule, BigDecimal montant, SoldeOriginContext context) {
        BigDecimal m = validateMontant(montant);
        SoldeOriginContext originContext = safeContext(context);

        Joueur joueur = joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        BigDecimal detteAvant = nullSafe(joueur.getSolde());

        joueur.setSolde(detteAvant.add(m));
        joueurRepository.save(joueur);

        mouvementSoldeRepository.save(buildMouvement(TypeMouvement.DEBIT, joueur, m, originContext));
    }

    private BigDecimal validateMontant(BigDecimal montant) {
        if (montant == null || montant.signum() <= 0) {
            throw new BusinessException("Montant invalide");
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private SoldeOriginContext safeContext(SoldeOriginContext context) {
        return context == null ? SoldeOriginContext.legacy() : context;
    }

    private MouvementSolde buildMouvement(TypeMouvement type,
                                          Joueur joueur,
                                          BigDecimal montant,
                                          SoldeOriginContext context) {
        return new MouvementSolde(
                LocalDateTime.now(),
                montant,
                type,
                joueur,
                context.getParticipationId(),
                context.getMatchId(),
                context.getOrigineType() == null ? OrigineMouvementSoldeType.LEGACY : context.getOrigineType(),
                context.getDescription()
        );
    }
}
