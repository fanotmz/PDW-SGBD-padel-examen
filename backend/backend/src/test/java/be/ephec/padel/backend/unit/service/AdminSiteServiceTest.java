package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.AdminSiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminSiteServiceTest {

    private SiteRepository siteRepository;
    private JoueurRepository joueurRepository;
    private TerrainRepository terrainRepository;
    private HoraireSiteRepository horaireSiteRepository;
    private ServiceAutorisationAdmin serviceAutorisationAdmin;
    private AdminSiteService service;

    @BeforeEach
    void setup() {
        siteRepository = mock(SiteRepository.class);
        joueurRepository = mock(JoueurRepository.class);
        terrainRepository = mock(TerrainRepository.class);
        horaireSiteRepository = mock(HoraireSiteRepository.class);
        serviceAutorisationAdmin = mock(ServiceAutorisationAdmin.class);

        service = new AdminSiteService(
                siteRepository,
                joueurRepository,
                terrainRepository,
                horaireSiteRepository,
                serviceAutorisationAdmin
        );
    }

    @Test
    void getSitesConsultables_adminGlobal_retourne_tous_les_sites_avec_terrains_et_horaires() {
        Site site1 = site(1L, "Site 1", "Bruxelles", Set.of(DayOfWeek.MONDAY));
        Site site2 = site(2L, "Site 2", "Liege", Set.of());

        when(serviceAutorisationAdmin.getSiteAdministreId()).thenReturn(null);
        when(siteRepository.findAll()).thenReturn(List.of(site1, site2));
        when(terrainRepository.findBySite_Id(1L)).thenReturn(List.of(terrain(10L, "Terrain A", site1)));
        when(terrainRepository.findBySite_Id(2L)).thenReturn(List.of(terrain(20L, "Terrain B", site2)));
        when(horaireSiteRepository.findBySiteIdOrderByAnneeAsc(1L))
                .thenReturn(List.of(horaire(100L, site1, 2026, 8, 22)));
        when(horaireSiteRepository.findBySiteIdOrderByAnneeAsc(2L))
                .thenReturn(List.of(horaire(200L, site2, 2027, 9, 21)));

        List<AdminSiteConsultationDto> result = service.getSitesConsultables();

        assertEquals(2, result.size());
        assertEquals("Site 1", result.get(0).getNom());
        assertTrue(result.get(0).getJoursFermeture().contains(DayOfWeek.MONDAY));
        assertEquals("Terrain A", result.get(0).getTerrains().get(0).getNom());
        assertEquals(2026, result.get(0).getHoraires().get(0).getAnnee());
        assertEquals("Site 2", result.get(1).getNom());
    }

    @Test
    void getSitesConsultables_adminSite_retourne_uniquement_son_site() {
        Site site1 = site(1L, "Site 1", "Bruxelles", Set.of(DayOfWeek.SUNDAY));

        when(serviceAutorisationAdmin.getSiteAdministreId()).thenReturn(1L);
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site1));
        when(terrainRepository.findBySite_Id(1L)).thenReturn(List.of(terrain(10L, "Terrain A", site1)));
        when(horaireSiteRepository.findBySiteIdOrderByAnneeAsc(1L))
                .thenReturn(List.of(horaire(100L, site1, 2026, 8, 22)));

        List<AdminSiteConsultationDto> result = service.getSitesConsultables();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Site 1", result.get(0).getNom());
        assertEquals("Terrain A", result.get(0).getTerrains().get(0).getNom());
        assertEquals(LocalTime.of(8, 0), result.get(0).getHoraires().get(0).getHeureOuverture());
    }

    private Site site(Long id, String nom, String ville, Set<DayOfWeek> joursFermeture) {
        Site site = new Site(nom, ville);
        ReflectionTestUtils.setField(site, "id", id);
        site.setJoursFermeture(joursFermeture);
        return site;
    }

    private Terrain terrain(Long id, String nom, Site site) {
        Terrain terrain = new Terrain(nom, site);
        ReflectionTestUtils.setField(terrain, "id", id);
        return terrain;
    }

    private HoraireSite horaire(Long id, Site site, Integer annee, int ouverture, int fermeture) {
        HoraireSite horaire = new HoraireSite(site, annee, LocalTime.of(ouverture, 0), LocalTime.of(fermeture, 0));
        horaire.setId(id);
        return horaire;
    }
}
