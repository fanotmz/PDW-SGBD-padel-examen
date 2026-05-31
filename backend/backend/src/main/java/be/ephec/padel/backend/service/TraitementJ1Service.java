package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
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
public class TraitementJ1Service {

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final MatchPadelRepository matchPadelRepository;
    private final PaiementRepository paiementRepository;
    private final SoldeService soldeService;
    private final PenaliteJoueurService penaliteJoueurService;
    private final Clock clock;

    public TraitementJ1Service(MatchPadelRepository matchPadelRepository,
                               PaiementRepository paiementRepository,
                               SoldeService soldeService,
                               PenaliteJoueurService penaliteJoueurService,
                               Clock clock) {
        this.matchPadelRepository = matchPadelRepository;
        this.paiementRepository = paiementRepository;
        this.soldeService = soldeService;
        this.penaliteJoueurService = penaliteJoueurService;
        this.clock = clock;
    }

    public int traiterJ1FenetreMinutes(int windowMinutes) {
        LocalDateTime now = LocalDateTime.now(clock);

        LocalDateTime from = now.plusHours(24);
        LocalDateTime to = from.plusMinutes(windowMinutes);

        List<MatchPadel> matchs = matchPadelRepository.findAtraiterJ1AvecDetails(from, to);
        int traites = 0;

        for (MatchPadel match : matchs) {
            if (match.getStatut() != MatchStatut.PLANIFIE) {
                continue;
            }
            appliquerReglesJ1(match, now);
            match.setJ1TraiteLe(now);
            matchPadelRepository.save(match);
            traites++;
        }

        return traites;
    }

    private void appliquerReglesJ1(MatchPadel match, LocalDateTime now) {
        List<Participation> participationsSnapshot = List.copyOf(match.getParticipations());

        for (Participation part : participationsSnapshot) {
            BigDecimal paye = paiementRepository
                    .sumMontantByParticipationId(part.getId())
                    .setScale(2, RoundingMode.HALF_UP);

            if (paye.compareTo(PART_JOUEUR) < 0) {
                match.setVisibilite(MatchVisibilite.PUBLIC);

                BigDecimal resteDu = PART_JOUEUR.subtract(paye).setScale(2, RoundingMode.HALF_UP);
                if (resteDu.signum() > 0) {
                    soldeService.crediter(
                            part.getJoueur().getMatricule(),
                            resteDu,
                            new SoldeOriginContext(
                                    OrigineMouvementSoldeType.TRAITEMENT_J1_NEUTRALISATION,
                                    part.getId(),
                                    match.getId(),
                                    null
                            )
                    );
                }

                match.removeParticipation(part);
            }
        }

        int nbParticipants = match.getParticipations().size();

        if (match.getVisibilite() == MatchVisibilite.PRIVE && nbParticipants < 4) {
            match.setVisibilite(MatchVisibilite.PUBLIC);
            penaliteJoueurService.appliquerPenaliteReservation(match.getOrganisateur());
        }
    }
}
