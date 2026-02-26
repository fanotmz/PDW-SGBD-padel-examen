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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private FermetureGlobaleRepository fermetureGlobaleRepo;

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
        fermetureGlobaleRepo = mock(FermetureGlobaleRepository.class);

        service = new MatchPadelService(
                matchRepo, terrainRepo, joueurRepo,
                soldeService, participationRepo,
                paiementService, paiementRepo,
                fermetureGlobaleRepo
        );
    }

    // ----------------
    // Helpers
    // ----------------

    private LocalDateTime dateValide() {
        return LocalDateTime.now()
                .plusDays(2)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private void stubSiteOuvert(Terrain t) {
        Site site = mock(Site.class);
        when(t.getSite()).thenReturn(site);

        when(site.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(site.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(site.getJoursFermeture()).thenReturn(Set.of());
    }

    private Joueur stubOrgaGlobalSansDette(String matricule) {
        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById(matricule)).thenReturn(Optional.of(orga));
        return orga;
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
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(dateValide());
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
        when(m.getDateDebut()).thenReturn(dateValide());
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
        when(m.getDateDebut()).thenReturn(dateValide());
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
                service.creerMatch(null, "G0001", dateValide(), MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_organisateurBlank_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "   ", dateValide(), MatchVisibilite.PUBLIC));
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
                service.creerMatch(1L, "G0001", dateValide(), null));
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
                service.creerMatch(1L, "G0001", dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(joueurRepo, matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_joueurIntrouvable_notFound() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.creerMatch(1L, "G0001", dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_refuse_si_dette_orga_positive() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getSolde()).thenReturn(new BigDecimal("0.01"));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    // ----------------
    // creerMatch droits réservation : GLOBAL / SITE / LIBRE
    // ----------------

    @Test
    void creerMatch_global_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusWeeks(3).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_libre_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.LIBRE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(joueurRepo.findById("L0001")).thenReturn(Optional.of(orga));

        LocalDateTime date = LocalDateTime.now().plusDays(5).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "L0001", date, MatchVisibilite.PUBLIC));
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

        LocalDateTime date = dateValide();

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

        LocalDateTime date = dateValide();

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

        LocalDateTime date = LocalDateTime.now().plusWeeks(2).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "S0001", date, MatchVisibilite.PUBLIC));
    }

    // ----------------
    // Issue 14 - overlap terrain
    // ----------------

    @Test
    void creerMatch_refuse_si_terrain_occupe_overlap() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);
        when(fermetureGlobaleRepo.existsByDate(any())).thenReturn(false);

        stubOrgaGlobalSansDette("G0001");

        LocalDateTime date = dateValide();

        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.minusMinutes(30));

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_ok_si_match_existant_finit_juste_a_la_limite_105min() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);
        when(fermetureGlobaleRepo.existsByDate(any())).thenReturn(false);

        stubOrgaGlobalSansDette("G0001");

        LocalDateTime date = dateValide();

        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.minusMinutes(105));

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        MatchPadel savedMatch = mock(MatchPadel.class);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
    }

    // ----------------
    // Issue 30 - fermeture globale
    // ----------------

    @Test
    void creerMatch_refuse_si_fermeture_globale() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        stubOrgaGlobalSansDette("G0001");

        LocalDateTime date = LocalDateTime.of(2026, 12, 25, 10, 0);
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_ok_si_pas_fermeture_globale() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        stubOrgaGlobalSansDette("G0001");

        LocalDateTime date = dateValide();
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        MatchPadel savedMatch = mock(MatchPadel.class);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
    }

    // ----------------
    // Issue 30 - horaires + jours fermeture site
    // ----------------

    @Test
    void creerMatch_refuse_si_site_ferme_ce_jour() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Site site = mock(Site.class);
        when(t.getSite()).thenReturn(site);

        LocalDateTime date = dateValide();
        when(site.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(site.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(site.getJoursFermeture()).thenReturn(Set.of(date.getDayOfWeek()));

        stubOrgaGlobalSansDette("G0001");
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_refuse_si_hors_horaires_fin_depasse_fermeture() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Site site = mock(Site.class);
        when(t.getSite()).thenReturn(site);

        LocalDateTime date = LocalDateTime.now()
                .plusDays(2)
                .withHour(21).withMinute(30).withSecond(0).withNano(0);

        when(site.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(site.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(site.getJoursFermeture()).thenReturn(Set.of());

        stubOrgaGlobalSansDette("G0001");
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_ok_si_dans_horaires_et_site_ouvert() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        stubOrgaGlobalSansDette("G0001");

        LocalDateTime date = dateValide();
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        MatchPadel savedMatch = mock(MatchPadel.class);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, "G0001", date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
    }
}