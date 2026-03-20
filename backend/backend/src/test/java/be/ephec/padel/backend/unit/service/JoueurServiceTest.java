package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.PlayerMatchSummaryDto;
import be.ephec.padel.backend.dto.response.OrganizerMatchSummaryDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.service.JoueurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JoueurServiceTest {

    private JoueurRepository joueurRepo;
    private SiteRepository siteRepo;
    private JoueurService service;
    private MatchPadelRepository matchPadelRepo;
    private ParticipationRepository participationRepo;

    @BeforeEach
    void setup() {
        joueurRepo = mock(JoueurRepository.class);
        siteRepo = mock(SiteRepository.class);
        participationRepo = mock(ParticipationRepository.class);
        matchPadelRepo = mock(MatchPadelRepository.class);
        service = new JoueurService(joueurRepo, siteRepo, participationRepo,matchPadelRepo);
    }

    private Participation mockParticipation(String organisateurMatricule,
                                            String joueurMatricule,
                                            LocalDate dateMatch,
                                            MatchVisibilite visibilite,
                                            boolean paiementEffectue,
                                            Long terrainId,
                                            String terrainNom,
                                            Long siteId,
                                            String siteNom) {
        Participation participation = mock(Participation.class);
        MatchPadel match = mock(MatchPadel.class);
        Joueur organisateur = mock(Joueur.class);
        Joueur joueur = mock(Joueur.class);
        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);

        when(participation.getMatch()).thenReturn(match);
        when(participation.getJoueur()).thenReturn(joueur);

        when(match.getId()).thenReturn(999L);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(dateMatch, LocalTime.of(10, 0)));
        when(match.getVisibilite()).thenReturn(visibilite);
        when(match.getOrganisateur()).thenReturn(organisateur);
        when(match.getTerrain()).thenReturn(terrain);

        when(organisateur.getMatricule()).thenReturn(organisateurMatricule);
        when(joueur.getMatricule()).thenReturn(joueurMatricule);

        when(terrain.getId()).thenReturn(terrainId);
        when(terrain.getNom()).thenReturn(terrainNom);
        when(terrain.getSite()).thenReturn(site);

        when(site.getId()).thenReturn(siteId);
        when(site.getNom()).thenReturn(siteNom);

        if (paiementEffectue) {
            when(participation.getPaiements()).thenReturn(List.of(mock(Paiement.class)));
        } else {
            when(participation.getPaiements()).thenReturn(List.of());
        }

        return participation;
    }
    private MatchPadel mockOrganizedMatch(LocalDate dateMatch,
                                          MatchVisibilite visibilite,
                                          int nbParticipations,
                                          Long matchId,
                                          Long terrainId,
                                          String terrainNom,
                                          Long siteId,
                                          String siteNom) {
        MatchPadel match = mock(MatchPadel.class);
        Terrain terrain = mock(Terrain.class);
        Site site = mock(Site.class);
        Joueur organisateur = mock(Joueur.class);

        when(match.getId()).thenReturn(matchId);
        when(match.getDateDebut()).thenReturn(LocalDateTime.of(dateMatch, LocalTime.of(10, 0)));
        when(match.getVisibilite()).thenReturn(visibilite);
        when(match.getTerrain()).thenReturn(terrain);
        when(match.getOrganisateur()).thenReturn(organisateur);

        when(terrain.getId()).thenReturn(terrainId);
        when(terrain.getNom()).thenReturn(terrainNom);
        when(terrain.getSite()).thenReturn(site);

        when(site.getId()).thenReturn(siteId);
        when(site.getNom()).thenReturn(siteNom);

        when(organisateur.getMatricule()).thenReturn("G0001");

        List<Participation> participations = java.util.stream.IntStream.range(0, nbParticipations)
                .mapToObj(i -> mock(Participation.class))
                .toList();
        when(match.getParticipations()).thenReturn(participations);

        return match;
    }
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
// ----------------
// getPlayerMatches
// ----------------

    @Test
    void getPlayerMatches_joueurIntrouvable_notFound() {
        when(joueurRepo.findById("G0001")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPlayerMatches("G0001"));

        verify(joueurRepo).findById("G0001");
        verifyNoInteractions(participationRepo);
    }

    @Test
    void getPlayerMatches_joueurExistant_sansParticipation_retourneListeVide() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));
        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of());

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(joueurRepo).findById("G0001");
        verify(participationRepo).findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001");
    }

    @Test
    void getPlayerMatches_matchFutur_participant_nonPaye() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        Participation participation = mockParticipation(
                "G9999",                // organisateur
                "G0001",                // joueur courant
                LocalDate.now().plusDays(5),
                MatchVisibilite.PUBLIC,
                false,                  // paiement
                10L,
                "Terrain 1",
                1L,
                "Site Delta"
        );

        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of(participation));

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertEquals(1, result.size());

        PlayerMatchSummaryDto dto = result.get(0);
        assertEquals(PlayerMatchRoleDto.PARTICIPANT, dto.roleJoueur());
        assertEquals(MatchTemporalStatusDto.FUTUR, dto.statutTemporel());
        assertEquals(5, dto.joursAvantMatch());
        assertFalse(dto.paiementJoueurEffectue());
        assertEquals(MatchVisibilite.PUBLIC, dto.visibilite());
        assertEquals(10L, dto.terrainId());
        assertEquals("Terrain 1", dto.terrainNom());
        assertEquals(1L, dto.siteId());
        assertEquals("Site Delta", dto.siteNom());
    }

    @Test
    void getPlayerMatches_matchFutur_organisateur_paye() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        Participation participation = mockParticipation(
                "G0001",                // organisateur = joueur courant
                "G0001",
                LocalDate.now().plusDays(3),
                MatchVisibilite.PRIVE,
                true,
                20L,
                "Terrain Central",
                2L,
                "Site Omega"
        );

        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of(participation));

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertEquals(1, result.size());

        PlayerMatchSummaryDto dto = result.get(0);
        assertEquals(PlayerMatchRoleDto.ORGANISATEUR, dto.roleJoueur());
        assertEquals(MatchTemporalStatusDto.FUTUR, dto.statutTemporel());
        assertEquals(3, dto.joursAvantMatch());
        assertTrue(dto.paiementJoueurEffectue());
        assertEquals(MatchVisibilite.PRIVE, dto.visibilite());
        assertEquals(20L, dto.terrainId());
        assertEquals("Terrain Central", dto.terrainNom());
        assertEquals(2L, dto.siteId());
        assertEquals("Site Omega", dto.siteNom());
    }

    @Test
    void getPlayerMatches_matchAujourdHui_retourneStatutAujourdHui_et_joursNull() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        Participation participation = mockParticipation(
                "G9999",
                "G0001",
                LocalDate.now(),
                MatchVisibilite.PUBLIC,
                true,
                30L,
                "Terrain 3",
                3L,
                "Site Today"
        );

        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of(participation));

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertEquals(1, result.size());

        PlayerMatchSummaryDto dto = result.get(0);
        assertEquals(MatchTemporalStatusDto.AUJOURD_HUI, dto.statutTemporel());
        assertNull(dto.joursAvantMatch());
        assertTrue(dto.paiementJoueurEffectue());
    }

    @Test
    void getPlayerMatches_matchPasse_retourneStatutPasse_et_joursNull() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        Participation participation = mockParticipation(
                "G9999",
                "G0001",
                LocalDate.now().minusDays(2),
                MatchVisibilite.PRIVE,
                false,
                40L,
                "Terrain 4",
                4L,
                "Site Past"
        );

        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of(participation));

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertEquals(1, result.size());

        PlayerMatchSummaryDto dto = result.get(0);
        assertEquals(MatchTemporalStatusDto.PASSE, dto.statutTemporel());
        assertNull(dto.joursAvantMatch());
        assertFalse(dto.paiementJoueurEffectue());
        assertEquals(MatchVisibilite.PRIVE, dto.visibilite());
    }

    @Test
    void getPlayerMatches_plusieursParticipations_conserveOrdreDuRepository() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        Participation first = mockParticipation(
                "G9999",
                "G0001",
                LocalDate.now().plusDays(1),
                MatchVisibilite.PUBLIC,
                false,
                11L,
                "Terrain A",
                101L,
                "Site A"
        );

        Participation second = mockParticipation(
                "G0001",
                "G0001",
                LocalDate.now().plusDays(4),
                MatchVisibilite.PRIVE,
                true,
                22L,
                "Terrain B",
                202L,
                "Site B"
        );

        when(participationRepo.findByJoueur_MatriculeOrderByMatch_DateDebutAsc("G0001"))
                .thenReturn(List.of(first, second));

        List<PlayerMatchSummaryDto> result = service.getPlayerMatches("G0001");

        assertEquals(2, result.size());

        assertEquals(11L, result.get(0).terrainId());
        assertEquals("Terrain A", result.get(0).terrainNom());
        assertEquals(PlayerMatchRoleDto.PARTICIPANT, result.get(0).roleJoueur());
        assertFalse(result.get(0).paiementJoueurEffectue());

        assertEquals(22L, result.get(1).terrainId());
        assertEquals("Terrain B", result.get(1).terrainNom());
        assertEquals(PlayerMatchRoleDto.ORGANISATEUR, result.get(1).roleJoueur());
        assertTrue(result.get(1).paiementJoueurEffectue());
    }
    // ----------------
