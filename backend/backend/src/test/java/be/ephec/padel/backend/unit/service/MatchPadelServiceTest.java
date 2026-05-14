package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.response.CreneauxMatchResponseDto;
import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    private FermetureSiteService fermetureSiteService;
    private HoraireSiteService horaireSiteService;
    private CurrentUserFacade currentUserFacade;
    private ServiceAutorisationAdmin serviceAutorisationAdmin;
    private Clock clock;

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
        fermetureSiteService = mock(FermetureSiteService.class);
        horaireSiteService = mock(HoraireSiteService.class);
        currentUserFacade = mock(CurrentUserFacade.class);
        serviceAutorisationAdmin = mock(ServiceAutorisationAdmin.class);
        clock = Clock.fixed(
                Instant.parse("2026-03-24T10:00:00Z"),
                ZoneId.of("Europe/Brussels")
        );

        service = new MatchPadelService(
                matchRepo,
                terrainRepo,
                soldeService,
                participationRepo,
                paiementService,
                horaireSiteService,
                fermetureSiteService,
                paiementRepo,
                fermetureGlobaleRepo,
                clock,
                currentUserFacade,
                serviceAutorisationAdmin
        );

        stubCurrentJoueur("G0001", TypeJoueur.GLOBAL, BigDecimal.ZERO);
    }

    // ----------------
    // Helpers
    // ----------------

    private LocalDateTime dateValide() {
        return LocalDateTime.now(clock)
                .plusDays(2)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private void stubSiteOuvert(Terrain t) {
        Site site = mock(Site.class);
        when(t.getSite()).thenReturn(site);

        when(site.getId()).thenReturn(1L);
        when(site.getJoursFermeture()).thenReturn(Set.of());

        HoraireSite horaire = mock(HoraireSite.class);
        when(horaire.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(horaire.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));

        when(horaireSiteService.getApplicable(eq(1L), any(LocalDateTime.class)))
                .thenReturn(horaire);
    }

    private Terrain stubTerrainPourCreneaux(Long terrainId, Long siteId, LocalDate date) {
        return stubTerrainPourCreneaux(terrainId, siteId, date, LocalTime.of(8, 0), LocalTime.of(22, 0));
    }

    private Terrain stubTerrainPourCreneaux(
            Long terrainId,
            Long siteId,
            LocalDate date,
            LocalTime heureOuverture,
            LocalTime heureFermeture) {
        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        HoraireSite horaire = mock(HoraireSite.class);

        when(terrain.getSite()).thenReturn(site);
        when(site.getId()).thenReturn(siteId);
        when(site.getJoursFermeture()).thenReturn(Set.of());
        when(horaire.getHeureOuverture()).thenReturn(heureOuverture);
        when(horaire.getHeureFermeture()).thenReturn(heureFermeture);

        when(terrainRepo.findById(terrainId)).thenReturn(Optional.of(terrain));
        when(horaireSiteService.getApplicable(siteId, date.atStartOfDay())).thenReturn(horaire);
        when(fermetureGlobaleRepo.existsByDate(date)).thenReturn(false);
        when(fermetureSiteService.isDateFermeePourSite(siteId, date)).thenReturn(false);
        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(terrainId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        return terrain;
    }

    private Joueur stubOrgaGlobalSansDette(String matricule) {
        return stubCurrentJoueur(matricule, TypeJoueur.GLOBAL, BigDecimal.ZERO);
    }

    private Joueur stubCurrentJoueur(String matricule, TypeJoueur type, BigDecimal solde) {
        Joueur joueur = mock(Joueur.class);
        when(joueur.getMatricule()).thenReturn(matricule);
        when(joueur.getType()).thenReturn(type);
        when(joueur.getSolde()).thenReturn(solde);
        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        return joueur;
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
        when(m.getStatut()).thenReturn(MatchStatut.PLANIFIE);

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

        Participation p1 = mock(Participation.class);
        Participation p2 = mock(Participation.class);
        when(p1.getId()).thenReturn(11L);
        when(p2.getId()).thenReturn(12L);
        when(m.getParticipations()).thenReturn(List.of(p1, p2));
        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));

        when(paiementRepo.sumMontantByParticipationIdAndType(11L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("15.00"));
        when(paiementRepo.sumMontantByParticipationIdAndType(12L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("5.00"));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getTerrainId());
        assertEquals("T1", dto.getTerrainNom());
        assertEquals(5L, dto.getSiteId());
        assertEquals("G0001", dto.getOrganisateurMatricule());
        assertEquals(MatchStatut.PLANIFIE, dto.getStatut());
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
        when(m.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(m.getParticipations()).thenReturn(List.of());

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getMontantPaye()));
        assertEquals(Tarifs.PRIX_MATCH, dto.getResteAPayer());
    }

    @Test
    void getMatchDto_resteJamaisNegatif() {
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(dateValide());
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(m.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        Participation p1 = mock(Participation.class);
        when(p1.getId()).thenReturn(21L);
        when(m.getParticipations()).thenReturn(List.of(p1));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(paiementRepo.sumMontantByParticipationIdAndType(21L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("25.00"));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(new BigDecimal("15.00"), dto.getMontantPaye());
        assertEquals(new BigDecimal("45.00"), dto.getResteAPayer());
    }

    @Test
    void getMatchDto_matchAnnule_resteAPayerZero_et_statutVisible() {
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(dateValide());
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(m.getStatut()).thenReturn(MatchStatut.ANNULE);
        Participation p1 = mock(Participation.class);
        when(p1.getId()).thenReturn(31L);
        when(m.getParticipations()).thenReturn(List.of(p1));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(paiementRepo.sumMontantByParticipationIdAndType(31L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("25.00"));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(MatchStatut.ANNULE, dto.getStatut());
        assertEquals(new BigDecimal("15.00"), dto.getMontantPaye());
        assertEquals(BigDecimal.ZERO, dto.getResteAPayer());
    }

    @Test
    void getMatchDto_paiementAvecRattrapageDette_estPlafonneA15ParParticipation() {
        MatchPadel m = mock(MatchPadel.class);
        when(m.getId()).thenReturn(1L);
        when(m.getDateDebut()).thenReturn(dateValide());
        when(m.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(m.getStatut()).thenReturn(MatchStatut.PLANIFIE);

        Participation p1 = mock(Participation.class);
        Participation p2 = mock(Participation.class);
        when(p1.getId()).thenReturn(41L);
        when(p2.getId()).thenReturn(42L);
        when(m.getParticipations()).thenReturn(List.of(p1, p2));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(paiementRepo.sumMontantByParticipationIdAndType(41L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("25.00"));
        when(paiementRepo.sumMontantByParticipationIdAndType(42L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("10.00"));

        MatchDto dto = service.getMatchDto(1L);

        assertEquals(new BigDecimal("25.00"), dto.getMontantPaye());
        assertEquals(new BigDecimal("35.00"), dto.getResteAPayer());
    }

    // ----------------
    // creerMatch validations
    // ----------------

    @Test
    void creerMatch_terrainIdNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(null, dateValide(), MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_organisateurBlank_refuse() {
        when(currentUserFacade.getCurrentJoueur()).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_dateNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, null, MatchVisibilite.PUBLIC));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_visibiliteNull_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, dateValide(), null));
        verifyNoInteractions(matchRepo, terrainRepo, joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_datePasDansFutur_refuse() {
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, LocalDateTime.now(clock), MatchVisibilite.PUBLIC));
        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, LocalDateTime.now(clock).minusMinutes(1), MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_terrainIntrouvable_notFound() {
        when(terrainRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(joueurRepo, matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_sansOrganisateurAuthentifie_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        when(currentUserFacade.getCurrentJoueur()).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_refuse_si_dette_orga_positive() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getSolde()).thenReturn(new BigDecimal("0.01"));
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC));

        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_refuse_si_penalite_active() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(orga.getPenaliteJusqua()).thenReturn(LocalDateTime.now(clock).plusDays(1));
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC));

        assertTrue(ex.getMessage().toLowerCase().contains("activ"));
        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void creerMatch_ok_si_penalite_expiree() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);
        when(fermetureGlobaleRepo.existsByDate(any())).thenReturn(false);
        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.GLOBAL);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(orga.getPenaliteJusqua()).thenReturn(LocalDateTime.now(clock).minusSeconds(1));
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(orga));

        MatchPadel savedMatch = mock(MatchPadel.class);
        when(savedMatch.getId()).thenReturn(77L);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, dateValide(), MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
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
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        LocalDateTime date = LocalDateTime.now(clock).plusWeeks(3).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_libre_tropLoin_refuse() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));
        stubSiteOuvert(t);

        Joueur orga = mock(Joueur.class);
        when(orga.getType()).thenReturn(TypeJoueur.LIBRE);
        when(orga.getSolde()).thenReturn(BigDecimal.ZERO);
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        LocalDateTime date = LocalDateTime.now(clock).plusDays(5).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        LocalDateTime date = dateValide();

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        LocalDateTime date = dateValide();

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        Site siteJoueur = mock(Site.class);
        when(siteJoueur.getId()).thenReturn(1L);
        when(orga.getSite()).thenReturn(siteJoueur);

        LocalDateTime date = LocalDateTime.now(clock).plusWeeks(2).plusMinutes(1);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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
        when(savedMatch.getId()).thenReturn(77L);
        when(matchRepo.save(any(MatchPadel.class))).thenReturn(savedMatch);

        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(123L);
        when(participationRepo.save(any(Participation.class))).thenReturn(p);

        MatchPadel res = service.creerMatch(1L, date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
        verify(soldeService).debiter(eq("G0001"), eq(Tarifs.PART_PAR_JOUEUR), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.CREATION_MATCH_ORGANISATEUR
                        && Long.valueOf(123L).equals(context.getParticipationId())
                        && Long.valueOf(77L).equals(context.getMatchId())
        ));
        verify(paiementService).payerParticipation(123L, Tarifs.PART_PAR_JOUEUR);
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
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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

        MatchPadel res = service.creerMatch(1L, date, MatchVisibilite.PUBLIC);
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
        when(site.getId()).thenReturn(1L);
        when(t.getSite()).thenReturn(site);

        LocalDateTime date = dateValide();
        when(site.getJoursFermeture()).thenReturn(Set.of(date.getDayOfWeek()));

        HoraireSite horaire = mock(HoraireSite.class);
        when(horaire.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(horaire.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(horaireSiteService.getApplicable(eq(1L), any(LocalDateTime.class)))
                .thenReturn(horaire);

        stubOrgaGlobalSansDette("G0001");
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
    }

    @Test
    void creerMatch_refuse_si_hors_horaires_fin_depasse_fermeture() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Site site = mock(Site.class);
        when(site.getId()).thenReturn(1L);
        when(t.getSite()).thenReturn(site);

        LocalDateTime date = LocalDateTime.now(clock)
                .plusDays(2)
                .withHour(21).withMinute(30).withSecond(0).withNano(0);

        when(site.getJoursFermeture()).thenReturn(Set.of());

        HoraireSite horaire = mock(HoraireSite.class);
        when(horaire.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(horaire.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(horaireSiteService.getApplicable(eq(1L), eq(date))).thenReturn(horaire);

        stubOrgaGlobalSansDette("G0001");
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));
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

        MatchPadel res = service.creerMatch(1L, date, MatchVisibilite.PUBLIC);
        assertSame(savedMatch, res);
    }

    @Test
    void getPublicMatchSummaries_calcule_placesRestantes_et_complet() {
        PublicMatchSummaryProjection row = mock(PublicMatchSummaryProjection.class);

        when(row.getId()).thenReturn(1L);
        when(row.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));
        when(row.getSiteId()).thenReturn(5L);
        when(row.getSiteNom()).thenReturn("Site Delta");
        when(row.getTerrainId()).thenReturn(10L);
        when(row.getTerrainNom()).thenReturn("Terrain 1");
        when(row.getOrganisateurMatricule()).thenReturn("G0001");
        when(row.getNbParticipants()).thenReturn(2L);

        when(matchRepo.findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                any(LocalDateTime.class),
                isNull(),
                isNull()
        )).thenReturn(List.of(row));

        List<PublicMatchSummaryDto> dtos = service.getPublicMatchSummaries(null, null, null);

        assertEquals(1, dtos.size());
        PublicMatchSummaryDto dto = dtos.get(0);

        assertEquals(1L, dto.getId());
        assertEquals("Site Delta", dto.getSiteNom());
        assertEquals("Terrain 1", dto.getTerrainNom());
        assertEquals(2, dto.getNbParticipants());
        assertEquals(2, dto.getPlacesRestantes());
        assertFalse(dto.isComplet());
        assertEquals(0, Tarifs.PART_PAR_JOUEUR.compareTo(dto.getMontantParJoueur()));
    }

    @Test
    void getPublicMatchSummaries_match_complet_si_4_participants() {
        PublicMatchSummaryProjection row = mock(PublicMatchSummaryProjection.class);

        when(row.getId()).thenReturn(1L);
        when(row.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));
        when(row.getSiteId()).thenReturn(5L);
        when(row.getSiteNom()).thenReturn("Site Delta");
        when(row.getTerrainId()).thenReturn(10L);
        when(row.getTerrainNom()).thenReturn("Terrain 1");
        when(row.getOrganisateurMatricule()).thenReturn("G0001");
        when(row.getNbParticipants()).thenReturn(4L);

        when(matchRepo.findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                any(LocalDateTime.class),
                isNull(),
                isNull()
        )).thenReturn(List.of(row));

        PublicMatchSummaryDto dto = service.getPublicMatchSummaries(null, null, null).get(0);

        assertEquals(4, dto.getNbParticipants());
        assertEquals(0, dto.getPlacesRestantes());
        assertTrue(dto.isComplet());
    }

    @Test
    void getPublicMatchSummaries_throw_businessException_si_from_apres_to() {
        LocalDate from = LocalDate.of(2030, 2, 1);
        LocalDate to = LocalDate.of(2030, 1, 1);

        assertThrows(BusinessException.class,
                () -> service.getPublicMatchSummaries(from, to, null));

        verify(matchRepo, never()).findPublicMatchSummaries(any(), any(), any(), any());
    }

    @Test
    void getPublicMatchSummaries_transmet_siteId_null_au_repository() {
        when(matchRepo.findPublicMatchSummaries(any(), any(), any(), any())).thenReturn(List.of());

        service.getPublicMatchSummaries(
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 1, 2),
                null
        );

        verify(matchRepo).findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        );
    }

    @Test
    void getPublicMatchSummaries_transmet_siteId_renseigne_au_repository() {
        when(matchRepo.findPublicMatchSummaries(any(), any(), any(), any())).thenReturn(List.of());

        service.getPublicMatchSummaries(
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 1, 2),
                5L
        );

        verify(matchRepo).findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(5L)
        );
    }

    @Test
    void getPublicMatchSummaries_normalise_from_null_en_debut_de_jour_courant() {
        when(matchRepo.findPublicMatchSummaries(any(), any(), any(), any())).thenReturn(List.of());

        service.getPublicMatchSummaries(
                null,
                LocalDate.of(2030, 1, 2),
                null
        );

        verify(matchRepo).findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                eq(LocalDate.of(2026, 3, 24).atStartOfDay()),
                any(LocalDateTime.class),
                isNull()
        );
    }

    @Test
    void getPublicMatchSummaries_normalise_to_en_fin_de_jour() {
        when(matchRepo.findPublicMatchSummaries(any(), any(), any(), any())).thenReturn(List.of());

        service.getPublicMatchSummaries(
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 1, 2),
                null
        );

        verify(matchRepo).findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                eq(LocalDate.of(2030, 1, 1).atStartOfDay()),
                eq(LocalDate.of(2030, 1, 2).atTime(LocalTime.MAX)),
                isNull()
        );
    }

    @Test
    void getPublicMatchSummaries_conserve_to_null() {
        when(matchRepo.findPublicMatchSummaries(any(), any(), any(), any())).thenReturn(List.of());

        service.getPublicMatchSummaries(
                LocalDate.of(2030, 1, 1),
                null,
                null
        );

        verify(matchRepo).findPublicMatchSummaries(
                eq(MatchVisibilite.PUBLIC),
                eq(LocalDate.of(2030, 1, 1).atStartOfDay()),
                isNull(),
                isNull()
        );
    }

    @Test
    void getMatchDetailDto_public_sansMatricule_ok() {
        stubCurrentJoueur("G0009", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");

        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);

        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of());

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertEquals(1L, dto.getId());
        assertEquals("Site Delta", dto.getSiteNom());
        assertEquals("Terrain 1", dto.getTerrainNom());
        assertEquals("G0001", dto.getOrganisateurMatricule());
        assertEquals(MatchStatut.PLANIFIE, dto.getStatut());
        assertEquals(0, dto.getNbParticipants());
        assertEquals(4, dto.getPlacesRestantes());
        assertFalse(dto.isComplet());
        assertFalse(dto.isPeutAjouterJoueurPrive());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getMontantRembourse()));
    }

    @Test
    void getMatchDetailDto_prive_sansMatricule_refuse() {
        when(currentUserFacade.getCurrentJoueur()).thenThrow(new ForbiddenException("Utilisateur authentifie requis."));

        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));

        assertThrows(ForbiddenException.class,
                () -> service.getMatchDetailDto(1L));

        verify(paiementRepo, never()).sumMontantByParticipationIdAndType(anyLong(), any());
    }

    @Test
    void getMatchDetailDto_prive_organisateur_ok() {
        when(currentUserFacade.isAdmin()).thenReturn(false);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");

        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);

        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of());

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertEquals(1L, dto.getId());
        assertEquals("G0001", dto.getOrganisateurMatricule());
        assertTrue(dto.isPeutAjouterJoueurPrive());
    }

    @Test
    void getMatchDetailDto_prive_participant_ok() {
        when(currentUserFacade.isAdmin()).thenReturn(false);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);
        Joueur joueur = mock(Joueur.class);
        Participation participation = mock(Participation.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");

        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);

        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(joueur.getMatricule()).thenReturn("J0001");
        when(joueur.getNom()).thenReturn("Alice");
        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);

        when(participation.getJoueur()).thenReturn(joueur);
        when(participation.getId()).thenReturn(51L);

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of(participation));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));
        when(paiementRepo.sumMontantByParticipationIdAndType(51L, TypePaiement.ENCAISSEMENT)).thenReturn(BigDecimal.ZERO);
        when(paiementRepo.sumMontantByParticipationIdAndType(51L, TypePaiement.REMBOURSEMENT)).thenReturn(BigDecimal.ZERO);

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertEquals(1, dto.getParticipants().size());
        assertEquals("J0001", dto.getParticipants().get(0).getMatricule());
        assertFalse(dto.isPeutAjouterJoueurPrive());
    }

    @Test
    void getMatchDetailDto_prive_adminGlobal_peutAjouterJoueurPrive_true() {
        when(currentUserFacade.isAdmin()).thenReturn(true);
        stubCurrentJoueur("X9999", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");
        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);
        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of());
        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));
        when(serviceAutorisationAdmin.peutAdministrerSite(5L)).thenReturn(true);

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertTrue(dto.isPeutAjouterJoueurPrive());
    }

    @Test
    void getMatchDetailDto_prive_adminSiteBonPerimetre_peutAjouterJoueurPrive_true() {
        when(currentUserFacade.isAdmin()).thenReturn(true);
        stubCurrentJoueur("X9999", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");
        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);
        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of());
        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));
        when(serviceAutorisationAdmin.peutAdministrerSite(5L)).thenReturn(true);

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertTrue(dto.isPeutAjouterJoueurPrive());
    }

    @Test
    void getMatchDetailDto_prive_adminSiteHorsPerimetre_peutAjouterJoueurPrive_false() {
        when(currentUserFacade.isAdmin()).thenReturn(true);
        stubCurrentJoueur("X9999", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");
        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);
        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        when(match.getParticipations()).thenReturn(List.of());
        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));
        when(serviceAutorisationAdmin.peutAdministrerSite(5L)).thenReturn(false);

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertFalse(dto.isPeutAjouterJoueurPrive());
    }

    @Test
    void getMatchDetailDto_prive_autreJoueur_refuse() {
        when(currentUserFacade.isAdmin()).thenReturn(false);
        stubCurrentJoueur("X9999", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        Joueur orga = mock(Joueur.class);
        Joueur joueur = mock(Joueur.class);
        Participation participation = mock(Participation.class);

        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        when(orga.getMatricule()).thenReturn("G0001");
        when(match.getOrganisateur()).thenReturn(orga);

        when(joueur.getMatricule()).thenReturn("J0001");
        when(participation.getJoueur()).thenReturn(joueur);
        when(match.getParticipations()).thenReturn(List.of(participation));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));

        assertThrows(ForbiddenException.class,
                () -> service.getMatchDetailDto(1L));

        verify(paiementRepo, never()).sumMontantByParticipationIdAndType(anyLong(), any());
    }

    @Test
    void getMatchDetailDto_matchAnnule_exposeStatut_et_montantRembourse() {
        stubCurrentJoueur("G0009", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(1L);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(match.getStatut()).thenReturn(MatchStatut.ANNULE);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(2030, 1, 1, 10, 0));

        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur orga = mock(Joueur.class);

        when(site.getId()).thenReturn(5L);
        when(site.getNom()).thenReturn("Site Delta");
        when(terrain.getId()).thenReturn(10L);
        when(terrain.getNom()).thenReturn("Terrain 1");
        when(terrain.getSite()).thenReturn(site);
        when(orga.getMatricule()).thenReturn("G0001");
        when(orga.getNom()).thenReturn("Orga");
        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);

        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(orga);
        Participation participation = mock(Participation.class);
        Joueur joueur = mock(Joueur.class);
        when(participation.getId()).thenReturn(61L);
        when(joueur.getMatricule()).thenReturn("J0002");
        when(joueur.getNom()).thenReturn("Bob");
        when(participation.getJoueur()).thenReturn(joueur);
        when(match.getParticipations()).thenReturn(List.of(participation));

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.of(match));
        when(paiementRepo.sumMontantByParticipationIdAndType(61L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("25.00"));
        when(paiementRepo.sumMontantByParticipationIdAndType(61L, TypePaiement.REMBOURSEMENT))
                .thenReturn(new BigDecimal("-20.00"));

        MatchDetailDto dto = service.getMatchDetailDto(1L);

        assertEquals(MatchStatut.ANNULE, dto.getStatut());
        assertFalse(dto.isPeutAjouterJoueurPrive());
        assertEquals(new BigDecimal("15.00"), dto.getMontantPaye());
        assertEquals(new BigDecimal("15.00"), dto.getMontantRembourse());
        assertEquals(BigDecimal.ZERO, dto.getResteAPayer());
    }

    @Test
    void getMatchDetailDto_introuvable_notFound() {
        stubCurrentJoueur("G0009", TypeJoueur.GLOBAL, BigDecimal.ZERO);

        when(matchRepo.findByIdWithDetails(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.getMatchDetailDto(1L));
    }

    @Test
    void creerMatch_refuse_si_fermeture_site_exceptionnelle() {
        Terrain t = mock(Terrain.class);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(t));

        Site site = mock(Site.class);
        when(site.getId()).thenReturn(99L);
        when(t.getSite()).thenReturn(site);

        LocalDateTime date = dateValide();

        when(site.getJoursFermeture()).thenReturn(Set.of());

        HoraireSite horaire = mock(HoraireSite.class);
        when(horaire.getHeureOuverture()).thenReturn(LocalTime.of(8, 0));
        when(horaire.getHeureFermeture()).thenReturn(LocalTime.of(22, 0));
        when(horaireSiteService.getApplicable(eq(99L), eq(date))).thenReturn(horaire);

        stubOrgaGlobalSansDette("G0001");
        when(fermetureGlobaleRepo.existsByDate(date.toLocalDate())).thenReturn(false);
        when(fermetureSiteService.isDateFermeePourSite(99L, date.toLocalDate())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.creerMatch(1L, date, MatchVisibilite.PUBLIC));

        assertTrue(ex.getMessage().toLowerCase().contains("site"));
        verify(fermetureSiteService).isDateFermeePourSite(99L, date.toLocalDate());
        verifyNoInteractions(matchRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void getCreneauxDisponibles_horaire_8_22_retourne_dernier_20_15_et_pas_22_00() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().contains("08:00"));
        assertTrue(response.getCreneaux().contains("20:15"));
        assertFalse(response.getCreneaux().contains("20:30"));
        assertFalse(response.getCreneaux().contains("22:00"));
        assertNull(response.getMessage());
        assertEquals(2026, response.getAnnee());
        assertEquals(LocalTime.of(8, 0), response.getHeureOuverture());
        assertEquals(LocalTime.of(22, 0), response.getHeureFermeture());
        assertEquals(90L, response.getDureeMatchMinutes());
        assertEquals(15L, response.getBufferMinutes());
    }

    @Test
    void getCreneauxDisponibles_horaire_8_23_retourne_dernier_21_15() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date, LocalTime.of(8, 0), LocalTime.of(23, 0));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().contains("21:15"));
        assertFalse(response.getCreneaux().contains("21:30"));
        assertFalse(response.getCreneaux().contains("23:00"));
        assertEquals("21:15", response.getCreneaux().get(response.getCreneaux().size() - 1));
        assertNull(response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_horaire_manquant_retourne_200_metier_liste_vide_message() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        when(terrain.getSite()).thenReturn(site);
        when(site.getId()).thenReturn(1L);
        when(terrainRepo.findById(1L)).thenReturn(Optional.of(terrain));
        when(horaireSiteService.getApplicable(1L, date.atStartOfDay()))
                .thenThrow(new BusinessException("Aucun horaire configuré"));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Aucun horaire n'est configuré pour ce site et cette année.", response.getMessage());
        assertEquals(2026, response.getAnnee());
        assertNull(response.getHeureOuverture());
        assertNull(response.getHeureFermeture());
        assertEquals(90L, response.getDureeMatchMinutes());
        assertEquals(15L, response.getBufferMinutes());
        verify(matchRepo, never()).findByTerrainIdAndDateDebutBetween(anyLong(), any(), any());
    }

    @Test
    void getCreneauxDisponibles_fermeture_globale_retourne_liste_vide_message_site_ferme() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);
        when(fermetureGlobaleRepo.existsByDate(date)).thenReturn(true);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Le site est fermé à cette date. Aucun créneau n'est disponible.", response.getMessage());
        assertNull(response.getHeureOuverture());
        assertNull(response.getHeureFermeture());
        verify(matchRepo, never()).findByTerrainIdAndDateDebutBetween(anyLong(), any(), any());
    }

    @Test
    void getCreneauxDisponibles_jour_fermeture_site_retourne_liste_vide_message_site_ferme() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        Terrain terrain = stubTerrainPourCreneaux(1L, 1L, date);
        when(terrain.getSite().getJoursFermeture()).thenReturn(Set.of(date.getDayOfWeek()));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Le site est fermé à cette date. Aucun créneau n'est disponible.", response.getMessage());
        assertNull(response.getHeureOuverture());
        assertNull(response.getHeureFermeture());
        verify(matchRepo, never()).findByTerrainIdAndDateDebutBetween(anyLong(), any(), any());
    }

    @Test
    void getCreneauxDisponibles_fermeture_site_exceptionnelle_retourne_liste_vide_message_site_ferme() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);
        when(fermetureSiteService.isDateFermeePourSite(1L, date)).thenReturn(true);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Le site est fermé à cette date. Aucun créneau n'est disponible.", response.getMessage());
        assertNull(response.getHeureOuverture());
        assertNull(response.getHeureFermeture());
        verify(matchRepo, never()).findByTerrainIdAndDateDebutBetween(anyLong(), any(), any());
    }

    @Test
    void getCreneauxDisponibles_terrain_occupe_retire_les_creneaux_chevauchants() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);
        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.atTime(10, 0));
        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().contains("08:15"));
        assertFalse(response.getCreneaux().contains("08:30"));
        assertFalse(response.getCreneaux().contains("10:00"));
        assertFalse(response.getCreneaux().contains("11:30"));
        assertTrue(response.getCreneaux().contains("11:45"));
    }

    @Test
    void getCreneauxDisponibles_terrain_totalement_occupe_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date, LocalTime.of(8, 0), LocalTime.of(9, 45));
        MatchPadel existing = mock(MatchPadel.class);
        when(existing.getDateDebut()).thenReturn(date.atTime(8, 0));
        when(matchRepo.findByTerrainIdAndDateDebutBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existing));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Tous les créneaux de cette date sont déjà occupés pour ce terrain.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_horaire_trop_court_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date, LocalTime.of(8, 0), LocalTime.of(9, 30));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("L'horaire du site ne permet pas de placer un match complet à cette date.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_dette_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);
        stubCurrentJoueur("G0001", TypeJoueur.GLOBAL, BigDecimal.TEN);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Vous ne pouvez pas réserver tant qu'un solde est dû.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_penalite_active_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 1L, date);
        Joueur joueur = stubCurrentJoueur("G0001", TypeJoueur.GLOBAL, BigDecimal.ZERO);
        when(joueur.getPenaliteJusqua()).thenReturn(LocalDateTime.now(clock).plusDays(2));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Vous ne pouvez pas réserver pendant votre période de pénalité.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_global_trop_loin_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        stubTerrainPourCreneaux(1L, 1L, date);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Vous pouvez réserver au maximum 3 semaines à l'avance.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_site_trop_loin_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        stubTerrainPourCreneaux(1L, 1L, date);
        Joueur joueur = stubCurrentJoueur("S0001", TypeJoueur.SITE, BigDecimal.ZERO);
        Site siteJoueur = mock(Site.class);
        when(siteJoueur.getId()).thenReturn(1L);
        when(joueur.getSite()).thenReturn(siteJoueur);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Vous pouvez réserver au maximum 2 semaines à l'avance.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_libre_trop_loin_retourne_liste_vide() {
        LocalDate date = LocalDate.of(2026, 4, 15);
        stubTerrainPourCreneaux(1L, 1L, date);
        stubCurrentJoueur("L0001", TypeJoueur.LIBRE, BigDecimal.ZERO);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Vous pouvez réserver au maximum 5 jours à l'avance.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_joueur_site_hors_site_retourne_liste_vide() {
        LocalDate date = LocalDate.of(2026, 3, 25);
        stubTerrainPourCreneaux(1L, 2L, date);

        Joueur joueur = stubCurrentJoueur("S0001", TypeJoueur.SITE, BigDecimal.ZERO);
        Site siteJoueur = mock(Site.class);
        when(siteJoueur.getId()).thenReturn(1L);
        when(joueur.getSite()).thenReturn(siteJoueur);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Votre abonnement SITE permet de réserver uniquement sur votre site.", response.getMessage());
    }

    @Test
    void getCreneauxDisponibles_date_du_jour_ne_propose_pas_les_heures_passees() {
        LocalDate date = LocalDate.of(2026, 3, 24);
        stubTerrainPourCreneaux(1L, 1L, date);

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertFalse(response.getCreneaux().contains("08:00"));
        assertFalse(response.getCreneaux().contains("11:00"));
        assertTrue(response.getCreneaux().contains("11:15"));
    }

    @Test
    void getCreneauxDisponibles_date_du_jour_sans_creneau_futur_retourne_message_dedie() {
        LocalDate date = LocalDate.of(2026, 3, 24);
        stubTerrainPourCreneaux(1L, 1L, date, LocalTime.of(8, 0), LocalTime.of(12, 45));

        CreneauxMatchResponseDto response = service.getCreneauxDisponibles(1L, date);

        assertTrue(response.getCreneaux().isEmpty());
        assertEquals("Il n'y a plus de créneau disponible aujourd'hui pour ce terrain.", response.getMessage());
    }
}

