package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class MeStatsService {

    private final CurrentUserFacade currentUserFacade;
    private final ParticipationRepository participationRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final PaiementRepository paiementRepository;
    private final Clock clock;

    public MeStatsService(CurrentUserFacade currentUserFacade,
                          ParticipationRepository participationRepository,
                          MatchPadelRepository matchPadelRepository,
                          PaiementRepository paiementRepository,
                          Clock clock) {
        this.currentUserFacade = currentUserFacade;
        this.participationRepository = participationRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.paiementRepository = paiementRepository;
        this.clock = clock;
    }

    public MeStatsDto getCurrentUserStats() {
        Joueur joueur = currentUserFacade.getCurrentJoueur();
        String matricule = joueur.getMatricule();
        LocalDateTime now = LocalDateTime.now(clock);

        long nbMatchsParticipes = participationRepository.countByJoueur_Matricule(matricule);
        long nbMatchsOrganises = matchPadelRepository.countByOrganisateur_Matricule(matricule);
        long nbMatchsPasses = matchPadelRepository.countDistinctLinkedPastMatchesByMatricule(matricule, now);
        long nbMatchsFuturs = matchPadelRepository.countDistinctLinkedFutureMatchesByMatricule(matricule, now);
        long nbMatchsAnnules = matchPadelRepository.countDistinctLinkedCancelledMatchesByMatricule(matricule);
        BigDecimal montantTotalPaye = paiementRepository.sumMontantByParticipationJoueurMatriculeAndType(
                matricule,
                TypePaiement.ENCAISSEMENT
        );
        BigDecimal detteActuelle = joueur.getSolde() == null ? BigDecimal.ZERO : joueur.getSolde();

        return new MeStatsDto(
                nbMatchsParticipes,
                nbMatchsOrganises,
                nbMatchsPasses,
                nbMatchsFuturs,
                nbMatchsAnnules,
                montantTotalPaye,
                detteActuelle
        );
    }
}
