package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.service.SoldeOriginContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnnulationMatchServiceTest {

    private MatchPadelRepository matchPadelRepository;
    private PaiementRepository paiementRepository;
    private PaiementService paiementService;
    private SoldeService soldeService;

    private AnnulationMatchService service;

    @BeforeEach
    void setUp() {
        matchPadelRepository = mock(MatchPadelRepository.class);
        paiementRepository = mock(PaiementRepository.class);
        paiementService = mock(PaiementService.class);
        soldeService = mock(SoldeService.class);

        Clock clock = Clock.fixed(Instant.parse("2030-01-01T09:00:00Z"), ZoneOffset.UTC);
        service = new AnnulationMatchService(
                matchPadelRepository,
                paiementRepository,
                paiementService,
                soldeService,
                clock
        );
    }

    @Test
    void annulerMatchSiPlanifie_matchIntrouvable_notFound() {
        when(matchPadelRepository.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.annulerMatchSiPlanifie(1L));
    }

    @Test
    void annulerMatchSiPlanifie_matchDejaAnnule_neFaitRien() {
        MatchPadel match = createMatch(LocalDateTime.of(2030, 1, 1, 10, 0));
        match.setStatut(MatchStatut.ANNULE);

        when(matchPadelRepository.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));

        boolean result = service.annulerMatchSiPlanifie(1L);

        assertFalse(result);
        verifyNoInteractions(paiementRepository, paiementService, soldeService);
        verify(matchPadelRepository, never()).save(any(MatchPadel.class));
    }

    @Test
    void annulerMatchSiPlanifie_matchDejaCommence_neFaitRien() {
        MatchPadel match = createMatch(LocalDateTime.of(2030, 1, 1, 9, 0));

        when(matchPadelRepository.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));

        boolean result = service.annulerMatchSiPlanifie(1L);

        assertFalse(result);
        verifyNoInteractions(paiementRepository, paiementService, soldeService);
        verify(matchPadelRepository, never()).save(any(MatchPadel.class));
    }

    @Test
    void annulerMatchSiPlanifie_paiementPartiel_annuleDetteRestanteEtRembourseLePaye() {
        MatchPadel match = createMatch(LocalDateTime.of(2030, 1, 1, 10, 0));
        Joueur joueur = createJoueur("J001", new BigDecimal("5.00"));
        Participation participation = new Participation(match, joueur);
        ReflectionTestUtils.setField(participation, "id", 11L);
        match.addParticipation(participation);

        when(matchPadelRepository.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(paiementRepository.sumMontantByParticipationId(11L)).thenReturn(new BigDecimal("10.00"));

        boolean result = service.annulerMatchSiPlanifie(1L);

        assertTrue(result);
        assertTrue(match.getStatut() == MatchStatut.ANNULE);
        verify(soldeService).crediter(
                org.mockito.ArgumentMatchers.eq("J001"),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("5.00")),
                argThat((SoldeOriginContext context) ->
                        context.getOrigineType() == OrigineMouvementSoldeType.ANNULATION_MATCH_NEUTRALISATION
                                && Long.valueOf(11L).equals(context.getParticipationId())
                                && Long.valueOf(1L).equals(context.getMatchId())
                )
        );
        verify(paiementService).enregistrerRemboursementAnnulation(participation, new BigDecimal("10.00"));
        verify(matchPadelRepository).save(match);
    }

    @Test
    void annulerMatchSiPlanifie_remboursementPlafonneAuCoutDuMatch() {
        MatchPadel match = createMatch(LocalDateTime.of(2030, 1, 1, 10, 0));
        Joueur joueur = createJoueur("J001", BigDecimal.ZERO);
        Participation participation = new Participation(match, joueur);
        ReflectionTestUtils.setField(participation, "id", 12L);
        match.addParticipation(participation);

        when(matchPadelRepository.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(paiementRepository.sumMontantByParticipationId(12L)).thenReturn(new BigDecimal("20.00"));

        boolean result = service.annulerMatchSiPlanifie(1L);

        assertTrue(result);
        verifyNoInteractions(soldeService);
        verify(paiementService).enregistrerRemboursementAnnulation(participation, new BigDecimal("15.00"));
        verify(matchPadelRepository).save(match);
    }

    @Test
    void annulerMatchsFutursPlanifiesSite_annuleTousLesMatchsRetournes() {
        MatchPadel match1 = createMatch(LocalDateTime.of(2030, 1, 1, 10, 0));
        MatchPadel match2 = createMatch(LocalDateTime.of(2030, 1, 1, 11, 0));
        ReflectionTestUtils.setField(match1, "id", 101L);
        ReflectionTestUtils.setField(match2, "id", 102L);

        when(matchPadelRepository.findPlannedFutureMatchesBySiteIdAndDateDebutBetween(
                55L,
                MatchStatut.PLANIFIE,
                LocalDateTime.of(2030, 1, 1, 9, 0),
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 2, 0, 0)
        )).thenReturn(List.of(match1, match2));

        when(matchPadelRepository.findByIdForUpdateWithParticipations(101L)).thenReturn(Optional.of(match1));
        when(matchPadelRepository.findByIdForUpdateWithParticipations(102L)).thenReturn(Optional.of(match2));

        int result = service.annulerMatchsFutursPlanifiesSite(
                55L,
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 2, 0, 0)
        );

        assertEquals(2, result);
        verify(matchPadelRepository).save(match1);
        verify(matchPadelRepository).save(match2);
    }

    private MatchPadel createMatch(LocalDateTime dateDebut) {
        Site site = new Site("Site A", "Bruxelles");
        Terrain terrain = new Terrain("T1", site);
        Joueur organisateur = createJoueur("ORG1", BigDecimal.ZERO);
        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, MatchVisibilite.PUBLIC);
        ReflectionTestUtils.setField(match, "id", 1L);
        return match;
    }

    private Joueur createJoueur(String matricule, BigDecimal solde) {
        Joueur joueur = new Joueur(matricule, "Nom", TypeJoueur.GLOBAL);
        joueur.setSolde(solde);
        return joueur;
    }
}
