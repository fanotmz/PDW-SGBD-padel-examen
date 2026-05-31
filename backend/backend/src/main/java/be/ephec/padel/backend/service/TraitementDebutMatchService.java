package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
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
public class TraitementDebutMatchService {

    private static final BigDecimal PRIX_MATCH = Tarifs.PRIX_MATCH;
    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final MatchPadelRepository matchPadelRepository;
    private final PaiementRepository paiementRepository;
    private final SoldeService soldeService;
    private final Clock clock;

    public TraitementDebutMatchService(MatchPadelRepository matchPadelRepository,
                                       PaiementRepository paiementRepository,
                                       SoldeService soldeService,
                                       Clock clock) {
        this.matchPadelRepository = matchPadelRepository;
        this.paiementRepository = paiementRepository;
        this.soldeService = soldeService;
        this.clock = clock;
    }

    public int traiterDebutMatchFenetreMinutes(int windowMinutes) {
        LocalDateTime now = LocalDateTime.now(clock);

        LocalDateTime from = now.minusMinutes(windowMinutes);
        LocalDateTime to = now.plusSeconds(1);

        List<MatchPadel> matchs = matchPadelRepository.findAtraiterDebutMatchAvecDetails(from, to);
        int traites = 0;

        for (MatchPadel match : matchs) {
            if (match.getStatut() != MatchStatut.PLANIFIE) {
                continue;
            }
            appliquerSoldeSiIncomplet(match);
            match.setSoldeTraiteLe(now);
            matchPadelRepository.save(match);
            traites++;
        }

        return traites;
    }

    private void appliquerSoldeSiIncomplet(MatchPadel match) {
        BigDecimal totalPayeMatch = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Participation participation : match.getParticipations()) {
            if (participation == null || participation.getId() == null) {
                continue;
            }

            BigDecimal encaisseParticipation = paiementRepository.sumMontantByParticipationIdAndType(
                    participation.getId(),
                    TypePaiement.ENCAISSEMENT
            );
            if (encaisseParticipation == null) {
                encaisseParticipation = BigDecimal.ZERO;
            }

            totalPayeMatch = totalPayeMatch.add(
                    encaisseParticipation
                            .setScale(2, RoundingMode.HALF_UP)
                            .min(PART_JOUEUR)
            );
        }

        BigDecimal solde = PRIX_MATCH.subtract(totalPayeMatch).setScale(2, RoundingMode.HALF_UP);

        if (solde.signum() > 0) {
            String matOrga = match.getOrganisateur().getMatricule();
            soldeService.debiter(
                    matOrga,
                    solde,
                    new SoldeOriginContext(
                            OrigineMouvementSoldeType.TRANSFERT_ORGANISATEUR_DEBUT_MATCH,
                            null,
                            match.getId(),
                            null
                    )
            );
        }
    }
}
