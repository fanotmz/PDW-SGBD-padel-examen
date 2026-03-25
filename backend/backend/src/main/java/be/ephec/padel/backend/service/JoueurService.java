package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class JoueurService {

    private static final int CAPACITE_MATCH = 4;

    private final JoueurRepository joueurRepository;
    private final SiteRepository siteRepository;
    private final ParticipationRepository participationRepository;
    private final MatchPadelRepository matchPadelRepository;

    public JoueurService(JoueurRepository joueurRepository,
                         SiteRepository siteRepository,
                         ParticipationRepository participationRepository,
                         MatchPadelRepository matchPadelRepository) {
        this.joueurRepository = joueurRepository;
        this.siteRepository = siteRepository;
        this.participationRepository = participationRepository;
        this.matchPadelRepository = matchPadelRepository;
    }

    public Joueur creerJoueur(String matricule, String nom, TypeJoueur type, Long siteId) {
        if (matricule == null || matricule.isBlank()) throw new BusinessException("Matricule obligatoire");
        if (nom == null || nom.isBlank()) throw new BusinessException("Nom obligatoire");
        if (type == null) throw new BusinessException("Type joueur obligatoire");

        verifierMatricule(type, matricule);

        if (joueurRepository.existsById(matricule)) {
            throw new BusinessException("Matricule déjà utilisé");
        }

        Site site = null;

        if (type == TypeJoueur.SITE) {
            if (siteId == null) throw new BusinessException("Un joueur SITE doit être lié à un site");
            site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new NotFoundException("Site introuvable"));
        } else {
            if (siteId != null) {
                throw new BusinessException("Seul un joueur SITE peut avoir un site.");
            }
        }

        Joueur joueur = new Joueur(matricule, nom, type, site);
        joueur.setSolde(BigDecimal.ZERO);
        return joueurRepository.save(joueur);
    }

    public Joueur getJoueur(String matricule) {
        return joueurRepository.findById(matricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));
    }

    public List<Joueur> lister() {
        return joueurRepository.findAll();
    }

    public boolean aDette(String matricule) {
        Joueur joueur = getJoueur(matricule);
        return joueur.getSolde() != null && joueur.getSolde().compareTo(BigDecimal.ZERO) > 0;
    }

    public void verifierPasDeDette(String matricule) {
        if (aDette(matricule)) {
            throw new BusinessException("Action impossible : solde dû (dette) non réglé.");
        }
    }

    private void verifierMatricule(TypeJoueur type, String matricule) {
        String pattern;
        switch (type) {
            case GLOBAL -> pattern = "^G\\d{4}$";
            case SITE -> pattern = "^S\\d{4}$";
            case LIBRE -> pattern = "^L\\d{4}$";
            default -> throw new BusinessException("Type joueur inconnu");
        }

        if (!matricule.matches(pattern)) {
            throw new BusinessException("Matricule invalide pour le type " + type + " : " + matricule);
        }
    }

    private PlayerMatchSummaryDto toPlayerMatchSummaryDto(Participation participation,
                                                          String matricule,
                                                          LocalDate today) {
        MatchPadel match = participation.getMatch();
        LocalDate dateMatch = match.getDateDebut().toLocalDate();

        PlayerMatchRoleDto roleJoueur =
                match.getOrganisateur().getMatricule().equals(matricule)
                        ? PlayerMatchRoleDto.ORGANISATEUR
                        : PlayerMatchRoleDto.PARTICIPANT;

        MatchTemporalStatusDto statutTemporel;
        Integer joursAvantMatch = null;

        if (dateMatch.isBefore(today)) {
            statutTemporel = MatchTemporalStatusDto.PASSE;
        } else if (dateMatch.isEqual(today)) {
            statutTemporel = MatchTemporalStatusDto.AUJOURD_HUI;
        } else {
            statutTemporel = MatchTemporalStatusDto.FUTUR;
            joursAvantMatch = (int) ChronoUnit.DAYS.between(today, dateMatch);
        }

        boolean paiementJoueurEffectue = !participation.getPaiements().isEmpty();

        return new PlayerMatchSummaryDto(
                match.getId(),
                match.getDateDebut(),
                match.getTerrain().getSite().getId(),
                match.getTerrain().getSite().getNom(),
                match.getTerrain().getId(),
                match.getTerrain().getNom(),
                match.getVisibilite(),
                match.getStatut(),
                roleJoueur,
                statutTemporel,
                joursAvantMatch,
                paiementJoueurEffectue
        );
    }

    public List<PlayerMatchSummaryDto> getPlayerMatches(String matricule) {
        getJoueur(matricule);

        List<Participation> participations =
                participationRepository.findByJoueur_MatriculeOrderByMatch_DateDebutAsc(matricule);

        LocalDate today = LocalDate.now();

        return participations.stream()
                .map(participation -> toPlayerMatchSummaryDto(participation, matricule, today))
                .toList();
    }

    private OrganizerMatchSummaryDto toOrganizerMatchSummaryDto(MatchPadel match, LocalDate today) {
        LocalDate dateMatch = match.getDateDebut().toLocalDate();

        MatchTemporalStatusDto statutTemporel;
        Integer joursAvantMatch = null;

        if (dateMatch.isBefore(today)) {
            statutTemporel = MatchTemporalStatusDto.PASSE;
        } else if (dateMatch.isEqual(today)) {
            statutTemporel = MatchTemporalStatusDto.AUJOURD_HUI;
        } else {
            statutTemporel = MatchTemporalStatusDto.FUTUR;
            joursAvantMatch = (int) ChronoUnit.DAYS.between(today, dateMatch);
        }

        int nbParticipants = match.getParticipations().size();
        int placesRestantes = Math.max(0, CAPACITE_MATCH - nbParticipants);
        boolean complet = nbParticipants >= CAPACITE_MATCH;

        boolean risquePenaliteJ1 =
                match.getStatut() != MatchStatut.ANNULE
                        && match.getVisibilite() == MatchVisibilite.PRIVE
                        && !complet
                        && statutTemporel == MatchTemporalStatusDto.FUTUR
                        && joursAvantMatch != null
                        && joursAvantMatch <= 1;

        return new OrganizerMatchSummaryDto(
                match.getId(),
                match.getDateDebut(),
                match.getTerrain().getSite().getId(),
                match.getTerrain().getSite().getNom(),
                match.getTerrain().getId(),
                match.getTerrain().getNom(),
                match.getVisibilite(),
                match.getStatut(),
                nbParticipants,
                placesRestantes,
                complet,
                statutTemporel,
                joursAvantMatch,
                risquePenaliteJ1
        );
    }
    public List<OrganizerMatchSummaryDto> getOrganizedMatches(String matricule) {
        getJoueur(matricule);

        List<MatchPadel> matchs =
                matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule(matricule);

        LocalDate today = LocalDate.now();

        return matchs.stream()
                .map(match -> toOrganizerMatchSummaryDto(match, today))
                .toList();
    }
}
