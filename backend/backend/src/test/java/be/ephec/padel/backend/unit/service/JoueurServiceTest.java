package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JoueurServiceTest {

    private JoueurRepository joueurRepo;
    private SiteRepository siteRepo;
    private JoueurService service;

    @BeforeEach
    void setup() {
        joueurRepo = mock(JoueurRepository.class);
        siteRepo = mock(SiteRepository.class);
        service = new JoueurService(joueurRepo, siteRepo);
    }

    // ----------------
    // creerJoueur validations
    // ----------------

    @Test
    void creerJoueur_matriculeNull_ouBlank_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur(null, "Nom", TypeJoueur.GLOBAL, null));
        assertThrows(BusinessException.class, () -> service.creerJoueur("   ", "Nom", TypeJoueur.GLOBAL, null));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    @Test
    void creerJoueur_nomNull_ouBlank_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur("G0001", null, TypeJoueur.GLOBAL, null));
        assertThrows(BusinessException.class, () -> service.creerJoueur("G0001", "   ", TypeJoueur.GLOBAL, null));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    @Test
    void creerJoueur_typeNull_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur("G0001", "Nom", null, null));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    // ----------------
    // creerJoueur matricule patterns
    // ----------------

    @Test
    void creerJoueur_global_matriculeInvalide_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur("S0001", "Nom", TypeJoueur.GLOBAL, null));
        assertThrows(BusinessException.class, () -> service.creerJoueur("G12", "Nom", TypeJoueur.GLOBAL, null));
        assertThrows(BusinessException.class, () -> service.creerJoueur("G000A", "Nom", TypeJoueur.GLOBAL, null));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    @Test
    void creerJoueur_site_matriculeInvalide_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur("G0001", "Nom", TypeJoueur.SITE, 1L));
        assertThrows(BusinessException.class, () -> service.creerJoueur("S12", "Nom", TypeJoueur.SITE, 1L));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    @Test
    void creerJoueur_libre_matriculeInvalide_refuse() {
        assertThrows(BusinessException.class, () -> service.creerJoueur("G0001", "Nom", TypeJoueur.LIBRE, null));
        assertThrows(BusinessException.class, () -> service.creerJoueur("L12", "Nom", TypeJoueur.LIBRE, null));
        verifyNoInteractions(joueurRepo, siteRepo);
    }

    // ----------------
    // creerJoueur unicité + site rules
    // ----------------

    @Test
    void creerJoueur_matriculeDejaUtilise_refuse() {
        when(joueurRepo.existsById("G0001")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.creerJoueur("G0001", "Nom", TypeJoueur.GLOBAL, null));

        verify(joueurRepo, never()).save(any());
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerJoueur_site_typeSansSiteId_refuse() {
        when(joueurRepo.existsById("S0001")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.creerJoueur("S0001", "Nom", TypeJoueur.SITE, null));

        verifyNoInteractions(siteRepo);
        verify(joueurRepo, never()).save(any());
    }

    @Test
    void creerJoueur_site_siteIntrouvable_notFound() {
        when(joueurRepo.existsById("S0001")).thenReturn(false);
        when(siteRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.creerJoueur("S0001", "Nom", TypeJoueur.SITE, 1L));

        verify(joueurRepo, never()).save(any());
    }

    @Test
    void creerJoueur_global_avecSiteId_refuse() {
        when(joueurRepo.existsById("G0001")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.creerJoueur("G0001", "Nom", TypeJoueur.GLOBAL, 1L));

        verify(joueurRepo, never()).save(any());
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerJoueur_libre_avecSiteId_refuse() {
        when(joueurRepo.existsById("L0001")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.creerJoueur("L0001", "Nom", TypeJoueur.LIBRE, 1L));

        verify(joueurRepo, never()).save(any());
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerJoueur_global_ok_soldeZero_et_save() {
        when(joueurRepo.existsById("G0001")).thenReturn(false);
        when(joueurRepo.save(any(Joueur.class))).thenAnswer(inv -> inv.getArgument(0));

        Joueur j = service.creerJoueur("G0001", "Nom", TypeJoueur.GLOBAL, null);

        assertNotNull(j);
        assertEquals("G0001", j.getMatricule());
        assertEquals(TypeJoueur.GLOBAL, j.getType());
        assertEquals(new BigDecimal("0.00"), j.getSolde().setScale(2));
        verify(joueurRepo).save(any(Joueur.class));
        verifyNoInteractions(siteRepo);
    }

    @Test
    void creerJoueur_site_ok_associeSite_et_save() {
        when(joueurRepo.existsById("S0001")).thenReturn(false);

        Site site = mock(Site.class);
        when(siteRepo.findById(1L)).thenReturn(Optional.of(site));
        when(joueurRepo.save(any(Joueur.class))).thenAnswer(inv -> inv.getArgument(0));

        Joueur j = service.creerJoueur("S0001", "Nom", TypeJoueur.SITE, 1L);

        assertNotNull(j);
        assertEquals("S0001", j.getMatricule());
        assertEquals(TypeJoueur.SITE, j.getType());
        assertEquals(new BigDecimal("0.00"), j.getSolde().setScale(2));
        verify(siteRepo).findById(1L);
        verify(joueurRepo).save(any(Joueur.class));
    }

    // ----------------
    // getJoueur / lister
    // ----------------

    @Test
    void getJoueur_introuvable_notFound() {
        when(joueurRepo.findById("G0001")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getJoueur("G0001"));
    }

    @Test
    void getJoueur_ok() {
        Joueur j = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(j));
        assertSame(j, service.getJoueur("G0001"));
    }

    @Test
    void lister_ok() {
        when(joueurRepo.findAll()).thenReturn(List.of(mock(Joueur.class), mock(Joueur.class)));
        assertEquals(2, service.lister().size());
    }

    // ----------------
    // dette checks
    // ----------------

    @Test
    void aDette_false_si_soldeNull_ou_zero() {
        Joueur j = mock(Joueur.class);
        when(j.getSolde()).thenReturn(null);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(j));
        assertFalse(service.aDette("G0001"));

        Joueur j2 = mock(Joueur.class);
        when(j2.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0002")).thenReturn(Optional.of(j2));
        assertFalse(service.aDette("G0002"));
    }

    @Test
    void aDette_true_si_soldePositif() {
        Joueur j = mock(Joueur.class);
        when(j.getSolde()).thenReturn(new BigDecimal("0.01"));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(j));
        assertTrue(service.aDette("G0001"));
    }

    @Test
    void verifierPasDeDette_refuse_si_dette() {
        Joueur j = mock(Joueur.class);
        when(j.getSolde()).thenReturn(new BigDecimal("1.00"));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(j));

        assertThrows(BusinessException.class, () -> service.verifierPasDeDette("G0001"));
    }

    @Test
    void verifierPasDeDette_ok_si_pas_dette() {
        Joueur j = mock(Joueur.class);
        when(j.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(j));

        assertDoesNotThrow(() -> service.verifierPasDeDette("G0001"));
    }
}