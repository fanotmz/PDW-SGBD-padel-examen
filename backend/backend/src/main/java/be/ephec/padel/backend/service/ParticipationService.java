package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class ParticipationService {

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final ParticipationRepository participationRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final JoueurRepository joueurRepository;
    private final SoldeService soldeService;
    private final PaiementService paiementService;

    public ParticipationService(ParticipationRepository participationRepository,
                                MatchPadelRepository matchPadelRepository,
                                JoueurRepository joueurRepository,
                                SoldeService soldeService,
                                PaiementService paiementService) {
        this.participationRepository = participationRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.joueurRepository = joueurRepository;
        this.soldeService = soldeService;
        this.paiementService = paiementService;
    }

    public Participation rejoindreEtPayerMatchPublic(Long matchId, String joueurMatricule, BigDecimal montant) {
        MatchPadel match = getMatchOrThrow(matchId);

        if (match.getVisibilite() != MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match privé : seule l'organisation peut ajouter des joueurs.");
        }

        // ✅ En match PUBLIC : paiement obligatoire et complet (= 15.00)
        BigDecimal m = validerMontantPublic(montant);

        Joueur joueur = getJoueurOrThrow(joueurMatricule);

        verifierNonDejaInscrit(matchId, joueurMatricule);
        verifierPlaceDisponible(matchId);

        Participation saved = participationRepository.save(new Participation(match, joueur));

        // dette puis paiement immédiat (validation au paiement)
        soldeService.debiter(joueurMatricule, PART_JOUEUR);
        paiementService.payerParticipation(saved.getId(), m);

        return saved;
    }

    public Participation ajouterJoueurParOrganisateur(Long matchId,
                                                      String organisateurMatricule,
                                                      String joueurMatriculeAAjouter) {
        MatchPadel match = getMatchOrThrow(matchId);

        if (match.getVisibilite() == MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match public : l'organisateur ne peut pas ajouter des joueurs.");
        }

        String orga = match.getOrganisateur().getMatricule();
        if (!orga.equals(organisateurMatricule)) {
            throw new BusinessException("Seul l'organisateur peut ajouter des joueurs à ce match.");
        }

        Joueur joueurAAjouter = getJoueurOrThrow(joueurMatriculeAAjouter);

        verifierNonDejaInscrit(matchId, joueurMatriculeAAjouter);
        verifierPlaceDisponible(matchId);

        Participation saved = participationRepository.save(new Participation(match, joueurAAjouter));

        // en privé : inscription => dette (paiement ultérieur via endpoint paiement)
        soldeService.debiter(joueurMatriculeAAjouter, PART_JOUEUR);

        return saved;
    }

    private BigDecimal validerMontantPublic(BigDecimal montant) {
        if (montant == null) throw new BusinessException("Montant obligatoire");
        if (montant.signum() <= 0) throw new BusinessException("Montant invalide");

        BigDecimal m = montant.setScale(2, BigDecimal.ROUND_HALF_UP);
        if (m.compareTo(PART_JOUEUR) != 0) {
            throw new BusinessException("Pour un match public, le paiement doit être de " + PART_JOUEUR);
        }
        return m;
    }

    private MatchPadel getMatchOrThrow(Long matchId) {
        return matchPadelRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    private Joueur getJoueurOrThrow(String matricule) {
        return joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));
    }

    private void verifierNonDejaInscrit(Long matchId, String joueurMatricule) {
        if (participationRepository.existsByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)) {
            throw new BusinessException("Joueur déjà inscrit à ce match");
        }
    }

    private void verifierPlaceDisponible(Long matchId) {
        int nb = participationRepository.countByMatch_Id(matchId);
        if (nb >= 4) {
            throw new BusinessException("Match déjà complet");
        }
    }
}