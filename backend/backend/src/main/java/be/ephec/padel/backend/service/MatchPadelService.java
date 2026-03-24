package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.mapper.MatchMapper;
import be.ephec.padel.backend.mapper.MatchDetailMapper;
import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.service.FermetureSiteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.mapper.PublicMatchSummaryMapper;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;

import java.time.LocalDate;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class MatchPadelService {

    // Valeurs MVP temporaires.
    // À remplacer par les paramètres de réservation par site/année quand l'issue dédiée sera implémentée.
    private static final long DUREE_MATCH_MIN = 90;
    private static final long BUFFER_MIN = 15;
    private static final long SLOT_MIN = DUREE_MATCH_MIN + BUFFER_MIN; // 105

    private final MatchPadelRepository matchPadelRepository;
    private final TerrainRepository terrainRepository;
    private final JoueurRepository joueurRepository;
    private final SoldeService soldeService;
    private final ParticipationRepository participationRepository;
    private final PaiementService paiementService;
    private final HoraireSiteService horaireSiteService;
    private final FermetureSiteService fermetureSiteService;
    private final PaiementRepository paiementRepository;
    private final FermetureGlobaleRepository fermetureGlobaleRepository;

    public MatchPadelService(MatchPadelRepository matchPadelRepository,
                             TerrainRepository terrainRepository,
                             JoueurRepository joueurRepository,
                             SoldeService soldeService,
                             ParticipationRepository participationRepository,
                             PaiementService paiementService, HoraireSiteService horaireSiteService, FermetureSiteService fermetureSiteService,
                             PaiementRepository paiementRepository,
                             FermetureGlobaleRepository fermetureGlobaleRepository) {
        this.matchPadelRepository = matchPadelRepository;
        this.terrainRepository = terrainRepository;
        this.joueurRepository = joueurRepository;
        this.soldeService = soldeService;
        this.participationRepository = participationRepository;
        this.paiementService = paiementService;
        this.horaireSiteService = horaireSiteService;
        this.paiementRepository = paiementRepository;
        this.fermetureGlobaleRepository = fermetureGlobaleRepository;
        this.fermetureSiteService = fermetureSiteService;
    }

    @Transactional(readOnly = true)
    public MatchPadel getMatch(Long id) {
        return matchPadelRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    @Transactional(readOnly = true)
    public MatchDto getMatchDto(Long id) {
        MatchPadel m = getMatch(id);

        BigDecimal montantTotal = Tarifs.PRIX_MATCH;

        BigDecimal montantPaye = paiementRepository.sumMontantByMatchId(id);
        if (montantPaye == null) {
            montantPaye = BigDecimal.ZERO;
        }

        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);
        if (resteAPayer.signum() < 0) {
            resteAPayer = BigDecimal.ZERO;
        }

        return MatchMapper.toDtoComplet(m, montantTotal, montantPaye, resteAPayer);
    }

    private void verifierFermetureGlobale(LocalDateTime dateDebut) {
        if (dateDebut == null) {
            throw new BusinessException("Date début obligatoire");
        }
        if (fermetureGlobaleRepository.existsByDate(dateDebut.toLocalDate())) {
            throw new BusinessException("Réservation impossible : fermeture globale (jour férié).");
        }
    }

    private void verifierOuvertureSite(Terrain terrain, LocalDateTime dateDebut) {
        if (terrain == null || terrain.getSite() == null) {
            throw new BusinessException("Terrain sans site associé.");
        }

        Site site = terrain.getSite();

        DayOfWeek jour = dateDebut.getDayOfWeek();
        if (site.getJoursFermeture().contains(jour)) {
            throw new BusinessException("Réservation impossible : site fermé ce jour-là.");
        }

        HoraireSite horaire = horaireSiteService.getApplicable(site.getId(), dateDebut);

        LocalTime ouverture = horaire.getHeureOuverture();
        LocalTime fermeture = horaire.getHeureFermeture();

        LocalTime start = dateDebut.toLocalTime();
        LocalTime end = dateDebut.plusMinutes(SLOT_MIN).toLocalTime();

        if (end.isBefore(start)) {
            throw new BusinessException("Réservation impossible : le créneau dépasse minuit.");
        }

        if (start.isBefore(ouverture) || end.isAfter(fermeture)) {
            throw new BusinessException("Réservation impossible : en dehors des horaires d'ouverture.");
        }
    }

    public MatchPadel creerMatch(Long terrainId,
                                 String organisateurMatricule,
                                 LocalDateTime dateDebut,
                                 MatchVisibilite visibilite) {

        if (terrainId == null) {
            throw new BusinessException("Terrain obligatoire");
        }
        if (organisateurMatricule == null || organisateurMatricule.isBlank()) {
            throw new BusinessException("Organisateur obligatoire");
        }
        if (dateDebut == null) {
            throw new BusinessException("Date début obligatoire");
        }
        if (visibilite == null) {
            throw new BusinessException("Visibilité obligatoire");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!dateDebut.isAfter(now)) {
            throw new BusinessException("La date du match doit être dans le futur.");
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));

        Joueur organisateur = joueurRepository.findById(organisateurMatricule)
                .orElseThrow(() -> new NotFoundException("Joueur introuvable"));

        if (organisateur.getSolde() != null && organisateur.getSolde().signum() > 0) {
            throw new BusinessException("Réservation impossible : dette en cours (" + organisateur.getSolde() + ").");
        }

        verifierDroitReservation(organisateur, terrain, dateDebut, now);
        verifierFermetureGlobale(dateDebut);
        verifierOuvertureSite(terrain, dateDebut);
        verifierFermetureSite(terrain, dateDebut);
        verifierTerrainDisponible(terrainId, dateDebut);

        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, visibilite);
        MatchPadel saved = matchPadelRepository.save(match);

        Participation pOrg = participationRepository.save(new Participation(saved, organisateur));

        BigDecimal part = Tarifs.PART_PAR_JOUEUR;
        soldeService.debiter(organisateurMatricule, part);
        paiementService.payerParticipation(pOrg.getId(), part);

        return saved;
    }

    private void verifierTerrainDisponible(Long terrainId, LocalDateTime newStart) {
        if (terrainId == null) {
            throw new BusinessException("Terrain obligatoire");
        }
        if (newStart == null) {
            throw new BusinessException("Date début obligatoire");
        }

        LocalDateTime from = newStart.minusMinutes(SLOT_MIN);
        LocalDateTime to = newStart.plusMinutes(SLOT_MIN);

        List<MatchPadel> candidats =
                matchPadelRepository.findByTerrainIdAndDateDebutBetween(terrainId, from, to);

        LocalDateTime newEndBuffer = newStart.plusMinutes(SLOT_MIN);

        for (MatchPadel existing : candidats) {
            LocalDateTime existingStart = existing.getDateDebut();
            if (existingStart == null) {
                continue;
            }

            LocalDateTime existingEndBuffer = existingStart.plusMinutes(SLOT_MIN);

            boolean overlap = existingStart.isBefore(newEndBuffer) && newStart.isBefore(existingEndBuffer);
            if (overlap) {
                throw new BusinessException(
                        "Terrain indisponible : un match est déjà prévu sur ce terrain (1h30 + 15 minutes de battement)."
                );
            }
        }
    }

    private void verifierDroitReservation(Joueur orga,
                                          Terrain terrain,
                                          LocalDateTime dateDebut,
                                          LocalDateTime now) {
        TypeJoueur type = orga.getType();
        if (type == null) {
            throw new BusinessException("Type joueur manquant.");
        }

        switch (type) {
            case GLOBAL -> {
                if (dateDebut.isAfter(now.plusWeeks(3))) {
                    throw new BusinessException("Un membre GLOBAL peut réserver au maximum 3 semaines à l'avance.");
                }
            }
            case SITE -> {
                if (dateDebut.isAfter(now.plusWeeks(2))) {
                    throw new BusinessException("Un membre SITE peut réserver au maximum 2 semaines à l'avance.");
                }
                if (orga.getSite() == null) {
                    throw new BusinessException("Joueur SITE sans site associé.");
                }
                if (terrain.getSite() == null) {
                    throw new BusinessException("Terrain sans site associé.");
                }

                Long siteJoueur = orga.getSite().getId();
                Long siteTerrain = terrain.getSite().getId();

                if (!siteJoueur.equals(siteTerrain)) {
                    throw new BusinessException("Un membre SITE ne peut réserver que sur son site.");
                }
            }
            case LIBRE -> {
                if (dateDebut.isAfter(now.plusDays(5))) {
                    throw new BusinessException("Un membre LIBRE peut réserver au maximum 5 jours à l'avance.");
                }
            }
            default -> throw new BusinessException("Type joueur inconnu.");
        }
    }
    private void verifierFermetureSite(Terrain terrain, LocalDateTime dateDebut) {
        if (terrain == null || terrain.getSite() == null || terrain.getSite().getId() == null) {
            throw new BusinessException("Terrain sans site associé.");
        }
        if (dateDebut == null) {
            throw new BusinessException("Date début obligatoire");
        }

        Long siteId = terrain.getSite().getId();
        LocalDate date = dateDebut.toLocalDate();

        if (fermetureSiteService.isDateFermeePourSite(siteId, date)) {
            throw new BusinessException("Réservation impossible : site fermé à cette date.");
        }
    }

    @Transactional(readOnly = true)
    public List<PublicMatchSummaryDto> getPublicMatchSummaries(LocalDate from,
                                                               LocalDate to,
                                                               Long siteId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Le paramètre 'from' doit être antérieur ou égal à 'to'.");
        }

        LocalDateTime fromDateTime = (from != null)
                ? from.atStartOfDay()
                : LocalDate.now().atStartOfDay();

        LocalDateTime toDateTime = (to != null)
                ? to.atTime(LocalTime.MAX)
                : null;

        List<PublicMatchSummaryProjection> rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                fromDateTime,
                toDateTime,
                siteId
        );

        return rows.stream()
                .map(row -> PublicMatchSummaryMapper.toDto(row, Tarifs.PART_PAR_JOUEUR))
                .collect(Collectors.toList());
    }
    private boolean peutVoirMatchPrive(MatchPadel match, String matricule) {
        if (matricule == null || matricule.isBlank()) {
            return false;
        }

        if (match.getOrganisateur() != null
                && matricule.equals(match.getOrganisateur().getMatricule())) {
            return true;
        }

        if (match.getParticipations() == null) {
            return false;
        }

        return match.getParticipations().stream()
                .map(Participation::getJoueur)
                .filter(j -> j != null && j.getMatricule() != null)
                .anyMatch(j -> matricule.equals(j.getMatricule()));
    }
    @Transactional(readOnly = true)
    public MatchDetailDto getMatchDetailDto(Long id, String matricule) {
        MatchPadel m = getMatch(id);

        if (m.getVisibilite() == MatchVisibilite.PRIVE && !peutVoirMatchPrive(m, matricule)) {
            throw new ForbiddenException("Accès refusé à ce match privé.");
        }

        BigDecimal montantTotal = Tarifs.PRIX_MATCH;

        BigDecimal montantPaye = paiementRepository.sumMontantByMatchId(id);
        if (montantPaye == null) {
            montantPaye = BigDecimal.ZERO;
        }

        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);
        if (resteAPayer.signum() < 0) {
            resteAPayer = BigDecimal.ZERO;
        }

        return MatchDetailMapper.toDto(m, montantTotal, montantPaye, resteAPayer);
    }
}