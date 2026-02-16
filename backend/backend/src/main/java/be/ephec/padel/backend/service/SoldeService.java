package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MouvementSolde;
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
        BigDecimal m = validateMontant(montant);

        Joueur joueur = joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        BigDecimal detteAvant = nullSafe(joueur.getSolde());

        if (m.compareTo(detteAvant) > 0) {
            throw new BusinessException("Paiement trop élevé. Dette actuelle = " + detteAvant);
        }

        joueur.setSolde(detteAvant.subtract(m));
        joueurRepository.save(joueur);

        mouvementSoldeRepository.save(new MouvementSolde(
                LocalDateTime.now(),
                m,
                TypeMouvement.CREDIT,
                joueur
        ));
    }

    // DEBIT = dette : augmente la dette
    public void debiter(String matricule, BigDecimal montant) {
        BigDecimal m = validateMontant(montant);

        Joueur joueur = joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        BigDecimal detteAvant = nullSafe(joueur.getSolde());

        joueur.setSolde(detteAvant.add(m));
        joueurRepository.save(joueur);

        mouvementSoldeRepository.save(new MouvementSolde(
                LocalDateTime.now(),
                m,
                TypeMouvement.DEBIT,
                joueur
        ));
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
}
