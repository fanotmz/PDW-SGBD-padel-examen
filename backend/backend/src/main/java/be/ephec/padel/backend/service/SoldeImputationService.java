package be.ephec.padel.backend.service;

import be.ephec.padel.backend.model.entities.MouvementSolde;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypeMouvement;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.service.model.ImputationResult;
import be.ephec.padel.backend.service.model.OpenDebtLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SoldeImputationService {

    private static final EnumSet<OrigineMouvementSoldeType> TRACKED_DEBIT_ORIGINS = EnumSet.of(
            OrigineMouvementSoldeType.CREATION_MATCH_ORGANISATEUR,
            OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE,
            OrigineMouvementSoldeType.REJOINDRE_MATCH_PUBLIC_PART
    );

    private static final EnumSet<OrigineMouvementSoldeType> TRACKED_CREDIT_ORIGINS = EnumSet.of(
            OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION,
            OrigineMouvementSoldeType.TRAITEMENT_J1_NEUTRALISATION,
            OrigineMouvementSoldeType.ANNULATION_MATCH_NEUTRALISATION
    );

    private final MouvementSoldeRepository mouvementSoldeRepository;

    public SoldeImputationService(MouvementSoldeRepository mouvementSoldeRepository) {
        this.mouvementSoldeRepository = mouvementSoldeRepository;
    }

    public ImputationResult reconstruirePourJoueur(String matricule) {
        List<MouvementSolde> mouvements =
                mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc(matricule);
        return reconstruirePourMouvements(mouvements);
    }

    ImputationResult reconstruirePourMouvements(List<MouvementSolde> mouvements) {
        Map<Long, List<OpenDebtLine>> dettesParParticipation = construireDettesParParticipation(mouvements);
        List<OpenDebtLine> openDebtLines = new ArrayList<>();

        for (List<OpenDebtLine> lines : dettesParParticipation.values()) {
            for (OpenDebtLine line : lines) {
                if (line.isOpen()) {
                    openDebtLines.add(line);
                }
            }
        }

        return new ImputationResult(openDebtLines, calculerTotalOuvert(dettesParParticipation.values()));
    }

    private Map<Long, List<OpenDebtLine>> construireDettesParParticipation(List<MouvementSolde> mouvements) {
        Map<Long, List<OpenDebtLine>> dettesParParticipation = new LinkedHashMap<>();

        for (MouvementSolde mouvement : mouvements) {
            if (estDebitCibleParticipation(mouvement)) {
                dettesParParticipation
                        .computeIfAbsent(mouvement.getParticipationId(), ignored -> new ArrayList<>())
                        .add(toOpenDebtLine(mouvement));
                continue;
            }

            if (estCreditCibleParticipation(mouvement)) {
                appliquerCreditCible(mouvement, dettesParParticipation);
            }
        }

        return dettesParParticipation;
    }

    private void appliquerCreditCible(MouvementSolde credit, Map<Long, List<OpenDebtLine>> dettesParParticipation) {
        List<OpenDebtLine> lines = dettesParParticipation.get(credit.getParticipationId());
        if (lines == null || lines.isEmpty()) {
            return;
        }

        BigDecimal restant = scale(credit.getMontant());
        for (OpenDebtLine line : lines) {
            if (!line.isOpen() || restant.signum() <= 0) {
                continue;
            }

            BigDecimal imputation = restant.min(line.getMontantRestant());
            line.imputer(imputation);
            restant = restant.subtract(imputation).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private boolean estDebitCibleParticipation(MouvementSolde mouvement) {
        return mouvement != null
                && mouvement.getType() == TypeMouvement.DEBIT
                && mouvement.getParticipationId() != null
                && mouvement.getOrigineType() != null
                && mouvement.getOrigineType() != OrigineMouvementSoldeType.LEGACY
                && TRACKED_DEBIT_ORIGINS.contains(mouvement.getOrigineType());
    }

    private boolean estCreditCibleParticipation(MouvementSolde mouvement) {
        return mouvement != null
                && mouvement.getType() == TypeMouvement.CREDIT
                && mouvement.getParticipationId() != null
                && mouvement.getOrigineType() != null
                && mouvement.getOrigineType() != OrigineMouvementSoldeType.LEGACY
                && TRACKED_CREDIT_ORIGINS.contains(mouvement.getOrigineType());
    }

    private OpenDebtLine toOpenDebtLine(MouvementSolde mouvement) {
        return new OpenDebtLine(
                mouvement.getId(),
                mouvement.getParticipationId(),
                mouvement.getMatchId(),
                mouvement.getOrigineType(),
                mouvement.getDateMouvement(),
                mouvement.getMontant(),
                false,
                mouvement.getDescription()
        );
    }

    private BigDecimal calculerTotalOuvert(Collection<List<OpenDebtLine>> dettesParParticipation) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (List<OpenDebtLine> lines : dettesParParticipation) {
            for (OpenDebtLine line : lines) {
                total = total.add(line.getMontantRestant()).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return total;
    }

    private BigDecimal scale(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }
}
