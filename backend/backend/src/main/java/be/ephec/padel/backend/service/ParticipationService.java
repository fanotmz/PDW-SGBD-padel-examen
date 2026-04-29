package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CurrentUserFacade currentUserFacade;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    @Autowired
    public ParticipationService(ParticipationRepository participationRepository,
                                MatchPadelRepository matchPadelRepository,
                                JoueurRepository joueurRepository,
                                SoldeService soldeService,
                                PaiementService paiementService,
                                CurrentUserFacade currentUserFacade,
                                ServiceAutorisationAdmin serviceAutorisationAdmin) {
        this.participationRepository = participationRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.joueurRepository = joueurRepository;
        this.soldeService = soldeService;
        this.paiementService = paiementService;
        this.currentUserFacade = currentUserFacade;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
    }

    public Participation rejoindreEtPayerMatchPublic(Long matchId) {
        Joueur joueur = currentUserFacade.getCurrentJoueur();
        MatchPadel match = matchPadelRepository.findByIdForUpdateWithParticipations(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
        verifierMatchNonAnnule(match);

        if (match.getVisibilite() != MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match priv\u00e9 : seule l'organisation peut ajouter des joueurs.");
        }

        String joueurMatricule = joueur.getMatricule();

        verifierNonDejaInscrit(matchId, joueurMatricule);

        if (match.getParticipations().size() >= 4) {
            throw new BusinessException("Match deja complet");
        }

        BigDecimal dette = joueur.getSolde() == null ? BigDecimal.ZERO : joueur.getSolde().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montant = PART_JOUEUR.add(dette).setScale(2, RoundingMode.HALF_UP);

        Participation saved = participationRepository.save(new Participation(match, joueur));

        soldeService.debiter(joueurMatricule, PART_JOUEUR);
        paiementService.payerParticipationAvecRattrapageDette(saved.getId(), montant);

        return saved;
    }

    public BigDecimal calculerMontantAttenduPourMatchPublic(Long matchId) {
        Joueur joueur = currentUserFacade.getCurrentJoueur();
        MatchPadel match = matchPadelRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
        verifierMatchNonAnnule(match);

        if (match.getVisibilite() != MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match priv\u00e9 : ce calcul n'est valable que pour un match public.");
        }

        BigDecimal dette = joueur.getSolde() == null ? BigDecimal.ZERO : joueur.getSolde().setScale(2, RoundingMode.HALF_UP);
        return PART_JOUEUR.add(dette).setScale(2, RoundingMode.HALF_UP);
    }

    public Participation ajouterJoueurParOrganisateur(Long matchId, String joueurMatriculeAAjouter) {
        MatchPadel match = getMatchOrThrow(matchId);
        verifierMatchNonAnnule(match);

        if (match.getVisibilite() == MatchVisibilite.PUBLIC) {
            throw new BusinessException("Match public : l'organisateur ne peut pas ajouter des joueurs.");
        }

        if (match.getStatut() != MatchStatut.PLANIFIE) {
            throw new BusinessException("Ajout impossible : seuls les matchs planifi\u00e9s acceptent de nouveaux joueurs.");
        }

        if (!peutAjouterJoueurPrive(match)) {
            throw new BusinessException(
                    "Seul l'organisateur ou un admin autoris\u00e9 sur le site peut ajouter des joueurs \u00e0 ce match."
            );
        }

        Joueur joueurAAjouter = getJoueurOrThrow(joueurMatriculeAAjouter);

        verifierNonDejaInscrit(matchId, joueurMatriculeAAjouter);
        verifierPlaceDisponible(matchId);

        Participation saved = participationRepository.save(new Participation(match, joueurAAjouter));
        soldeService.debiter(joueurMatriculeAAjouter, PART_JOUEUR);

        return saved;
    }

    private MatchPadel getMatchOrThrow(Long matchId) {
        return matchPadelRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    private void verifierMatchNonAnnule(MatchPadel match) {
        if (match.getStatut() == MatchStatut.ANNULE) {
            throw new BusinessException("Match annule : aucune nouvelle participation n'est possible.");
        }
    }

    private Joueur getJoueurOrThrow(String matricule) {
        return joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));
    }

    private void verifierNonDejaInscrit(Long matchId, String joueurMatricule) {
        if (participationRepository.existsByMatch_IdAndJoueur_Matricule(matchId, joueurMatricule)) {
            throw new BusinessException("Joueur d\u00e9j\u00e0 inscrit \u00e0 ce match");
        }
    }

    private void verifierPlaceDisponible(Long matchId) {
        int nb = participationRepository.countByMatch_Id(matchId);
        if (nb >= 4) {
            throw new BusinessException("Match d\u00e9j\u00e0 complet");
        }
    }

    private boolean peutAjouterJoueurPrive(MatchPadel match) {
        if (estOrganisateur(match)) {
            return true;
        }

        return serviceAutorisationAdmin.peutAdministrerSite(getSiteId(match));
    }

    private boolean estOrganisateur(MatchPadel match) {
        try {
            Joueur currentJoueur = currentUserFacade.getCurrentJoueur();
            Joueur organisateur = match.getOrganisateur();

            return currentJoueur != null
                    && organisateur != null
                    && currentJoueur.getMatricule() != null
                    && currentJoueur.getMatricule().equals(organisateur.getMatricule());
        } catch (ForbiddenException exception) {
            return false;
        }
    }

    private Long getSiteId(MatchPadel match) {
        if (match == null
                || match.getTerrain() == null
                || match.getTerrain().getSite() == null) {
            return null;
        }

        return match.getTerrain().getSite().getId();
    }
}
