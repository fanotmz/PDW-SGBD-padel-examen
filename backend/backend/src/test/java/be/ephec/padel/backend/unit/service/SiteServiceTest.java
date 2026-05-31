package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.service.SiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SiteServiceTest {

    private SiteRepository siteRepo;
    private HoraireSiteRepository horaireSiteRepo;
    private SiteService service;

    @BeforeEach
    void setup() {
        siteRepo = mock(SiteRepository.class);
        horaireSiteRepo = mock(HoraireSiteRepository.class);
        service = new SiteService(siteRepo, horaireSiteRepo);
    }

    @Test
    void lister_ok() {
        when(siteRepo.findAll()).thenReturn(List.of(mock(Site.class), mock(Site.class)));

        List<Site> res = service.lister();

        assertEquals(2, res.size());
        verify(siteRepo).findAll();
    }

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

    @Test
    void creerSite_nomNullOuBlank_refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerSite(null, "Bruxelles", 2026, LocalTime.of(9, 0), LocalTime.of(21, 0)));
        assertThrows(BusinessException.class,
                () -> service.creerSite("   ", "Bruxelles", 2026, LocalTime.of(9, 0), LocalTime.of(21, 0)));

        verifyNoInteractions(siteRepo, horaireSiteRepo);
    }

    @Test
    void creerSite_villeNullOuBlank_refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", null, 2026, LocalTime.of(9, 0), LocalTime.of(21, 0)));
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "   ", 2026, LocalTime.of(9, 0), LocalTime.of(21, 0)));

        verifyNoInteractions(siteRepo, horaireSiteRepo);
    }

    @Test
    void creerSite_anneeNull_refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", null, LocalTime.of(9, 0), LocalTime.of(21, 0)));

        verifyNoInteractions(siteRepo, horaireSiteRepo);
    }

    @Test
    void creerSite_horairesNull_refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", 2026, null, LocalTime.of(21, 0)));
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", 2026, LocalTime.of(9, 0), null));

        verifyNoInteractions(siteRepo, horaireSiteRepo);
    }

    @Test
    void creerSite_horairesInvalides_refuse() {
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", 2026, LocalTime.of(21, 0), LocalTime.of(21, 0)));
        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", 2026, LocalTime.of(22, 0), LocalTime.of(21, 0)));

        verifyNoInteractions(horaireSiteRepo);
    }

    @Test
    void creerSite_nomDejaUtilise_refuse() {
        when(siteRepo.existsByNom("Site A")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.creerSite("Site A", "Bruxelles", 2026, LocalTime.of(9, 0), LocalTime.of(21, 0)));

        verify(siteRepo).existsByNom("Site A");
        verify(siteRepo, never()).save(any(Site.class));
        verifyNoInteractions(horaireSiteRepo);
    }

    @Test
    void creerSite_ok_save_site_et_horaire() {
        when(siteRepo.existsByNom("Site A")).thenReturn(false);
        when(siteRepo.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));
        when(horaireSiteRepo.save(any(HoraireSite.class))).thenAnswer(inv -> inv.getArgument(0));

        Site res = service.creerSite(
                "Site A",
                "Bruxelles",
                2026,
                LocalTime.of(9, 0),
                LocalTime.of(21, 0)
        );

        assertNotNull(res);
        assertEquals("Site A", res.getNom());
        assertEquals("Bruxelles", res.getVille());

        verify(siteRepo).existsByNom("Site A");
        verify(siteRepo).save(any(Site.class));
        verify(horaireSiteRepo).save(any(HoraireSite.class));
    }
}