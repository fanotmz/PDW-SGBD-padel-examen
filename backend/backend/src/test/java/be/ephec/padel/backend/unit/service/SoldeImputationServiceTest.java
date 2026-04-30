package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MouvementSolde;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.TypeMouvement;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.service.SoldeImputationService;
import be.ephec.padel.backend.service.model.ImputationResult;
import be.ephec.padel.backend.service.model.OpenDebtLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoldeImputationServiceTest {

    private MouvementSoldeRepository mouvementSoldeRepository;
    private SoldeImputationService service;
    private Joueur joueur;

    @BeforeEach
    void setUp() {
        mouvementSoldeRepository = mock(MouvementSoldeRepository.class);
        service = new SoldeImputationService(mouvementSoldeRepository);
        joueur = new Joueur("J1", "Joueur 1", TypeJoueur.GLOBAL);
    }

    @Test
    void debitParticipationA15_aucunCredit_reste15() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1)));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("15.00");
        assertThat(result.getOpenDebtLines()).hasSize(1);
        assertThat(result.getOpenDebtLines().getFirst().getMontantRestant()).isEqualByComparingTo("15.00");
    }

    @Test
    void debitParticipationA15_creditA10_reste5() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 100L, 10L, "10.00", OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("5.00");
        assertThat(result.getOpenDebtLines()).hasSize(1);
        OpenDebtLine line = result.getOpenDebtLines().getFirst();
        assertThat(line.getMontantImpute()).isEqualByComparingTo("10.00");
        assertThat(line.getMontantRestant()).isEqualByComparingTo("5.00");
    }

    @Test
    void debitParticipationA15_creditA15_reste0() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 100L, 10L, "15.00", OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getOpenDebtLines()).isEmpty();
    }

    @Test
    void debitA15_debitB15_creditA15_A_soldee_B_reste15() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        debit(2L, 200L, 20L, "15.00", OrigineMouvementSoldeType.CREATION_MATCH_ORGANISATEUR, 2),
                        credit(3L, 100L, 10L, "15.00", OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION, 3)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("15.00");
        assertThat(result.getOpenDebtLines()).hasSize(1);
        assertThat(result.getOpenDebtLines().getFirst().getParticipationId()).isEqualTo(200L);
        assertThat(result.getOpenDebtLines().getFirst().getMontantRestant()).isEqualByComparingTo("15.00");
    }

    @Test
    void neutralisationJ1_surA_A_soldee() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 100L, 10L, "15.00", OrigineMouvementSoldeType.TRAITEMENT_J1_NEUTRALISATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getOpenDebtLines()).isEmpty();
        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void neutralisationAnnulation_surA_A_soldee() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 100L, 10L, "15.00", OrigineMouvementSoldeType.ANNULATION_MATCH_NEUTRALISATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getOpenDebtLines()).isEmpty();
        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void creditLegacy_ne_solde_pas_dette_ciblee() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 100L, 10L, "15.00", OrigineMouvementSoldeType.LEGACY, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getOpenDebtLines()).hasSize(1);
        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void creditParticipationDifferente_ne_solde_pas_autre_participation() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 100L, 10L, "15.00", OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE, 1),
                        credit(2L, 200L, 20L, "15.00", OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getOpenDebtLines()).hasSize(1);
        assertThat(result.getOpenDebtLines().getFirst().getParticipationId()).isEqualTo(100L);
        assertThat(result.getOpenDebtLines().getFirst().getMontantRestant()).isEqualByComparingTo("15.00");
    }

    @Test
    void rejoindrePublic_avecPaiementImmediat_ne_laisse_aucune_dette_tracable_sur_la_participation() {
        when(mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementAscIdAsc("J1"))
                .thenReturn(List.of(
                        debit(1L, 300L, 30L, "15.00", OrigineMouvementSoldeType.REJOINDRE_MATCH_PUBLIC_PART, 1),
                        credit(2L, 300L, 30L, "15.00", OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION, 2)
                ));

        ImputationResult result = service.reconstruirePourJoueur("J1");

        assertThat(result.getOpenDebtLines()).isEmpty();
        assertThat(result.getTotalTrackedOpenAmount()).isEqualByComparingTo("0.00");
    }

    private MouvementSolde debit(Long id,
                                 Long participationId,
                                 Long matchId,
                                 String montant,
                                 OrigineMouvementSoldeType origine,
                                 int dayOffset) {
        return mouvement(id, participationId, matchId, montant, TypeMouvement.DEBIT, origine, dayOffset);
    }

    private MouvementSolde credit(Long id,
                                  Long participationId,
                                  Long matchId,
                                  String montant,
                                  OrigineMouvementSoldeType origine,
                                  int dayOffset) {
        return mouvement(id, participationId, matchId, montant, TypeMouvement.CREDIT, origine, dayOffset);
    }

    private MouvementSolde mouvement(Long id,
                                     Long participationId,
                                     Long matchId,
                                     String montant,
                                     TypeMouvement type,
                                     OrigineMouvementSoldeType origine,
                                     int dayOffset) {
        MouvementSolde mouvement = new MouvementSolde(
                LocalDateTime.of(2030, 1, 1, 10, 0).plusDays(dayOffset),
                new BigDecimal(montant),
                type,
                joueur,
                participationId,
                matchId,
                origine,
                null
        );
        ReflectionTestUtils.setField(mouvement, "id", id);
        return mouvement;
    }
}
