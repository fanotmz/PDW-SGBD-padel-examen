package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.RegularisationsResponseDto;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.service.RegularisationService;
import be.ephec.padel.backend.service.SoldeOriginContext;
import be.ephec.padel.backend.service.SoldeImputationService;
import be.ephec.padel.backend.service.SoldeService;
import be.ephec.padel.backend.service.model.ImputationResult;
import be.ephec.padel.backend.service.model.OpenDebtLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegularisationServiceTest {

    private CurrentUserFacade currentUserFacade;
    private SoldeImputationService soldeImputationService;
    private ParticipationRepository participationRepository;
    private SoldeService soldeService;

    private RegularisationService service;
    private Joueur joueur;

    @BeforeEach
    void setUp() {
        currentUserFacade = mock(CurrentUserFacade.class);
        soldeImputationService = mock(SoldeImputationService.class);
        participationRepository = mock(ParticipationRepository.class);
        soldeService = mock(SoldeService.class);
        service = new RegularisationService(currentUserFacade, soldeImputationService, participationRepository, soldeService);

        joueur = new Joueur("G9001", "Joueur Global Demo", TypeJoueur.GLOBAL);
        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
    }

    @Test
    void aucuneDetteTracable_listeVide_totalZero() {
        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(), BigDecimal.ZERO));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.totalTracable()).isEqualByComparingTo("0.00");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void uneDetteOuverteTracable_uneLigneCorrectementMappee() {
        OpenDebtLine line = new OpenDebtLine(
                1L,
                10L,
                100L,
                OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                new BigDecimal("15.00"),
                false,
                null
        );
        line.imputer(new BigDecimal("5.00"));

        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(line), new BigDecimal("10.00")));

        Participation participation = participation(10L, 100L, joueur, MatchVisibilite.PRIVE, MatchStatut.PLANIFIE);
        when(participationRepository.findByIdInWithDetails(List.of(10L))).thenReturn(List.of(participation));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.totalTracable()).isEqualByComparingTo("10.00");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().participationId()).isEqualTo(10L);
        assertThat(result.items().getFirst().matchId()).isEqualTo(100L);
        assertThat(result.items().getFirst().siteNom()).isEqualTo("Site Nord");
        assertThat(result.items().getFirst().terrainNom()).isEqualTo("Terrain 1");
        assertThat(result.items().getFirst().roleJoueur()).isEqualTo(PlayerMatchRoleDto.ORGANISATEUR);
        assertThat(result.items().getFirst().montantInitial()).isEqualByComparingTo("15.00");
        assertThat(result.items().getFirst().montantDejaPaye()).isEqualByComparingTo("5.00");
        assertThat(result.items().getFirst().montantRestant()).isEqualByComparingTo("10.00");
        assertThat(result.items().getFirst().payable()).isTrue();
    }

    @Test
    void detteAnnulationTardiveSurMatchAnnule_restePayable() {
        OpenDebtLine line = new OpenDebtLine(
                1L,
                10L,
                100L,
                OrigineMouvementSoldeType.ANNULATION_TARDIVE_ORGANISATEUR,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                new BigDecimal("45.00"),
                false,
                null
        );

        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(line), new BigDecimal("45.00")));

        Participation participation = participation(10L, 100L, joueur, MatchVisibilite.PUBLIC, MatchStatut.ANNULE);
        when(participationRepository.findByIdInWithDetails(List.of(10L))).thenReturn(List.of(participation));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().origineType())
                .isEqualTo(OrigineMouvementSoldeType.ANNULATION_TARDIVE_ORGANISATEUR);
        assertThat(result.items().getFirst().payable()).isTrue();
    }

    @Test
    void payerAnnulationTardiveOrganisateur_crediteSoldeAvecOrigineCiblee() {
        Participation participation = participation(10L, 100L, joueur, MatchVisibilite.PUBLIC, MatchStatut.ANNULE);
        when(participationRepository.findByIdWithDetails(10L)).thenReturn(java.util.Optional.of(participation));
        when(soldeImputationService.getMontantOuvertPourParticipationEtOrigine(
                "G9001",
                10L,
                OrigineMouvementSoldeType.ANNULATION_TARDIVE_ORGANISATEUR
        )).thenReturn(new BigDecimal("45.00"));

        LocalDateTime penaliteAvant = LocalDateTime.of(2030, 2, 1, 23, 59);
        joueur.setPenaliteJusqua(penaliteAvant);

        service.payerAnnulationTardiveOrganisateur(10L, new BigDecimal("45.00"));

        org.mockito.ArgumentCaptor<SoldeOriginContext> captor =
                org.mockito.ArgumentCaptor.forClass(SoldeOriginContext.class);
        verify(soldeService).crediter(eq("G9001"), eq(new BigDecimal("45.00")), captor.capture());
        assertThat(captor.getValue().getOrigineType())
                .isEqualTo(OrigineMouvementSoldeType.REGULARISATION_ANNULATION_TARDIVE);
        assertThat(captor.getValue().getParticipationId()).isEqualTo(10L);
        assertThat(captor.getValue().getMatchId()).isEqualTo(100L);
        assertThat(joueur.getPenaliteJusqua()).isEqualTo(penaliteAvant);
    }

    @Test
    void payerAnnulationTardiveOrganisateur_refuseDetteAutreJoueur() {
        Participation participation = participation(10L, 100L, joueur, MatchVisibilite.PUBLIC, MatchStatut.ANNULE);
        participation.setJoueur(new Joueur("G9999", "Autre", TypeJoueur.GLOBAL));
        when(participationRepository.findByIdWithDetails(10L)).thenReturn(java.util.Optional.of(participation));

        assertThatThrownBy(() -> service.payerAnnulationTardiveOrganisateur(10L, new BigDecimal("45.00")))
                .isInstanceOf(be.ephec.padel.backend.exception.ForbiddenException.class);

        verify(soldeService, never()).crediter(eq("G9001"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void payerAnnulationTardiveOrganisateur_refuseMontantNulOuNegatif() {
        assertThatThrownBy(() -> service.payerAnnulationTardiveOrganisateur(10L, BigDecimal.ZERO))
                .isInstanceOf(be.ephec.padel.backend.exception.BusinessException.class)
                .hasMessageContaining("Montant invalide");

        assertThatThrownBy(() -> service.payerAnnulationTardiveOrganisateur(10L, new BigDecimal("-1.00")))
                .isInstanceOf(be.ephec.padel.backend.exception.BusinessException.class)
                .hasMessageContaining("Montant invalide");
    }

    @Test
    void payerAnnulationTardiveOrganisateur_refuseMontantSuperieurResteDu() {
        Participation participation = participation(10L, 100L, joueur, MatchVisibilite.PUBLIC, MatchStatut.ANNULE);
        when(participationRepository.findByIdWithDetails(10L)).thenReturn(java.util.Optional.of(participation));
        when(soldeImputationService.getMontantOuvertPourParticipationEtOrigine(
                "G9001",
                10L,
                OrigineMouvementSoldeType.ANNULATION_TARDIVE_ORGANISATEUR
        )).thenReturn(new BigDecimal("45.00"));

        assertThatThrownBy(() -> service.payerAnnulationTardiveOrganisateur(10L, new BigDecimal("45.01")))
                .isInstanceOf(be.ephec.padel.backend.exception.BusinessException.class)
                .hasMessageContaining("Montant trop élevé");

        verify(soldeService, never()).crediter(eq("G9001"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deuxDettesOuvertes_totalCorrect() {
        OpenDebtLine line1 = new OpenDebtLine(1L, 10L, 100L, OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE,
                LocalDateTime.of(2030, 1, 2, 10, 0), new BigDecimal("15.00"), false, null);
        OpenDebtLine line2 = new OpenDebtLine(2L, 20L, 200L, OrigineMouvementSoldeType.CREATION_MATCH_ORGANISATEUR,
                LocalDateTime.of(2030, 1, 3, 10, 0), new BigDecimal("15.00"), false, null);
        line2.imputer(new BigDecimal("3.00"));

        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(line1, line2), new BigDecimal("27.00")));

        Participation p1 = participation(10L, 100L, joueur, MatchVisibilite.PRIVE, MatchStatut.PLANIFIE);
        Participation p2 = participation(20L, 200L, new Joueur("S9001", "Autre", TypeJoueur.SITE), MatchVisibilite.PUBLIC, MatchStatut.PLANIFIE);
        when(participationRepository.findByIdInWithDetails(List.of(10L, 20L))).thenReturn(List.of(p1, p2));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.items()).hasSize(2);
        assertThat(result.totalTracable()).isEqualByComparingTo("27.00");
    }

    @Test
    void participationTotalementSoldee_aucuneLigne() {
        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(), BigDecimal.ZERO));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.items()).isEmpty();
        assertThat(result.totalTracable()).isEqualByComparingTo("0.00");
    }

    @Test
    void detteGlobaleNonTracableEtDetteCiblee_seuleDetteCibleeRetournee() {
        OpenDebtLine line = new OpenDebtLine(1L, 10L, 100L, OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE,
                LocalDateTime.of(2030, 1, 2, 10, 0), new BigDecimal("15.00"), false, "Dette traçable");

        when(soldeImputationService.reconstruirePourJoueur("G9001"))
                .thenReturn(new ImputationResult(List.of(line), new BigDecimal("15.00")));

        Participation p1 = participation(10L, 100L, joueur, MatchVisibilite.PRIVE, MatchStatut.PLANIFIE);
        when(participationRepository.findByIdInWithDetails(List.of(10L))).thenReturn(List.of(p1));

        RegularisationsResponseDto result = service.getCurrentUserRegularisations();

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalTracable()).isEqualByComparingTo("15.00");
    }

    private Participation participation(Long participationId,
                                        Long matchId,
                                        Joueur organisateur,
                                        MatchVisibilite visibilite,
                                        MatchStatut statut) {
        Site site = new Site("Site Nord", "Bruxelles");
        Terrain terrain = new Terrain("Terrain 1", site);
        MatchPadel match = new MatchPadel(terrain, organisateur, LocalDateTime.of(2030, 1, 10, 18, 0), visibilite);
        match.setStatut(statut);
        ReflectionTestUtils.setField(match, "id", matchId);

        Participation participation = new Participation(match, joueur);
        ReflectionTestUtils.setField(participation, "id", participationId);
        return participation;
    }
}
