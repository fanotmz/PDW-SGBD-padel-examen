package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.dto.response.AdminSiteMatchSummaryDto;
import be.ephec.padel.backend.dto.response.HoraireSiteDto;
import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.AdminMatchScope;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final JoueurRepository joueurRepository;
    private final TerrainRepository terrainRepository;
    private final HoraireSiteRepository horaireSiteRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;
    private final Clock clock;

    public AdminSiteService(
            SiteRepository siteRepository,
            JoueurRepository joueurRepository,
            TerrainRepository terrainRepository,
            HoraireSiteRepository horaireSiteRepository,
            MatchPadelRepository matchPadelRepository,
            ServiceAutorisationAdmin serviceAutorisationAdmin,
            Clock clock
    ) {
        this.siteRepository = siteRepository;
        this.joueurRepository = joueurRepository;
        this.terrainRepository = terrainRepository;
        this.horaireSiteRepository = horaireSiteRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
        this.clock = clock;
    }

    public List<AdminSiteConsultationDto> getSitesConsultables() {
        Long siteAdministreId = serviceAutorisationAdmin.getSiteAdministreId();

        List<Site> sites = siteAdministreId == null
                ? siteRepository.findAll()
                : List.of(siteRepository.findById(siteAdministreId)
                        .orElseThrow(() -> new NotFoundException("Site admin introuvable: " + siteAdministreId)));

        return sites.stream()
                .map(this::toAdminSiteConsultationDto)
                .toList();
    }

    public List<JoueurAdminDto> getJoueursBySite(Long siteId) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        if (!siteRepository.existsById(siteId)) {
            throw new NotFoundException("Site introuvable: " + siteId);
        }

        List<Joueur> joueurs = joueurRepository.findBySite_Id(siteId);

        return joueurs.stream()
                .map(this::toJoueurAdminDto)
                .toList();
    }

    public List<AdminSiteMatchSummaryDto> getMatchsBySite(Long siteId,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          MatchStatut statut,
                                                          MatchVisibilite visibilite,
                                                          AdminMatchScope scope) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        if (!siteRepository.existsById(siteId)) {
            throw new NotFoundException("Site introuvable: " + siteId);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AdminMatchScope effectiveScope = scope == null ? AdminMatchScope.UPCOMING_PLANNED : scope;
        boolean upcomingPlannedOnly = effectiveScope == AdminMatchScope.UPCOMING_PLANNED;
        boolean historyOnly = effectiveScope == AdminMatchScope.HISTORY;
        MatchStatut effectiveStatut = upcomingPlannedOnly ? MatchStatut.PLANIFIE : statut;
        LocalDateTime fromDateTime = toStartOfDay(from);
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();

        return matchPadelRepository.findAdminSiteMatches(
                        siteId,
                        effectiveStatut,
                        visibilite,
                        fromDateTime,
                        toDateTime,
                        upcomingPlannedOnly,
                        historyOnly,
                        now
                )
                .stream()
                .sorted(getAdminMatchComparator(effectiveScope))
                .map((match) -> toAdminSiteMatchSummaryDto(match, now))
                .toList();
    }

    private Comparator<MatchPadel> getAdminMatchComparator(AdminMatchScope scope) {
        Comparator<MatchPadel> comparator = Comparator.comparing(MatchPadel::getDateDebut);
        if (scope == AdminMatchScope.UPCOMING_PLANNED) {
            return comparator;
        }
        return comparator.reversed();
    }

    private JoueurAdminDto toJoueurAdminDto(Joueur joueur) {
        return new JoueurAdminDto(
                joueur.getMatricule(),
                joueur.getNom(),
                joueur.getType(),
                joueur.getSolde(),
                joueur.getPenaliteJusqua()
        );
    }

    private AdminSiteConsultationDto toAdminSiteConsultationDto(Site site) {
        Long siteId = site.getId();
        List<TerrainDto> terrains = terrainRepository.findBySite_Id(siteId).stream()
                .map(this::toTerrainDto)
                .toList();
        List<HoraireSiteDto> horaires = horaireSiteRepository.findBySiteIdOrderByAnneeAsc(siteId).stream()
                .map(this::toHoraireSiteDto)
                .toList();

        return new AdminSiteConsultationDto(
                site.getId(),
                site.getNom(),
                site.getVille(),
                site.getJoursFermeture(),
                terrains,
                horaires
        );
    }

    private AdminSiteMatchSummaryDto toAdminSiteMatchSummaryDto(MatchPadel match, LocalDateTime now) {
        int nbParticipants = match.getParticipations() == null ? 0 : match.getParticipations().size();
        int placesRestantes = Math.max(0, 4 - nbParticipants);
        boolean peutAnnuler = match.getStatut() == MatchStatut.PLANIFIE
                && match.getDateDebut().isAfter(now)
                && serviceAutorisationAdmin.peutAdministrerSite(match.getTerrain().getSite().getId());
        boolean passe = match.getDateDebut().isBefore(now);

        return new AdminSiteMatchSummaryDto(
                match.getId(),
                match.getDateDebut().toLocalDate(),
                match.getDateDebut().toLocalTime(),
                match.getTerrain().getSite().getId(),
                match.getTerrain().getSite().getNom(),
                match.getTerrain().getId(),
                match.getTerrain().getNom(),
                match.getOrganisateur().getMatricule(),
                match.getOrganisateur().getNom(),
                match.getVisibilite(),
                match.getStatut(),
                nbParticipants,
                placesRestantes,
                peutAnnuler,
                passe
        );
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private TerrainDto toTerrainDto(Terrain terrain) {
        return new TerrainDto(
                terrain.getId(),
                terrain.getNom(),
                terrain.getSite().getId()
        );
    }

    private HoraireSiteDto toHoraireSiteDto(HoraireSite horaire) {
        return new HoraireSiteDto(
                horaire.getId(),
                horaire.getSite().getId(),
                horaire.getAnnee(),
                horaire.getHeureOuverture(),
                horaire.getHeureFermeture()
        );
    }
}
