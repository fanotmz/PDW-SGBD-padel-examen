package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminSiteConsultationDto;
import be.ephec.padel.backend.dto.response.AdminSiteMatchSummaryDto;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.AdminMatchScope;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.AdminSiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSiteServiceTest {

    private SiteRepository siteRepository;
    private JoueurRepository joueurRepository;
    private TerrainRepository terrainRepository;
    private HoraireSiteRepository horaireSiteRepository;
    private MatchPadelRepository matchPadelRepository;
    private ServiceAutorisationAdmin serviceAutorisationAdmin;
    private Clock clock;
    private AdminSiteService service;

    @BeforeEach
    void setup() {
        siteRepository = mock(SiteRepository.class);
        joueurRepository = mock(JoueurRepository.class);
        terrainRepository = mock(TerrainRepository.class);
        horaireSiteRepository = mock(HoraireSiteRepository.class);
        matchPadelRepository = mock(MatchPadelRepository.class);
        serviceAutorisationAdmin = mock(ServiceAutorisationAdmin.class);
        clock = Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC"));

        service = new AdminSiteService(
                siteRepository,
                joueurRepository,
                terrainRepository,
                horaireSiteRepository,
                matchPadelRepository,
                serviceAutorisationAdmin,
                clock
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

    @Test
    void getMatchsBySite_filtre_par_defaut_matchs_futurs_planifies_et_mappe_dto() {
        Site site = site(1L, "Site 1", "Bruxelles", Set.of());
        Terrain terrain = terrain(10L, "Terrain A", site);
        Joueur organisateur = new Joueur("ORG001", "Organisateur", TypeJoueur.GLOBAL);
        MatchPadel match = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 15, 14, 30),
                MatchVisibilite.PUBLIC
        );
        ReflectionTestUtils.setField(match, "id", 100L);
        match.addParticipation(new Participation(match, organisateur));
        match.addParticipation(new Participation(match, new Joueur("JOU001", "Joueur", TypeJoueur.GLOBAL)));

        when(siteRepository.existsById(1L)).thenReturn(true);
        when(matchPadelRepository.findAdminSiteMatches(
                1L,
                MatchStatut.PLANIFIE,
                null,
                null,
                null,
                true,
                false,
                LocalDateTime.of(2026, 5, 14, 10, 0)
        )).thenReturn(List.of(match));
        when(serviceAutorisationAdmin.peutAdministrerSite(1L)).thenReturn(true);

        List<AdminSiteMatchSummaryDto> result = service.getMatchsBySite(1L, null, null, null, null, null);

        assertEquals(1, result.size());
        AdminSiteMatchSummaryDto dto = result.getFirst();
        assertEquals(100L, dto.getId());
        assertEquals(LocalDate.of(2026, 5, 15), dto.getDateDebut());
        assertEquals(LocalTime.of(14, 30), dto.getHeureDebut());
        assertEquals("Site 1", dto.getSiteNom());
        assertEquals("Terrain A", dto.getTerrainNom());
        assertEquals("ORG001", dto.getOrganisateurMatricule());
        assertEquals("Organisateur", dto.getOrganisateurNom());
        assertEquals(2, dto.getNbParticipants());
        assertEquals(2, dto.getPlacesRestantes());
        assertTrue(dto.isPeutAnnuler());
        assertFalse(dto.isPasse());
    }

    @Test
    void getMatchsBySite_history_retourne_matchs_passes_et_annules_futurs() {
        Site site = site(1L, "Site 1", "Bruxelles", Set.of());
        Terrain terrain = terrain(10L, "Terrain A", site);
        Joueur organisateur = new Joueur("ORG001", "Organisateur", TypeJoueur.GLOBAL);

        MatchPadel passe = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                MatchVisibilite.PUBLIC
        );
        ReflectionTestUtils.setField(passe, "id", 101L);

        MatchPadel annuleFutur = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 20, 9, 0),
                MatchVisibilite.PRIVE
        );
        ReflectionTestUtils.setField(annuleFutur, "id", 102L);
        annuleFutur.setStatut(MatchStatut.ANNULE);

        when(siteRepository.existsById(1L)).thenReturn(true);
        when(matchPadelRepository.findAdminSiteMatches(
                1L,
                null,
                null,
                null,
                null,
                false,
                true,
                LocalDateTime.of(2026, 5, 14, 10, 0)
        )).thenReturn(List.of(passe, annuleFutur));
        when(serviceAutorisationAdmin.peutAdministrerSite(1L)).thenReturn(true);

        List<AdminSiteMatchSummaryDto> result = service.getMatchsBySite(
                1L,
                null,
                null,
                null,
                null,
                AdminMatchScope.HISTORY
        );

        assertEquals(2, result.size());
        assertEquals(102L, result.get(0).getId());
        assertFalse(result.get(0).isPasse());
        assertEquals(MatchStatut.ANNULE, result.get(0).getStatut());
        assertEquals(101L, result.get(1).getId());
        assertTrue(result.get(1).isPasse());
        assertEquals(MatchStatut.PLANIFIE, result.get(1).getStatut());
    }

    @Test
    void getMatchsBySite_scopeAll_sans_filtres_retourne_tous_les_matchs_et_calcule_passe() {
        Site site = site(1L, "Site 1", "Bruxelles", Set.of());
        Terrain terrain = terrain(10L, "Terrain A", site);
        Joueur organisateur = new Joueur("ORG001", "Organisateur", TypeJoueur.GLOBAL);

        MatchPadel passe = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 3, 9, 0),
                MatchVisibilite.PUBLIC
        );
        ReflectionTestUtils.setField(passe, "id", 201L);

        MatchPadel futur = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 20, 9, 0),
                MatchVisibilite.PUBLIC
        );
        ReflectionTestUtils.setField(futur, "id", 202L);

        MatchPadel annule = new MatchPadel(
                terrain,
                organisateur,
                LocalDateTime.of(2026, 5, 10, 9, 0),
                MatchVisibilite.PRIVE
        );
        ReflectionTestUtils.setField(annule, "id", 203L);
        annule.setStatut(MatchStatut.ANNULE);

        when(siteRepository.existsById(1L)).thenReturn(true);
        when(matchPadelRepository.findAdminSiteMatches(
                1L,
                null,
                null,
                null,
                null,
                false,
                false,
                LocalDateTime.of(2026, 5, 14, 10, 0)
        )).thenReturn(List.of(passe, futur, annule));
        when(serviceAutorisationAdmin.peutAdministrerSite(1L)).thenReturn(true);

        List<AdminSiteMatchSummaryDto> result = service.getMatchsBySite(
                1L,
                null,
                null,
                null,
                null,
                AdminMatchScope.ALL
        );

        assertEquals(3, result.size());
        assertEquals(202L, result.get(0).getId());
        assertFalse(result.get(0).isPasse());
        assertEquals(203L, result.get(1).getId());
        assertTrue(result.get(1).isPasse());
        assertEquals(MatchStatut.ANNULE, result.get(1).getStatut());
        assertEquals(201L, result.get(2).getId());
        assertTrue(result.get(2).isPasse());
    }

    @Test
    void getMatchsBySite_transmet_filtres_periode_statut_visibilite_et_scope_all() {
        when(siteRepository.existsById(2L)).thenReturn(true);
        when(matchPadelRepository.findAdminSiteMatches(
                2L,
                MatchStatut.ANNULE,
                MatchVisibilite.PRIVE,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0),
                false,
                false,
                LocalDateTime.of(2026, 5, 14, 10, 0)
        )).thenReturn(List.of());

        List<AdminSiteMatchSummaryDto> result = service.getMatchsBySite(
                2L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                MatchStatut.ANNULE,
                MatchVisibilite.PRIVE,
                AdminMatchScope.ALL
        );

        assertTrue(result.isEmpty());
        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
    }

    @Test
    void getMatchsBySite_adminSite_hors_perimetre_est_refuse() {
        doThrow(new ForbiddenException("Accès interdit")).when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.getMatchsBySite(
                2L,
                null,
                null,
                null,
                null,
                AdminMatchScope.HISTORY
        ));
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
