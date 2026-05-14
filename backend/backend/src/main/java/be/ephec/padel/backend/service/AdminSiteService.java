package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.dto.response.HoraireSiteDto;
import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final JoueurRepository joueurRepository;
    private final TerrainRepository terrainRepository;
    private final HoraireSiteRepository horaireSiteRepository;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    public AdminSiteService(
            SiteRepository siteRepository,
            JoueurRepository joueurRepository,
            TerrainRepository terrainRepository,
            HoraireSiteRepository horaireSiteRepository,
            ServiceAutorisationAdmin serviceAutorisationAdmin
    ) {
        this.siteRepository = siteRepository;
        this.joueurRepository = joueurRepository;
        this.terrainRepository = terrainRepository;
        this.horaireSiteRepository = horaireSiteRepository;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
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
