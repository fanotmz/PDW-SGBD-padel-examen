package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.UpsertHoraireSiteRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.service.HoraireSiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HoraireSiteServiceTest {

    private HoraireSiteRepository horaireSiteRepository;
    private SiteRepository siteRepository;
    private HoraireSiteService service;

    @BeforeEach
    void setup() {
        horaireSiteRepository = mock(HoraireSiteRepository.class);
        siteRepository = mock(SiteRepository.class);
        service = new HoraireSiteService(horaireSiteRepository, siteRepository);
    }

    private UpsertHoraireSiteRequest req(int annee, int hOpen, int hClose) {
        UpsertHoraireSiteRequest req = new UpsertHoraireSiteRequest();
        req.setAnnee(annee);
        req.setHeureOuverture(LocalTime.of(hOpen, 0));
        req.setHeureFermeture(LocalTime.of(hClose, 0));
        return req;
    }

    private Site site(Long id) {
        Site s = mock(Site.class);
        when(s.getId()).thenReturn(id);
        return s;
    }

    private HoraireSite horaire(Long id, Site site, int annee, int hOpen, int hClose) {
        HoraireSite h = new HoraireSite(site, annee, LocalTime.of(hOpen, 0), LocalTime.of(hClose, 0));
        h.setId(id);
        return h;
    }

    @Test
    void create_ok() {
        Site site = site(1L);
        UpsertHoraireSiteRequest req = req(2026, 8, 22);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(horaireSiteRepository.existsBySiteIdAndAnnee(1L, 2026)).thenReturn(false);
        when(horaireSiteRepository.save(any(HoraireSite.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        HoraireSite created = service.create(1L, req);

        assertNotNull(created);
        assertEquals(site, created.getSite());
        assertEquals(2026, created.getAnnee());
        assertEquals(LocalTime.of(8, 0), created.getHeureOuverture());
        assertEquals(LocalTime.of(22, 0), created.getHeureFermeture());

        verify(siteRepository).findById(1L);
        verify(horaireSiteRepository).existsBySiteIdAndAnnee(1L, 2026);
        verify(horaireSiteRepository).save(any(HoraireSite.class));
    }

    @Test
    void create_refuse_si_site_introuvable() {
        when(siteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(1L, req(2026, 8, 22)));

        verify(siteRepository).findById(1L);
        verifyNoInteractions(horaireSiteRepository);
    }

    @Test
    void create_refuse_si_doublon_meme_site_meme_annee() {
        Site site = site(1L);
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(horaireSiteRepository.existsBySiteIdAndAnnee(1L, 2026)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(1L, req(2026, 8, 22)));

        verify(siteRepository).findById(1L);
        verify(horaireSiteRepository).existsBySiteIdAndAnnee(1L, 2026);
        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    void create_refuse_si_annee_null() {
        Site site = site(1L);
        UpsertHoraireSiteRequest req = new UpsertHoraireSiteRequest();
        req.setHeureOuverture(LocalTime.of(8, 0));
        req.setHeureFermeture(LocalTime.of(22, 0));

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        assertThrows(BusinessException.class, () -> service.create(1L, req));

        verify(siteRepository).findById(1L);
        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    void create_refuse_si_heures_null() {
        Site site = site(1L);
        UpsertHoraireSiteRequest req = new UpsertHoraireSiteRequest();
        req.setAnnee(2026);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        assertThrows(BusinessException.class, () -> service.create(1L, req));

        verify(siteRepository).findById(1L);
        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    void create_refuse_si_ouverture_apres_ou_egale_fermeture() {
        Site site = site(1L);
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));

        assertThrows(BusinessException.class, () -> service.create(1L, req(2026, 22, 22)));
        assertThrows(BusinessException.class, () -> service.create(1L, req(2026, 23, 22)));

        verify(siteRepository, times(2)).findById(1L);
        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    void update_ok() {
        Site site = site(1L);
        HoraireSite existing = horaire(10L, site, 2026, 8, 22);
        UpsertHoraireSiteRequest req = req(2027, 9, 21);

        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(horaireSiteRepository.existsBySiteIdAndAnneeAndIdNot(1L, 2027, 10L)).thenReturn(false);

        HoraireSite updated = service.update(1L, 10L, req);

        assertSame(existing, updated);
        assertEquals(2027, updated.getAnnee());
        assertEquals(LocalTime.of(9, 0), updated.getHeureOuverture());
        assertEquals(LocalTime.of(21, 0), updated.getHeureFermeture());

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository).existsBySiteIdAndAnneeAndIdNot(1L, 2027, 10L);
    }

    @Test
    void update_refuse_si_horaire_introuvable() {
        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(1L, 10L, req(2027, 9, 21)));

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository, never()).existsBySiteIdAndAnneeAndIdNot(any(), any(), any());
    }

    @Test
    void update_refuse_si_horaire_non_associe_au_site() {
        Site autreSite = site(2L);
        HoraireSite existing = horaire(10L, autreSite, 2026, 8, 22);

        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () -> service.update(1L, 10L, req(2027, 9, 21)));

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository, never()).existsBySiteIdAndAnneeAndIdNot(any(), any(), any());
    }

    @Test
    void update_refuse_si_collision_avec_autre_horaire() {
        Site site = site(1L);
        HoraireSite existing = horaire(10L, site, 2026, 8, 22);

        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(horaireSiteRepository.existsBySiteIdAndAnneeAndIdNot(1L, 2027, 10L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.update(1L, 10L, req(2027, 9, 21)));

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository).existsBySiteIdAndAnneeAndIdNot(1L, 2027, 10L);
    }

    @Test
    void listBySite_ok() {
        when(siteRepository.existsById(1L)).thenReturn(true);

        Site site = site(1L);
        List<HoraireSite> expected = List.of(
                horaire(1L, site, 2026, 8, 22),
                horaire(2L, site, 2027, 9, 21)
        );

        when(horaireSiteRepository.findBySiteIdOrderByAnneeAsc(1L)).thenReturn(expected);

        List<HoraireSite> result = service.listBySite(1L);

        assertEquals(2, result.size());
        verify(siteRepository).existsById(1L);
        verify(horaireSiteRepository).findBySiteIdOrderByAnneeAsc(1L);
    }

    @Test
    void listBySite_refuse_si_site_introuvable() {
        when(siteRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.listBySite(1L));

        verify(siteRepository).existsById(1L);
        verify(horaireSiteRepository, never()).findBySiteIdOrderByAnneeAsc(anyLong());
    }

    @Test
    void getBySiteAndAnnee_ok() {
        Site site = site(1L);
        HoraireSite h = horaire(1L, site, 2026, 8, 22);

        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026)).thenReturn(Optional.of(h));

        HoraireSite result = service.getBySiteAndAnnee(1L, 2026);

        assertSame(h, result);
        verify(horaireSiteRepository).findBySiteIdAndAnnee(1L, 2026);
    }

    @Test
    void getBySiteAndAnnee_refuse_si_introuvable() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getBySiteAndAnnee(1L, 2026));

        verify(horaireSiteRepository).findBySiteIdAndAnnee(1L, 2026);
    }

    @Test
    void delete_ok() {
        Site site = site(1L);
        HoraireSite h = horaire(10L, site, 2026, 8, 22);

        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.of(h));

        service.delete(1L, 10L);

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository).delete(h);
    }

    @Test
    void delete_refuse_si_horaire_introuvable() {
        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(1L, 10L));

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository, never()).delete(any());
    }

    @Test
    void delete_refuse_si_horaire_non_associe_au_site() {
        Site autreSite = site(2L);
        HoraireSite h = horaire(10L, autreSite, 2026, 8, 22);

        when(horaireSiteRepository.findById(10L)).thenReturn(Optional.of(h));

        assertThrows(BusinessException.class, () -> service.delete(1L, 10L));

        verify(horaireSiteRepository).findById(10L);
        verify(horaireSiteRepository, never()).delete(any());
    }

    @Test
    void getApplicable_retourne_horaire_de_l_annee_du_match() {
        Site site = site(1L);
        HoraireSite h = horaire(1L, site, 2026, 8, 22);
        LocalDateTime dateDebut = LocalDateTime.of(2026, 5, 10, 10, 0);

        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026)).thenReturn(Optional.of(h));

        HoraireSite result = service.getApplicable(1L, dateDebut);

        assertSame(h, result);
        verify(horaireSiteRepository).findBySiteIdAndAnnee(1L, 2026);
    }

    @Test
    void getApplicable_refuse_si_aucun_horaire_pour_l_annee() {
        LocalDateTime dateDebut = LocalDateTime.of(2027, 5, 10, 10, 0);

        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2027)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getApplicable(1L, dateDebut));

        verify(horaireSiteRepository).findBySiteIdAndAnnee(1L, 2027);
    }
}