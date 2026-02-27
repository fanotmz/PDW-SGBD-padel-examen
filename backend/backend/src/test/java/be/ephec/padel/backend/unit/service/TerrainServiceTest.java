package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.service.TerrainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TerrainServiceTest {

    private TerrainRepository terrainRepo;
    private SiteRepository siteRepo;
    private TerrainService service;

    @BeforeEach
    void setup() {
        terrainRepo = mock(TerrainRepository.class);
        siteRepo = mock(SiteRepository.class);
        service = new TerrainService(terrainRepo, siteRepo);
    }

    // --------
    // lister
    // --------
    @Test
    void lister_ok() {
        when(terrainRepo.findAll()).thenReturn(List.of(mock(Terrain.class), mock(Terrain.class)));

        List<Terrain> res = service.lister();

        assertEquals(2, res.size());
        verify(terrainRepo).findAll();
    }

    // ----------------
    // listerParSite
    // ----------------
    @Test
    void listerParSite_siteIdNull_refuse() {
        assertThrows(BusinessException.class, () -> service.listerParSite(null));
        verifyNoInteractions(terrainRepo, siteRepo);
    }

    @Test
    void listerParSite_ok() {
        when(terrainRepo.findBySite_Id(1L)).thenReturn(List.of(mock(Terrain.class)));

        List<Terrain> res = service.listerParSite(1L);

        assertEquals(1, res.size());
        verify(terrainRepo).findBySite_Id(1L);
        verifyNoInteractions(siteRepo);
    }

    // --------
    // getTerrain
    // --------
    @Test
    void getTerrain_idNull_refuse() {
        assertThrows(BusinessException.class, () -> service.getTerrain(null));
        verifyNoInteractions(terrainRepo, siteRepo);
    }

    @Test
    void getTerrain_introuvable_notFound() {
        when(terrainRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getTerrain(10L));
        verify(terrainRepo).findById(10L);
    }

    @Test
    void getTerrain_ok() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(10L)).thenReturn(Optional.of(t));

        Terrain res = service.getTerrain(10L);

        assertSame(t, res);
        verify(terrainRepo).findById(10L);
    }

    // --------
    // creerTerrain
    // --------
    @Test
    void creerTerrain_nomNullOuBlank_refuse() {
        assertThrows(BusinessException.class, () -> service.creerTerrain(null, 1L));
        assertThrows(BusinessException.class, () -> service.creerTerrain("   ", 1L));
        verifyNoInteractions(terrainRepo, siteRepo);
    }

    @Test
    void creerTerrain_siteIdNull_refuse() {
        assertThrows(BusinessException.class, () -> service.creerTerrain("T1", null));
        verifyNoInteractions(terrainRepo, siteRepo);
    }

    @Test
    void creerTerrain_siteIntrouvable_notFound() {
        when(siteRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.creerTerrain("T1", 1L));

        verify(siteRepo).findById(1L);
        verifyNoInteractions(terrainRepo);
    }

    @Test
    void creerTerrain_terrainDejaExistant_refuse() {
        Site site = mock(Site.class);
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));
        when(terrainRepo.existsByNomAndSiteId("T1", 1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.creerTerrain("T1", 1L));

        verify(siteRepo).findById(1L);
        verify(terrainRepo).existsByNomAndSiteId("T1", 1L);
        verify(terrainRepo, never()).save(any(Terrain.class));
    }

    @Test
    void creerTerrain_ok_save() {
        Site site = mock(Site.class);
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));
        when(terrainRepo.existsByNomAndSiteId("T1", 1L)).thenReturn(false);
        when(terrainRepo.save(any(Terrain.class))).thenAnswer(inv -> inv.getArgument(0));

        Terrain res = service.creerTerrain("T1", 1L);

        assertNotNull(res);
        assertEquals("T1", res.getNom());
        assertSame(site, res.getSite());

        verify(siteRepo).findById(1L);
        verify(terrainRepo).existsByNomAndSiteId("T1", 1L);
        verify(terrainRepo).save(any(Terrain.class));
    }
}