package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchPadelServiceTest {

    private MatchPadelRepository matchRepo;
    private TerrainRepository terrainRepo;
    private JoueurRepository joueurRepo;
    private SoldeService soldeService;
    private ParticipationRepository participationRepo;
    private PaiementService paiementService;
    private PaiementRepository paiementRepo;

    private MatchPadelService service;

    @BeforeEach
    void setup() {
        matchRepo = mock(MatchPadelRepository.class);
        terrainRepo = mock(TerrainRepository.class);
        joueurRepo = mock(JoueurRepository.class);
        soldeService = mock(SoldeService.class);
        participationRepo = mock(ParticipationRepository.class);
        paiementService = mock(PaiementService.class);
        paiementRepo = mock(PaiementRepository.class);

        service = new MatchPadelService(
                matchRepo, terrainRepo, joueurRepo,
                soldeService, participationRepo,
                paiementService, paiementRepo
        );
    }

    // ----------------
    // getMatch / getMatchDto
    // ----------------

    @Test
    void getMatch_introuvable_notFound() {
        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getMatch(1L));
    }

    @Test
    void getMatchDto_calcule_montants_et_nbParticipants() {
        // Match mocké
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(LocalDateTime.now().plusDays(1));
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);

        Terrain t = mock(Terrain.class);
        when(t.getId()).thenReturn(10L);
        when(t.getNom()).thenReturn("T1");
        Site s = mock(Site.class);
        when(s.getId()).thenReturn(5L);
        when(t.getSite()).thenReturn(s);
        when(m.getTerrain()).thenReturn(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G0001");
        when(m.getOrganisateur()).thenReturn(orga);

        when(m.getParticipations()).thenReturn(List.of(mock(Participation.class), mock(Participation.class)));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));

        // payé 20 sur un prix match (Tarifs.PRIX_MATCH)
        when(paiementRepo.sumMontantByMatchId(1L)).thenReturn(new BigDecimal("20.00"));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getTerrainId());
        assertEquals("T1", dto.getTerrainNom());
        assertEquals(5L, dto.getSiteId());
        assertEquals("G0001", dto.getOrganisateurMatricule());
        assertEquals(2, dto.getNbParticipants());

        assertEquals(Tarifs.PRIX_MATCH, dto.getMontantTotal());
        assertEquals(new BigDecimal("20.00"), dto.getMontantPaye());
        assertEquals(Tarifs.PRIX_MATCH.subtract(new BigDecimal("20.00")), dto.getResteAPayer());
    }

    @Test
    void getMatchDto_montantPayeNull_considererZero() {
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(LocalDateTime.now().plusDays(1));
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(m.getParticipations()).thenReturn(List.of());

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(paiementRepo.sumMontantByMatchId(1L)).thenReturn(null);

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(BigDecimal.ZERO, dto.getMontantPaye());
        assertEquals(Tarifs.PRIX_MATCH, dto.getResteAPayer());
    }

    @Test
    void getMatchDto_resteJamaisNegatif() {
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(LocalDateTime.now().plusDays(1));
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(m.getParticipations()).thenReturn(List.of());

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(paiementRepo.sumMontantByMatchId(1L)).thenReturn(Tarifs.PRIX_MATCH.add(new BigDecimal("1.00")));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(BigDecimal.ZERO, dto.getResteAPayer());
    }

    // ----------------
    // creerMatch validations
    // ----------------

    @Test
    void creerMatch_terrainIdNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(null, "G0001", LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_organisateurBlank_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "   ", LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_dateNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", null, MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_visibiliteNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now().plusDays(1), null));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_datePasDansFutur_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now(), MatchVisibilite.PUBLIC));
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now().minusMinutes(1), MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_terrainIntrouvable_notFound() {
        when(terrainRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));

        verifyNoInteractions(joueurRepo, matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_joueurIntrouvable_notFound() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_refuse_si_dette_orga_positive() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getSolde()).thenReturn(new BigDecimal("0.01"));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    // ----------------
    // creerMatch droits réservation : GLOBAL / SITE / LIBRE
    // ----------------

    @Test
    void creerMatch_global_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        // > 3 semaines
        LocalDateTime date = LocalDateTime.now().plusWeeks(3).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_libre_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.LIBRE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("L0001")).thenReturn(Optional.of(orga));

        // > 5 jours
        LocalDateTime date = LocalDateTime.now().plusDays(5).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "L0001", date, MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_site_sansSiteAssocie_refuse() {
        Terrain t = mock(Terrain.class);
        Site siteTerrain = mock(Site.class);
        when(siteTerrain.getId()).thenReturn(1L);
        when(t.getSite()).thenReturn(siteTerrain);

        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.SITE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(orga.getSite()).thenReturn(null);

        when(joueurRepo.findById("S0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusDays(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "S0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_site_mauvaisSite_refuse() {
        Terrain t = mock(Terrain.class);
        Site siteTerrain = mock(Site.class);
        when(siteTerrain.getId()).thenReturn(2L);
        when(t.getSite()).thenReturn(siteTerrain);

        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.SITE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);

        Site siteJoueur = mock(Site.class);
        when(siteJoueur.getId()).thenReturn(1L);
        when(orga.getSite()).thenReturn(siteJoueur);

        when(joueurRepo.findById("S0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusDays(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "S0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_site_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        Site siteTerrain = mock(Site.class);
        when(siteTerrain.getId()).thenReturn(1L);
        when(t.getSite()).thenReturn(siteTerrain);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.SITE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("S0001")).thenReturn(Optional.of(orga));

        Site siteJoueur = mock(Site.class);
        when(siteJoueur.getId()).thenReturn(1L);
        when(orga.getSite()).thenReturn(siteJoueur);

        // > 2 semaines
        LocalDateTime date = LocalDateTime.now().plusWeeks(2).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "S0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_refuse_si_terrain_occupe_overlap() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusDays(2);

        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.minusMinutes(30));

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));

        verify(matchRepo, never()).save(any());
        verify(participationRepo, never()).save(any());
        verify(soldeService, never()).debiter(anyString(), any());
        verify(paiementService, never()).payerParticipation(anyLong(), any());
    }

    @Test
    void creerMatch_ok_si_match_existant_finit_juste_a_la_limite_105min() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusDays(2);

        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.minusMinutes(105)); // pile à la limite

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        MatchPadel savedMatch = mock(MatchPadel.class);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);

        verify(matchRepo).save(any(MatchPadel.class));
        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G0001"), eq(Tarifs.PART_PAR_JOUEUR));
        verify(paiementService).payerParticipation(eq(123L), eq(Tarifs.PART_PAR_JOUEUR));
    }

    // ----------------
    // creerMatch happy paths
    // ----------------

    @Test
    void creerMatch_ok_global_cree_match_participation_debite_et_paye() {
        // terrain
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        // orga
        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // save match
        MatchPadel savedMatch = mock(MatchPadel.class);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        // save participation orga -> doit avoir un id pour payerParticipation
        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        LocalDateTime date = LocalDateTime.now().plusDays(2);

        MatchPadel res = service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC);

        assertSame(savedMatch, res);

        verify(matchRepo).save(any(MatchPadel.class));
        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G0001"), eq(Tarifs.PART_PAR_JOUEUR));
        verify(paiementService).payerParticipation(eq(123L), eq(Tarifs.PART_PAR_JOUEUR));
    }
}