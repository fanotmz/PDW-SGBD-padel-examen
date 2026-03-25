package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    /**
     * Match PUBLIC : "premier payé = premier servi"
     * -> sous verrou pessimiste (findByIdForUpdateWithParticipations)
     * -> inscription + dette (15) + paiement immédiat
     * -> si le joueur a une dette existante, il paie (15 + dette)
     */
    public Participation rejoindreEtPayerMatchPublic(Long matchId, String joueurMatricule) {

        MatchPadel match = matchPadelRepository.findByIdForUpdateWithParticipations(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
        verifierMatchNonAnnule(match);

        if (match.getVisibilite() != MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match privé : seule l'organisation peut ajouter des joueurs.");
        }

        Joueur joueur = getJoueurOrThrow(joueurMatricule);

        verifierNonDejaInscrit(matchId, joueurMatricule);

        // place dispo vérifiée sous verrou
        if (match.getParticipations().size() >= 4) {
            throw new BusinessException("Match déjà complet");
        }

        // montant à payer = 15 + dette existante (solde = dette)
        BigDecimal dette = joueur.getSolde() == null ? BigDecimal.ZERO : joueur.getSolde().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montant = PART_JOUEUR.add(dette).setScale(2, RoundingMode.HALF_UP);

        Participation saved = participationRepository.save(new Participation(match, joueur));

        // inscription => dette de participation (15)
        soldeService.debiter(joueurMatricule, PART_JOUEUR);

        // paiement => (15 + dette) : rembourse dette + paie la part
        paiementService.payerParticipationAvecRattrapageDette(saved.getId(), montant);

        return saved;
    }

    /**
     * Calcule le montant attendu pour l'UI : 15 + dette actuelle.
     */
    public BigDecimal calculerMontantAttenduPourMatchPublic(Long matchId, String joueurMatricule) {

        MatchPadel match = matchPadelRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
        verifierMatchNonAnnule(match);

        if (match.getVisibilite() != MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match privé : ce calcul n'est valable que pour un match public.");
        }

        Joueur joueur = getJoueurOrThrow(joueurMatricule);

        BigDecimal dette = joueur.getSolde() == null ? BigDecimal.ZERO : joueur.getSolde().setScale(2, RoundingMode.HALF_UP);
        return PART_JOUEUR.add(dette).setScale(2, RoundingMode.HALF_UP);
    }

    public Participation ajouterJoueurParOrganisateur(Long matchId,
                                                      String organisateurMatricule,
                                                      String joueurMatriculeAAjouter) {
        MatchPadel match = getMatchOrThrow(matchId);
        verifierMatchNonAnnule(match);

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

    private MatchPadel getMatchOrThrow(Long matchId) {
        return matchPadelRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    private void verifierMatchNonAnnule(MatchPadel match) {
        if (match.getStatut() == MatchStatut.ANNULE) {
            throw new BusinessException("Match annulé : aucune nouvelle participation n'est possible.");
        }
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
