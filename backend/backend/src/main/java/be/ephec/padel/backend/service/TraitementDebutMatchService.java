package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.model.entities.MatchPadel;
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

        for (MatchPadel match : matchs) {
            appliquerSoldeSiIncomplet(match);
            match.setSoldeTraiteLe(now);
            matchPadelRepository.save(match);
        }

        return matchs.size();
    }

    private void appliquerSoldeSiIncomplet(MatchPadel match) {
        // Si déjà complet => solde = 0 (normalement total payé = 60)
        // Si incomplet => solde = 60 - total payé, débité à l'organisateur (dette)
        BigDecimal totalPayeMatch = paiementRepository.sumMontantByMatchId(match.getId());
        if (totalPayeMatch == null) totalPayeMatch = BigDecimal.ZERO;

        totalPayeMatch = totalPayeMatch.setScale(2, RoundingMode.HALF_UP);

        BigDecimal solde = PRIX_MATCH.subtract(totalPayeMatch).setScale(2, RoundingMode.HALF_UP);

        if (solde.signum() > 0) {
            String matOrga = match.getOrganisateur().getMatricule();
            soldeService.debiter(matOrga, solde);
        }
    }
}