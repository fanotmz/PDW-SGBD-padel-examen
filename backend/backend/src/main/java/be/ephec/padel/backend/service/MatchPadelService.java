package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.response.CreneauxMatchResponseDto;
import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.mapper.MatchDetailMapper;
import be.ephec.padel.backend.mapper.MatchMapper;
import be.ephec.padel.backend.mapper.PublicMatchSummaryMapper;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class MatchPadelService {

    private static final long DUREE_MATCH_MIN = 90;
    private static final long BUFFER_MIN = 15;
    private static final long SLOT_MIN = DUREE_MATCH_MIN + BUFFER_MIN;
    private static final long PAS_CRENEAU_MIN = 15;
    private static final DateTimeFormatter CRENEAU_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MESSAGE_HORAIRE_MANQUANT =
            "Aucun horaire n'est configure pour ce site et cette annee.";
    private static final String MESSAGE_SITE_FERME =
            "Aucun creneau disponible : le site est ferme a cette date.";
    private static final String MESSAGE_AUCUN_CRENEAU =
            "Aucun creneau disponible pour ce terrain et cette date.";
    private static final String MESSAGE_RESERVATION_NON_AUTORISEE =
            "Aucun creneau disponible : votre situation ne permet pas de reserver.";

    private final MatchPadelRepository matchPadelRepository;
    private final TerrainRepository terrainRepository;
    private final SoldeService soldeService;
    private final ParticipationRepository participationRepository;
    private final PaiementService paiementService;
    private final HoraireSiteService horaireSiteService;
    private final FermetureSiteService fermetureSiteService;
    private final PaiementRepository paiementRepository;
    private final FermetureGlobaleRepository fermetureGlobaleRepository;
    private final Clock clock;
    private final CurrentUserFacade currentUserFacade;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    @Autowired
    public MatchPadelService(MatchPadelRepository matchPadelRepository,
                             TerrainRepository terrainRepository,
                             SoldeService soldeService,
                             ParticipationRepository participationRepository,
                             PaiementService paiementService,
                             HoraireSiteService horaireSiteService,
                             FermetureSiteService fermetureSiteService,
                             PaiementRepository paiementRepository,
                             FermetureGlobaleRepository fermetureGlobaleRepository,
                             Clock clock,
                             CurrentUserFacade currentUserFacade,
                             ServiceAutorisationAdmin serviceAutorisationAdmin) {
        this.matchPadelRepository = matchPadelRepository;
        this.terrainRepository = terrainRepository;
        this.soldeService = soldeService;
        this.participationRepository = participationRepository;
        this.paiementService = paiementService;
        this.horaireSiteService = horaireSiteService;
        this.fermetureSiteService = fermetureSiteService;
        this.paiementRepository = paiementRepository;
        this.fermetureGlobaleRepository = fermetureGlobaleRepository;
        this.clock = clock;
        this.currentUserFacade = currentUserFacade;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
    }

    @Transactional(readOnly = true)
    public MatchPadel getMatch(Long id) {
        return matchPadelRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Match introuvable"));
    }

    @Transactional(readOnly = true)
    public MatchDto getMatchDto(Long id) {
        MatchPadel match = getMatch(id);

        BigDecimal montantTotal = Tarifs.PRIX_MATCH;
        BigDecimal montantPaye = getMontantEncaisseParMatch(match);
        BigDecimal resteAPayer = calculerResteAPayer(match, montantTotal, montantPaye);

        return MatchMapper.toDtoComplet(match, montantTotal, montantPaye, resteAPayer);
    }

    private void verifierFermetureGlobale(LocalDateTime dateDebut) {
        if (dateDebut == null) {
            throw new BusinessException("Date de debut obligatoire");
        }
        if (fermetureGlobaleRepository.existsByDate(dateDebut.toLocalDate())) {
            throw new BusinessException("Reservation impossible : fermeture globale (jour ferie).");
        }
    }

    private void verifierOuvertureSite(Terrain terrain, LocalDateTime dateDebut) {
        if (terrain == null || terrain.getSite() == null) {
            throw new BusinessException("Terrain sans site associe.");
        }

        Site site = terrain.getSite();
        DayOfWeek jour = dateDebut.getDayOfWeek();
        if (estJourFermetureSite(site, jour)) {
            throw new BusinessException("Reservation impossible : site ferme ce jour-la.");
        }

        HoraireSite horaire = horaireSiteService.getApplicable(site.getId(), dateDebut);

        LocalTime ouverture = horaire.getHeureOuverture();
        LocalTime fermeture = horaire.getHeureFermeture();
        LocalTime start = dateDebut.toLocalTime();
        LocalTime end = dateDebut.plusMinutes(SLOT_MIN).toLocalTime();

        if (end.isBefore(start)) {
            throw new BusinessException("Reservation impossible : le creneau depasse minuit.");
        }

        if (start.isBefore(ouverture) || end.isAfter(fermeture)) {
            throw new BusinessException("Reservation impossible : en dehors des horaires d'ouverture.");
        }
    }

    public MatchPadel creerMatch(Long terrainId,
                                 LocalDateTime dateDebut,
                                 MatchVisibilite visibilite) {
        Joueur organisateur = currentUserFacade.getCurrentJoueur();
        if (organisateur == null) {
            throw new BusinessException("Organisateur obligatoire");
        }

        if (terrainId == null) {
            throw new BusinessException("Terrain obligatoire");
        }
        if (dateDebut == null) {
            throw new BusinessException("Date debut obligatoire");
        }
        if (visibilite == null) {
            throw new BusinessException("Visibilite obligatoire");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!dateDebut.isAfter(now)) {
            throw new BusinessException("La date du match doit etre dans le futur.");
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));

        if (aDette(organisateur)) {
            throw new BusinessException("Reservation impossible : dette en cours (" + organisateur.getSolde() + ").");
        }

        if (aPenaliteActive(organisateur, now)) {
            throw new BusinessException(
                    "Reservation impossible : penalite active jusqu'au "
                            + organisateur.getPenaliteJusqua().toLocalDate()
                            + " inclus."
            );
        }

        verifierDroitReservation(organisateur, terrain, dateDebut, now);
        verifierFermetureGlobale(dateDebut);
        verifierOuvertureSite(terrain, dateDebut);
        verifierFermetureSite(terrain, dateDebut);
        verifierTerrainDisponible(terrainId, dateDebut);

        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, visibilite);
        MatchPadel saved = matchPadelRepository.save(match);

        Participation participationOrganisateur = participationRepository.save(new Participation(saved, organisateur));

        BigDecimal part = Tarifs.PART_PAR_JOUEUR;
        soldeService.debiter(
                organisateur.getMatricule(),
                part,
                new SoldeOriginContext(
                        OrigineMouvementSoldeType.CREATION_MATCH_ORGANISATEUR,
                        participationOrganisateur.getId(),
                        saved.getId(),
                        null
                )
        );
        paiementService.payerParticipation(participationOrganisateur.getId(), part);

        return saved;
    }

    @Transactional(readOnly = true)
    public CreneauxMatchResponseDto getCreneauxDisponibles(Long terrainId, LocalDate date) {
        if (terrainId == null) {
            throw new BusinessException("Terrain obligatoire");
        }
        if (date == null) {
            throw new BusinessException("Date obligatoire");
        }

        Joueur joueur = currentUserFacade.getCurrentJoueur();
        if (joueur == null) {
            throw new BusinessException("Joueur obligatoire");
        }

        Terrain terrain = terrainRepository.findById(terrainId)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));

        if (terrain.getSite() == null || terrain.getSite().getId() == null) {
            throw new BusinessException("Terrain sans site associe.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (aDette(joueur) || aPenaliteActive(joueur, now)) {
            return new CreneauxMatchResponseDto(List.of(), MESSAGE_RESERVATION_NON_AUTORISEE);
        }

        Site site = terrain.getSite();
        Long siteId = site.getId();

        HoraireSite horaire;
        try {
            horaire = horaireSiteService.getApplicable(siteId, date.atStartOfDay());
        } catch (BusinessException exception) {
            return new CreneauxMatchResponseDto(List.of(), MESSAGE_HORAIRE_MANQUANT);
        }

        if (estJourFermetureSite(site, date.getDayOfWeek())
                || fermetureGlobaleRepository.existsByDate(date)
                || fermetureSiteService.isDateFermeePourSite(siteId, date)) {
            return new CreneauxMatchResponseDto(List.of(), MESSAGE_SITE_FERME);
        }

        LocalTime ouverture = horaire.getHeureOuverture();
        LocalTime fermeture = horaire.getHeureFermeture();
        LocalTime dernierDebut = fermeture.minusMinutes(SLOT_MIN);

        if (dernierDebut.isBefore(ouverture)) {
            return new CreneauxMatchResponseDto(List.of(), MESSAGE_AUCUN_CRENEAU);
        }

        LocalDateTime debutRecherche = date.atStartOfDay().minusMinutes(SLOT_MIN);
        LocalDateTime finRecherche = date.plusDays(1).atStartOfDay().plusMinutes(SLOT_MIN);
        List<MatchPadel> matchsProches = matchPadelRepository.findByTerrainIdAndDateDebutBetween(
                terrainId,
                debutRecherche,
                finRecherche
        );

        List<String> creneaux = new ArrayList<>();

        for (LocalTime heure = ouverture;
             !heure.isAfter(dernierDebut);
             heure = heure.plusMinutes(PAS_CRENEAU_MIN)) {

            LocalDateTime dateDebut = date.atTime(heure);

            if (!dateDebut.isAfter(now)) {
                continue;
            }

            if (!peutReserverSurCreneau(joueur, terrain, dateDebut, now)) {
                continue;
            }

            if (!estTerrainDisponible(matchsProches, dateDebut)) {
                continue;
            }

            creneaux.add(heure.format(CRENEAU_FORMATTER));
        }

        return new CreneauxMatchResponseDto(
                creneaux,
                creneaux.isEmpty() ? MESSAGE_AUCUN_CRENEAU : null
        );
    }

    private boolean aDette(Joueur joueur) {
        return joueur.getSolde() != null && joueur.getSolde().signum() > 0;
    }

    private boolean aPenaliteActive(Joueur joueur, LocalDateTime now) {
        return joueur.getPenaliteJusqua() != null && joueur.getPenaliteJusqua().isAfter(now);
    }

    private boolean peutReserverSurCreneau(Joueur joueur,
                                           Terrain terrain,
                                           LocalDateTime dateDebut,
                                           LocalDateTime now) {
        try {
            verifierDroitReservation(joueur, terrain, dateDebut, now);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private void verifierTerrainDisponible(Long terrainId, LocalDateTime newStart) {
        if (terrainId == null) {
            throw new BusinessException("Terrain obligatoire");
        }
        if (newStart == null) {
            throw new BusinessException("Date de debut obligatoire");
        }

        LocalDateTime from = newStart.minusMinutes(SLOT_MIN);
        LocalDateTime to = newStart.plusMinutes(SLOT_MIN);

        List<MatchPadel> candidats =
                matchPadelRepository.findByTerrainIdAndDateDebutBetween(terrainId, from, to);

        if (!estTerrainDisponible(candidats, newStart)) {
            throw new BusinessException(
                    "Terrain indisponible : un match est deja prevu sur ce terrain (1h30 + 15 minutes de battement)."
            );
        }
    }

    private boolean estTerrainDisponible(List<MatchPadel> candidats, LocalDateTime newStart) {
        LocalDateTime newEndBuffer = newStart.plusMinutes(SLOT_MIN);

        for (MatchPadel existing : candidats) {
            LocalDateTime existingStart = existing.getDateDebut();
            if (existingStart == null) {
                continue;
            }

            LocalDateTime existingEndBuffer = existingStart.plusMinutes(SLOT_MIN);
            boolean overlap = existingStart.isBefore(newEndBuffer) && newStart.isBefore(existingEndBuffer);

            if (overlap) {
                return false;
            }
        }

        return true;
    }

    private boolean estJourFermetureSite(Site site, DayOfWeek jour) {
        Set<DayOfWeek> joursFermeture = site.getJoursFermeture();
        return joursFermeture != null && joursFermeture.contains(jour);
    }

    private void verifierDroitReservation(Joueur organisateur,
                                          Terrain terrain,
                                          LocalDateTime dateDebut,
                                          LocalDateTime now) {
        TypeJoueur type = organisateur.getType();
        if (type == null) {
            throw new BusinessException("Type joueur manquant.");
        }

        switch (type) {
            case GLOBAL -> {
                if (dateDebut.isAfter(now.plusWeeks(3))) {
                    throw new BusinessException("Un membre GLOBAL peut reserver au maximum 3 semaines a l'avance.");
                }
            }
            case SITE -> {
                if (dateDebut.isAfter(now.plusWeeks(2))) {
                    throw new BusinessException("Un membre SITE peut reserver au maximum 2 semaines a l'avance.");
                }
                if (organisateur.getSite() == null) {
                    throw new BusinessException("Joueur SITE sans site associe.");
                }
                if (terrain.getSite() == null) {
                    throw new BusinessException("Terrain sans site associe.");
                }

                Long siteJoueur = organisateur.getSite().getId();
                Long siteTerrain = terrain.getSite().getId();
                if (!siteJoueur.equals(siteTerrain)) {
                    throw new BusinessException("Un membre SITE ne peut reserver que sur son site.");
                }
            }
            case LIBRE -> {
                if (dateDebut.isAfter(now.plusDays(5))) {
                    throw new BusinessException("Un membre LIBRE peut reserver au maximum 5 jours a l'avance.");
                }
            }
            default -> throw new BusinessException("Type joueur inconnu.");
        }
    }

    private void verifierFermetureSite(Terrain terrain, LocalDateTime dateDebut) {
        if (terrain == null || terrain.getSite() == null || terrain.getSite().getId() == null) {
            throw new BusinessException("Terrain sans site associe.");
        }
        if (dateDebut == null) {
            throw new BusinessException("Date de debut obligatoire");
        }

        Long siteId = terrain.getSite().getId();
        LocalDate date = dateDebut.toLocalDate();

        if (fermetureSiteService.isDateFermeePourSite(siteId, date)) {
            throw new BusinessException("RÃ©servation impossible : site fermÃ© Ã  cette date.");
        }
    }

    @Transactional(readOnly = true)
    public List<PublicMatchSummaryDto> getPublicMatchSummaries(LocalDate from,
                                                               LocalDate to,
                                                               Long siteId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Le parametre 'from' doit etre anterieur ou egal a 'to'.");
        }

        LocalDateTime fromDateTime = (from != null)
                ? from.atStartOfDay()
                : LocalDate.now(clock).atStartOfDay();

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
                .toList();
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
                .filter(joueur -> joueur != null && joueur.getMatricule() != null)
                .anyMatch(joueur -> matricule.equals(joueur.getMatricule()));
    }

    @Transactional(readOnly = true)
    public MatchDetailDto getMatchDetailDto(Long id) {
        MatchPadel match = getMatch(id);

        if (!currentUserFacade.isAdmin()) {
            String matricule = currentUserFacade.getCurrentJoueur().getMatricule();
            if (match.getVisibilite() == MatchVisibilite.PRIVE && !peutVoirMatchPrive(match, matricule)) {
                throw new ForbiddenException("Acc\u00e8s refus\u00e9 \u00e0 ce match priv\u00e9.");
            }
        }

        return buildMatchDetailDto(match);
    }

    private MatchDetailDto buildMatchDetailDto(MatchPadel match) {
        BigDecimal montantTotal = Tarifs.PRIX_MATCH;
        BigDecimal montantPaye = getMontantEncaisseParMatch(match);
        BigDecimal montantRembourse = getMontantRembourseParMatch(match);
        BigDecimal resteAPayer = calculerResteAPayer(match, montantTotal, montantPaye);
        boolean peutAjouterJoueurPrive = peutAjouterJoueurPrive(match);

        return MatchDetailMapper.toDto(
                match,
                peutAjouterJoueurPrive,
                montantTotal,
                montantPaye,
                resteAPayer,
                montantRembourse
        );
    }

    private boolean peutAjouterJoueurPrive(MatchPadel match) {
        if (match.getVisibilite() != MatchVisibilite.PRIVE) {
            return false;
        }

        if (match.getStatut() != MatchStatut.PLANIFIE) {
            return false;
        }

        if (match.getParticipations() != null && match.getParticipations().size() >= 4) {
            return false;
        }

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

    private BigDecimal getMontantEncaisseParMatch(MatchPadel match) {
        BigDecimal montantPaye = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (match.getParticipations() == null) {
            return montantPaye;
        }

        for (Participation participation : match.getParticipations()) {
            montantPaye = montantPaye.add(getMontantEncaisseAffecteParticipation(participation));
        }

        return montantPaye;
    }

    private BigDecimal getMontantRembourseParMatch(MatchPadel match) {
        BigDecimal totalRembourse = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (match.getParticipations() == null) {
            return totalRembourse;
        }

        for (Participation participation : match.getParticipations()) {
            totalRembourse = totalRembourse.add(getMontantRembourseAffecteParticipation(participation));
        }

        return totalRembourse;
    }

    private BigDecimal getMontantEncaisseAffecteParticipation(Participation participation) {
        if (participation == null || participation.getId() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal encaisseParticipation = nullSafeAmount(
                paiementRepository.sumMontantByParticipationIdAndType(participation.getId(), TypePaiement.ENCAISSEMENT)
        );

        return encaisseParticipation.min(Tarifs.PART_PAR_JOUEUR).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getMontantRembourseAffecteParticipation(Participation participation) {
        if (participation == null || participation.getId() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal encaisseAffecteParticipation = getMontantEncaisseAffecteParticipation(participation);
        BigDecimal rembourseParticipation = nullSafeAmount(
                paiementRepository.sumMontantByParticipationIdAndType(participation.getId(), TypePaiement.REMBOURSEMENT)
        ).abs();

        return rembourseParticipation.min(encaisseAffecteParticipation).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafeAmount(BigDecimal montant) {
        if (montant == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerResteAPayer(MatchPadel match, BigDecimal montantTotal, BigDecimal montantPaye) {
        if (match.getStatut() == MatchStatut.ANNULE) {
            return BigDecimal.ZERO;
        }

        BigDecimal resteAPayer = montantTotal.subtract(montantPaye);
        if (resteAPayer.signum() < 0) {
            resteAPayer = BigDecimal.ZERO;
        }
        return resteAPayer;
    }
}