// getOrganizedMatches
// ----------------

    @Test
    void getOrganizedMatches_joueurIntrouvable_notFound() {
        when(joueurRepo.findById("G0001")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getOrganizedMatches("G0001"));

        verify(joueurRepo).findById("G0001");
        verifyNoInteractions(matchPadelRepo);
    }

    @Test
    void getOrganizedMatches_joueurExistant_sansMatchOrganise_retourneListeVide() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));
        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of());

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(joueurRepo).findById("G0001");
        verify(matchPadelRepo).findOrganizedMatchesWithDetailsByMatricule("G0001");
    }

    @Test
    void getOrganizedMatches_matchFuturPriveIncomplet_risquePenaliteTrue() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel match = mockOrganizedMatch(
                LocalDate.now().plusDays(1),
                MatchVisibilite.PRIVE,
                2,
                1L,
                10L,
                "Terrain 1",
                100L,
                "Site Delta"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(match));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        assertEquals(1, result.size());

        OrganizerMatchSummaryDto dto = result.get(0);
        assertEquals(1L, dto.id());
        assertEquals(10L, dto.terrainId());
        assertEquals("Terrain 1", dto.terrainNom());
        assertEquals(100L, dto.siteId());
        assertEquals("Site Delta", dto.siteNom());
        assertEquals(MatchVisibilite.PRIVE, dto.visibilite());
        assertEquals(2, dto.nbParticipants());
        assertEquals(2, dto.placesRestantes());
        assertFalse(dto.complet());
        assertEquals(MatchTemporalStatusDto.FUTUR, dto.statutTemporel());
        assertEquals(1, dto.joursAvantMatch());
        assertTrue(dto.risquePenaliteJ1());
    }

    @Test
    void getOrganizedMatches_matchFuturPublicIncomplet_risquePenaliteFalse() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel match = mockOrganizedMatch(
                LocalDate.now().plusDays(1),
                MatchVisibilite.PUBLIC,
                2,
                2L,
                20L,
                "Terrain 2",
                200L,
                "Site Omega"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(match));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        OrganizerMatchSummaryDto dto = result.get(0);
        assertEquals(MatchVisibilite.PUBLIC, dto.visibilite());
        assertEquals(2, dto.nbParticipants());
        assertEquals(2, dto.placesRestantes());
        assertFalse(dto.complet());
        assertEquals(MatchTemporalStatusDto.FUTUR, dto.statutTemporel());
        assertEquals(1, dto.joursAvantMatch());
        assertFalse(dto.risquePenaliteJ1());
    }

    @Test
    void getOrganizedMatches_matchComplet_placesRestantesZero_et_pasDeRisque() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel match = mockOrganizedMatch(
                LocalDate.now().plusDays(3),
                MatchVisibilite.PRIVE,
                4,
                3L,
                30L,
                "Terrain 3",
                300L,
                "Site Full"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(match));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        OrganizerMatchSummaryDto dto = result.get(0);
        assertEquals(4, dto.nbParticipants());
        assertEquals(0, dto.placesRestantes());
        assertTrue(dto.complet());
        assertEquals(MatchTemporalStatusDto.FUTUR, dto.statutTemporel());
        assertEquals(3, dto.joursAvantMatch());
        assertFalse(dto.risquePenaliteJ1());
    }

    @Test
    void getOrganizedMatches_matchAujourdHui_statutAujourdHui_et_joursNull() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel match = mockOrganizedMatch(
                LocalDate.now(),
                MatchVisibilite.PRIVE,
                3,
                4L,
                40L,
                "Terrain 4",
                400L,
                "Site Today"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(match));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        OrganizerMatchSummaryDto dto = result.get(0);
        assertEquals(MatchTemporalStatusDto.AUJOURD_HUI, dto.statutTemporel());
        assertNull(dto.joursAvantMatch());
        assertFalse(dto.risquePenaliteJ1());
    }

    @Test
    void getOrganizedMatches_matchPasse_statutPasse_et_joursNull() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel match = mockOrganizedMatch(
                LocalDate.now().minusDays(2),
                MatchVisibilite.PRIVE,
                1,
                5L,
                50L,
                "Terrain 5",
                500L,
                "Site Past"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(match));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        OrganizerMatchSummaryDto dto = result.get(0);
        assertEquals(MatchTemporalStatusDto.PASSE, dto.statutTemporel());
        assertNull(dto.joursAvantMatch());
        assertFalse(dto.risquePenaliteJ1());
    }

    @Test
    void getOrganizedMatches_plusieursMatchs_conserveOrdreDuRepository() {
        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G0001")).thenReturn(Optional.of(joueur));

        MatchPadel first = mockOrganizedMatch(
                LocalDate.now().plusDays(1),
                MatchVisibilite.PUBLIC,
                1,
                10L,
                101L,
                "Terrain A",
                1001L,
                "Site A"
        );

        MatchPadel second = mockOrganizedMatch(
                LocalDate.now().plusDays(4),
                MatchVisibilite.PRIVE,
                3,
                20L,
                202L,
                "Terrain B",
                2002L,
                "Site B"
        );

        when(matchPadelRepo.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(first, second));

        List<OrganizerMatchSummaryDto> result = service.getOrganizedMatches("G0001");

        assertEquals(2, result.size());

        assertEquals(10L, result.get(0).id());
        assertEquals(101L, result.get(0).terrainId());
        assertEquals("Terrain A", result.get(0).terrainNom());

        assertEquals(20L, result.get(1).id());
        assertEquals(202L, result.get(1).terrainId());
        assertEquals("Terrain B", result.get(1).terrainNom());
    }
}