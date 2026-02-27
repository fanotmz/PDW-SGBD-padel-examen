package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.UpdateSiteHorairesRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.service.SiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SiteServiceTest {

    private SiteRepository siteRepo;
    private SiteService service;

    @BeforeEach
    void setup() {
        siteRepo = mock(SiteRepository.class);
        service = new SiteService(siteRepo);
    }

    // --------
    // lister
    // --------
    @Test
    void lister_ok() {
        when(siteRepo.findAll()).thenReturn(List.of(mock(Site.class), mock(Site.class)));

        List<Site> res = service.lister();

        assertEquals(2, res.size());
        verify(siteRepo).findAll();
    }

    // --------
    // getSite
    // --------
    @Test
    void getSite_idNull_refuse() {
        assertThrows(BusinessException.class, () -> service.getSite(null));
        verifyNoInteractions(siteRepo);
    }

    @Test
    void getSite_introuvable_notFound() {
        when(siteRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getSite(1L));
        verify(siteRepo).findById(1L);
    }

    @Test
    void getSite_ok() {
        Site site = mock(Site.class);
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));

        Site res = service.getSite(1L);

        assertSame(site, res);
        verify(siteRepo).findById(1L);
    }

    // --------
    // creerSite
    // --------
    @Test
    void creerSite_nomNullOuBlank_refuse() {
        assertThrows(BusinessException.class, () -> service.creerSite(null, "Bruxelles"));
        assertThrows(BusinessException.class, () -> service.creerSite("   ", "Bruxelles"));
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerSite_villeNullOuBlank_refuse() {
        assertThrows(BusinessException.class, () -> service.creerSite("Site A", null));
        assertThrows(BusinessException.class, () -> service.creerSite("Site A", "   "));
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerSite_nomDejaUtilise_refuse() {
        when(siteRepo.existsByNom("Site A")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.creerSite("Site A", "Bruxelles"));

        verify(siteRepo).existsByNom("Site A");
        verify(siteRepo, never()).save(any(Site.class));
    }

    @Test
    void creerSite_ok_save() {
        when(siteRepo.existsByNom("Site A")).thenReturn(false);
        when(siteRepo.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        Site res = service.creerSite("Site A", "Bruxelles");

        assertNotNull(res);
        assertEquals("Site A", res.getNom());
        assertEquals("Bruxelles", res.getVille());

        verify(siteRepo).existsByNom("Site A");
        verify(siteRepo).save(any(Site.class));
    }

    @Test
    void updateHoraires_ok_met_a_jour_ouverture_fermeture_et_jours() {
        // arrange
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));

        UpdateSiteHorairesRequest req = new UpdateSiteHorairesRequest();
        req.setHeureOuverture(LocalTime.of(8, 0));
        req.setHeureFermeture(LocalTime.of(22, 0));
        req.setJoursFermeture(Set.of(DayOfWeek.SUNDAY));

        // act
        Site updated = service.updateHoraires(1L, req);

        // assert
        assertSame(site, updated);
        assertEquals(LocalTime.of(8, 0), updated.getHeureOuverture());
        assertEquals(LocalTime.of(22, 0), updated.getHeureFermeture());
        assertTrue(updated.getJoursFermeture().contains(DayOfWeek.SUNDAY));

        verify(siteRepo).findById(1L);
        verifyNoMoreInteractions(siteRepo);
    }

    @Test
    void updateHoraires_refuse_si_ouverture_apres_ou_egale_fermeture() {
        // arrange
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));

        UpdateSiteHorairesRequest req = new UpdateSiteHorairesRequest();
        req.setHeureOuverture(LocalTime.of(22, 0));
        req.setHeureFermeture(LocalTime.of(22, 0)); // égal -> KO
        req.setJoursFermeture(Set.of(DayOfWeek.MONDAY));

        // act + assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateHoraires(1L, req));

        assertTrue(ex.getMessage().toLowerCase().contains("ouverture"));

        // le site ne doit pas être modifié
        assertNull(site.getHeureOuverture());
        assertNull(site.getHeureFermeture());
        assertTrue(site.getJoursFermeture() == null || site.getJoursFermeture().isEmpty());

        verify(siteRepo).findById(1L);
        verifyNoMoreInteractions(siteRepo);
    }

    // (optionnel mais utile)
    @Test
    void updateHoraires_site_introuvable_notFound() {
        when(siteRepo.findById(99L)).thenReturn(Optional.empty());

        UpdateSiteHorairesRequest req = new UpdateSiteHorairesRequest();
        req.setHeureOuverture(LocalTime.of(8, 0));
        req.setHeureFermeture(LocalTime.of(22, 0));

        assertThrows(NotFoundException.class, () -> service.updateHoraires(99L, req));
        verify(siteRepo).findById(99L);
        verifyNoMoreInteractions(siteRepo);
    }
}
