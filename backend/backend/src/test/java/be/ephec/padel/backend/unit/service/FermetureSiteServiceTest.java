package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.CreateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.CreateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.FermetureSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.FermetureSiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FermetureSiteServiceTest {

    private FermetureSiteRepository fermetureSiteRepository;
    private SiteRepository siteRepository;
    private ServiceAutorisationAdmin serviceAutorisationAdmin;
    private FermetureSiteService service;

    @BeforeEach
    void setup() {
        fermetureSiteRepository = mock(FermetureSiteRepository.class);
        siteRepository = mock(SiteRepository.class);
        serviceAutorisationAdmin = mock(ServiceAutorisationAdmin.class);

        service = new FermetureSiteService(
                fermetureSiteRepository,
                siteRepository,
                serviceAutorisationAdmin
        );

        doNothing().when(serviceAutorisationAdmin).verifierAccesAuSite(anyLong());
    }

    // --------
    // listerParSite
    // --------

    @Test
    void listerParSite_idNull_refuse() {
        assertThrows(BusinessException.class, () -> service.listerParSite(null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(null);
        verifyNoInteractions(fermetureSiteRepository);
        verifyNoMoreInteractions(siteRepository);
    }

    @Test
    void listerParSite_refuse_si_admin_hors_perimetre() {
        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.listerParSite(2L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void listerParSite_siteIntrouvable_notFound() {
        when(siteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.listerParSite(1L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void listerParSite_ok() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite f1 = new FermetureSite();
        f1.setSite(site);
        f1.setDate(LocalDate.of(2030, 1, 10));

        FermetureSite f2 = new FermetureSite();
        f2.setSite(site);
        f2.setDateDebut(LocalDate.of(2030, 2, 1));
        f2.setDateFin(LocalDate.of(2030, 2, 3));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findBySiteIdOrderByDateAscDateDebutAsc(1L))
                .thenReturn(List.of(f1, f2));

        List<FermetureSite> res = service.listerParSite(1L);

        assertEquals(2, res.size());
        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findBySiteIdOrderByDateAscDateDebutAsc(1L);
    }

    // --------
    // getById
    // --------

    @Test
    void getById_refuse_si_admin_hors_perimetre() {
        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.getById(2L, 10L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void getById_refuse_si_fermetureId_null() {
        assertThrows(BusinessException.class, () -> service.getById(1L, null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void getById_siteIntrouvable_notFound() {
        when(siteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L, 10L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void getById_fermetureIntrouvable_notFound() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(1L, 10L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
    }

    @Test
    void getById_ok() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(LocalDate.of(2030, 1, 15));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        FermetureSite res = service.getById(1L, 10L);

        assertSame(fermeture, res);
        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
    }

    // --------
    // creerDate
    // --------

    @Test
    void creerDate_siteIdNull_refuse() {
        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 15));

        assertThrows(BusinessException.class, () -> service.creerDate(null, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(null);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void creerDate_refuse_si_admin_hors_perimetre() {
        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 15));

        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.creerDate(2L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void creerDate_siteIntrouvable_notFound() {
        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 15));

        when(siteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.creerDate(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerDate_requestNull_refuse() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        assertThrows(BusinessException.class, () -> service.creerDate(1L, null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerDate_refuse_si_date_absente() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();

        assertThrows(BusinessException.class, () -> service.creerDate(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerDate_refuse_si_date_deja_existante() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 15)))
                .thenReturn(true);

        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 15));
        req.setMotif("Jour férié");

        assertThrows(BusinessException.class, () -> service.creerDate(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 15));
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }

    @Test
    void creerDate_ok() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 15)))
                .thenReturn(false);
        when(fermetureSiteRepository.save(any(FermetureSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 15));
        req.setMotif("Jour férié");

        FermetureSite res = service.creerDate(1L, req);

        assertNotNull(res);
        assertSame(site, res.getSite());
        assertEquals(LocalDate.of(2030, 1, 15), res.getDate());
        assertNull(res.getDateDebut());
        assertNull(res.getDateFin());
        assertEquals("Jour férié", res.getMotif());

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 15));
        verify(fermetureSiteRepository).save(any(FermetureSite.class));
    }

    // --------
    // creerPeriode
    // --------

    @Test
    void creerPeriode_refuse_si_admin_hors_perimetre() {
        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));

        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.creerPeriode(2L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void creerPeriode_requestNull_refuse() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerPeriode_refuse_si_dates_absentes() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerPeriode_refuse_si_dateFin_avant_dateDebut() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 15));
        req.setDateFin(LocalDate.of(2030, 2, 10));

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verifyNoInteractions(fermetureSiteRepository);
    }

    @Test
    void creerPeriode_ok() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.save(any(FermetureSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));
        req.setMotif("Travaux");

        FermetureSite res = service.creerPeriode(1L, req);

        assertNotNull(res);
        assertSame(site, res.getSite());
        assertNull(res.getDate());
        assertEquals(LocalDate.of(2030, 2, 10), res.getDateDebut());
        assertEquals(LocalDate.of(2030, 2, 15), res.getDateFin());
        assertEquals("Travaux", res.getMotif());

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).save(any(FermetureSite.class));
    }

    // --------
    // updateDate
    // --------

    @Test
    void updateDate_refuse_si_admin_hors_perimetre() {
        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        UpdateFermetureSiteDateRequest req = new UpdateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 3, 10));

        assertThrows(ForbiddenException.class, () -> service.updateDate(2L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void updateDate_refuse_si_request_null() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(LocalDate.of(2030, 1, 15));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        assertThrows(BusinessException.class, () -> service.updateDate(1L, 10L, null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
        verify(fermetureSiteRepository, never()).save(any());
    }

    @Test
    void updateDate_refuse_doublon_sur_autre_fermeture() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(LocalDate.of(2030, 1, 15));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));
        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 20))).thenReturn(true);

        UpdateFermetureSiteDateRequest req = new UpdateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 20));

        assertThrows(BusinessException.class, () -> service.updateDate(1L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository, never()).save(any());
    }

    @Test
    void updateDate_ok() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(LocalDate.of(2030, 1, 15));
        fermeture.setMotif("Ancien");

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));
        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, LocalDate.of(2030, 1, 20))).thenReturn(false);
        when(fermetureSiteRepository.save(any(FermetureSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateFermetureSiteDateRequest req = new UpdateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 1, 20));
        req.setMotif("Nouveau");

        FermetureSite res = service.updateDate(1L, 10L, req);

        assertEquals(LocalDate.of(2030, 1, 20), res.getDate());
        assertNull(res.getDateDebut());
        assertNull(res.getDateFin());
        assertEquals("Nouveau", res.getMotif());

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository).save(fermeture);
    }

    // --------
    // updatePeriode
    // --------

    @Test
    void updatePeriode_refuse_si_admin_hors_perimetre() {
        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));

        assertThrows(ForbiddenException.class, () -> service.updatePeriode(2L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void updatePeriode_refuse_si_request_null() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        assertThrows(BusinessException.class, () -> service.updatePeriode(1L, 10L, null));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
        verify(fermetureSiteRepository, never()).save(any());
    }

    @Test
    void updatePeriode_refuse_si_dates_absentes() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();

        assertThrows(BusinessException.class, () -> service.updatePeriode(1L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository, never()).save(any());
    }

    @Test
    void updatePeriode_refuse_si_dateFin_avant_dateDebut() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 15));
        req.setDateFin(LocalDate.of(2030, 2, 10));

        assertThrows(BusinessException.class, () -> service.updatePeriode(1L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository, never()).save(any());
    }

    @Test
    void updatePeriode_ok() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(LocalDate.of(2030, 1, 15));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));
        when(fermetureSiteRepository.save(any(FermetureSite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));
        req.setMotif("Travaux");

        FermetureSite res = service.updatePeriode(1L, 10L, req);

        assertNull(res.getDate());
        assertEquals(LocalDate.of(2030, 2, 10), res.getDateDebut());
        assertEquals(LocalDate.of(2030, 2, 15), res.getDateFin());
        assertEquals("Travaux", res.getMotif());

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository).save(fermeture);
    }

    // --------
    // delete
    // --------

    @Test
    void delete_refuse_si_admin_hors_perimetre() {
        doThrow(new ForbiddenException("Accès refusé à ce site"))
                .when(serviceAutorisationAdmin).verifierAccesAuSite(2L);

        assertThrows(ForbiddenException.class, () -> service.delete(2L, 10L));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(2L);
        verifyNoInteractions(siteRepository, fermetureSiteRepository);
    }

    @Test
    void delete_ok() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));

        service.delete(1L, 10L);

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
        verify(fermetureSiteRepository).delete(fermeture);
    }

    // --------
    // isDateFermeePourSite
    // --------

    @Test
    void isDateFermeePourSite_siteIdNull_refuse() {
        assertThrows(BusinessException.class,
                () -> service.isDateFermeePourSite(null, LocalDate.of(2030, 1, 15)));

        verifyNoInteractions(serviceAutorisationAdmin, siteRepository, fermetureSiteRepository);
    }

    @Test
    void isDateFermeePourSite_dateNull_refuse() {
        assertThrows(BusinessException.class,
                () -> service.isDateFermeePourSite(1L, null));

        verifyNoInteractions(serviceAutorisationAdmin, siteRepository, fermetureSiteRepository);
    }

    @Test
    void isDateFermeePourSite_true_si_dateUnique() {
        LocalDate date = LocalDate.of(2030, 1, 15);

        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, date)).thenReturn(true);

        boolean res = service.isDateFermeePourSite(1L, date);

        assertTrue(res);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, date);
        verify(fermetureSiteRepository, never())
                .existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(anyLong(), any(), any());
        verifyNoInteractions(serviceAutorisationAdmin, siteRepository);
    }

    @Test
    void isDateFermeePourSite_true_si_date_dans_periode() {
        LocalDate date = LocalDate.of(2030, 2, 12);

        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, date)).thenReturn(false);
        when(fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(1L, date, date))
                .thenReturn(true);

        boolean res = service.isDateFermeePourSite(1L, date);

        assertTrue(res);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, date);
        verify(fermetureSiteRepository)
                .existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(1L, date, date);
        verifyNoInteractions(serviceAutorisationAdmin, siteRepository);
    }

    @Test
    void isDateFermeePourSite_false_si_hors_fermeture() {
        LocalDate date = LocalDate.of(2030, 3, 5);

        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, date)).thenReturn(false);
        when(fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(1L, date, date))
                .thenReturn(false);

        boolean res = service.isDateFermeePourSite(1L, date);

        assertFalse(res);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, date);
        verify(fermetureSiteRepository)
                .existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(1L, date, date);
        verifyNoInteractions(serviceAutorisationAdmin, siteRepository);
    }
    @Test
    void creerPeriode_refuse_si_periode_deja_existante() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        )).thenReturn(true);

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));
        req.setMotif("Travaux");

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        );
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
    @Test
    void updatePeriode_refuse_doublon_exact_sur_autre_fermeture() {
        Site site = new Site("Site A", "Bruxelles");
        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDateDebut(LocalDate.of(2030, 2, 1));
        fermeture.setDateFin(LocalDate.of(2030, 2, 3));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L)).thenReturn(Optional.of(fermeture));
        when(fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        )).thenReturn(true);

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));
        req.setMotif("Travaux");

        assertThrows(BusinessException.class, () -> service.updatePeriode(1L, 10L, req));

        verify(serviceAutorisationAdmin, times(1)).verifierAccesAuSite(1L);
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
    @Test
    void creerDate_refuse_si_date_dans_periode_existante() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.existsBySiteIdAndDate(1L, LocalDate.of(2030, 2, 12)))
                .thenReturn(false);
        when(fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                1L,
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 12)
        )).thenReturn(true);

        CreateFermetureSiteDateRequest req = new CreateFermetureSiteDateRequest();
        req.setDate(LocalDate.of(2030, 2, 12));
        req.setMotif("Jour férié");

        assertThrows(BusinessException.class, () -> service.creerDate(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDate(1L, LocalDate.of(2030, 2, 12));
        verify(fermetureSiteRepository)
                .existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        1L,
                        LocalDate.of(2030, 2, 12),
                        LocalDate.of(2030, 2, 12)
                );
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
    @Test
    void creerPeriode_refuse_si_chevauche_une_periode_existante() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        when(fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 20)
        )).thenReturn(false);

        when(fermetureSiteRepository.existsPeriodeChevauchante(
                1L,
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 20)
        )).thenReturn(true);

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 12));
        req.setDateFin(LocalDate.of(2030, 2, 20));
        req.setMotif("Travaux");

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 20)
        );
        verify(fermetureSiteRepository).existsPeriodeChevauchante(
                1L,
                LocalDate.of(2030, 2, 12),
                LocalDate.of(2030, 2, 20)
        );
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
    @Test
    void creerPeriode_refuse_si_contient_date_unique_existante() {
        Site site = new Site("Site A", "Bruxelles");
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        when(fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        )).thenReturn(false);

        when(fermetureSiteRepository.existsPeriodeChevauchante(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        )).thenReturn(false);

        when(fermetureSiteRepository.existsBySiteIdAndDateBetween(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        )).thenReturn(true);

        CreateFermetureSitePeriodeRequest req = new CreateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 10));
        req.setDateFin(LocalDate.of(2030, 2, 15));
        req.setMotif("Travaux");

        assertThrows(BusinessException.class, () -> service.creerPeriode(1L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        );
        verify(fermetureSiteRepository).existsPeriodeChevauchante(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        );
        verify(fermetureSiteRepository).existsBySiteIdAndDateBetween(
                1L,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15)
        );
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
    @Test
    void updatePeriode_refuse_si_chevauche_une_autre_periode() {
        Site site = new Site("Site A", "Bruxelles");

        FermetureSite fermetureCourante = mock(FermetureSite.class);
        when(fermetureCourante.getId()).thenReturn(10L);
        when(fermetureCourante.getSite()).thenReturn(site);
        when(fermetureCourante.getDateDebut()).thenReturn(LocalDate.of(2030, 2, 1));
        when(fermetureCourante.getDateFin()).thenReturn(LocalDate.of(2030, 2, 3));
        when(fermetureCourante.getDate()).thenReturn(null);

        FermetureSite autreFermeture = mock(FermetureSite.class);
        when(autreFermeture.getId()).thenReturn(20L);
        when(autreFermeture.getSite()).thenReturn(site);
        when(autreFermeture.getDateDebut()).thenReturn(LocalDate.of(2030, 2, 12));
        when(autreFermeture.getDateFin()).thenReturn(LocalDate.of(2030, 2, 20));
        when(autreFermeture.getDate()).thenReturn(null);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(fermetureSiteRepository.findByIdAndSiteId(10L, 1L))
                .thenReturn(Optional.of(fermetureCourante));

        when(fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 15),
                LocalDate.of(2030, 2, 25)
        )).thenReturn(false);

        when(fermetureSiteRepository.findBySiteIdOrderByDateAscDateDebutAsc(1L))
                .thenReturn(List.of(fermetureCourante, autreFermeture));

        UpdateFermetureSitePeriodeRequest req = new UpdateFermetureSitePeriodeRequest();
        req.setDateDebut(LocalDate.of(2030, 2, 15));
        req.setDateFin(LocalDate.of(2030, 2, 25));
        req.setMotif("Nouvelle période");

        assertThrows(BusinessException.class, () -> service.updatePeriode(1L, 10L, req));

        verify(serviceAutorisationAdmin).verifierAccesAuSite(1L);
        verify(siteRepository).findById(1L);
        verify(fermetureSiteRepository).findByIdAndSiteId(10L, 1L);
        verify(fermetureSiteRepository).existsBySiteIdAndDateDebutAndDateFin(
                1L,
                LocalDate.of(2030, 2, 15),
                LocalDate.of(2030, 2, 25)
        );
        verify(fermetureSiteRepository).findBySiteIdOrderByDateAscDateDebutAsc(1L);
        verify(fermetureSiteRepository, never()).save(any(FermetureSite.class));
    }
}