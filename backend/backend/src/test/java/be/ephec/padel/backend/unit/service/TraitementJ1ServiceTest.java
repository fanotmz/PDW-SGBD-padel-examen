package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.service.SoldeOriginContext;
import be.ephec.padel.backend.service.SoldeService;
import be.ephec.padel.backend.service.PenaliteJoueurService;
import be.ephec.padel.backend.service.TraitementJ1Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementJ1ServiceTest {

    @Mock MatchPadelRepository matchPadelRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock
    SoldeService soldeService;

    private Clock clock;
    private TraitementJ1Service service;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Europe/Brussels");
        Instant fixedInstant = LocalDateTime.of(2026, 2, 27, 10, 0)
                .atZone(zone).toInstant();

        clock = Clock.fixed(fixedInstant, zone);

        service = new TraitementJ1Service(
                matchPadelRepository,
                paiementRepository,
                soldeService,
                new PenaliteJoueurService(clock),
                clock
        );
    }

    @Test
    void prive_incomplet_devient_public_penalite_sans_paiement_solde_orga_a_J1() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.plusHours(24).plusMinutes(1), MatchVisibilite.PRIVE);
        setId(match, 10L);

        Joueur orga = new Joueur("O1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        Joueur j2 = new Joueur("J2", "Joueur2", TypeJoueur.GLOBAL);

        Participation pOrga = new Participation(match, orga);
        setId(pOrga, 100L);
        Participation pJ2 = new Participation(match, j2);
        setId(pJ2, 101L);

        match.addParticipation(pOrga);
        match.addParticipation(pJ2);

        when(matchPadelRepository.findAtraiterJ1AvecDetails(any(), any()))
                .thenReturn(List.of(match));
        when(matchPadelRepository.save(any(MatchPadel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(paiementRepository.sumMontantByParticipationId(100L)).thenReturn(Tarifs.PART_PAR_JOUEUR);
        when(paiementRepository.sumMontantByParticipationId(101L)).thenReturn(Tarifs.PART_PAR_JOUEUR);

        int treated = service.traiterJ1FenetreMinutes(5);

        assertThat(treated).isEqualTo(1);

        assertThat(match.getVisibilite()).isEqualTo(MatchVisibilite.PUBLIC);

        assertThat(orga.getPenaliteJusqua()).isEqualTo(
                now.toLocalDate().plusDays(7).atTime(LocalTime.MAX)
        );

        verify(soldeService, never()).debiter(eq("O1"), any());
        verify(soldeService, never()).crediter(eq("O1"), any());
        verify(paiementRepository, never()).save(any());

        assertThat(match.getJ1TraiteLe()).isEqualTo(now);
    }

    @Test
    void impaye_la_veille_place_liberee_match_public_et_dette_annulee() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.plusHours(24).plusMinutes(1), MatchVisibilite.PRIVE);
        setId(match, 20L);

        Joueur orga = new Joueur("O1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        Joueur impaye = new Joueur("JX", "Impayé", TypeJoueur.GLOBAL);

        Participation pOrga = new Participation(match, orga);
        setId(pOrga, 200L);
        Participation pImpayee = new Participation(match, impaye);
        setId(pImpayee, 201L);

        match.addParticipation(pOrga);
        match.addParticipation(pImpayee);

        when(matchPadelRepository.findAtraiterJ1AvecDetails(any(), any()))
                .thenReturn(List.of(match));
        when(matchPadelRepository.save(any(MatchPadel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(paiementRepository.sumMontantByParticipationId(200L)).thenReturn(new BigDecimal("15.00"));
        when(paiementRepository.sumMontantByParticipationId(201L)).thenReturn(new BigDecimal("0.00"));

        int treated = service.traiterJ1FenetreMinutes(5);

        assertThat(treated).isEqualTo(1);

        assertThat(match.getVisibilite()).isEqualTo(MatchVisibilite.PUBLIC);

        assertThat(match.getParticipations()).hasSize(1);
        assertThat(match.getParticipations().get(0).getJoueur().getMatricule()).isEqualTo("O1");

        verify(soldeService).crediter(
                eq("JX"),
                eq(new BigDecimal("15.00")),
                argThat((SoldeOriginContext context) ->
                        context.getOrigineType() == OrigineMouvementSoldeType.TRAITEMENT_J1_NEUTRALISATION
                                && Long.valueOf(201L).equals(context.getParticipationId())
                                && Long.valueOf(20L).equals(context.getMatchId())
                )
        );

        verify(soldeService, never()).debiter(eq("O1"), any());
        verify(soldeService, never()).crediter(eq("O1"), any());
        verify(paiementRepository, never()).save(any());

        assertThat(match.getJ1TraiteLe()).isEqualTo(now);
    }

    @Test
    void match_annule_ignore_meme_si_retourne_par_le_repository() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.plusHours(24).plusMinutes(1), MatchVisibilite.PRIVE);
        setId(match, 30L);
        match.setStatut(MatchStatut.ANNULE);

        when(matchPadelRepository.findAtraiterJ1AvecDetails(any(), any()))
                .thenReturn(List.of(match));

        int treated = service.traiterJ1FenetreMinutes(5);

        assertThat(treated).isZero();
        assertThat(match.getJ1TraiteLe()).isNull();

        verifyNoInteractions(soldeService);
        verify(matchPadelRepository, never()).save(any(MatchPadel.class));
        verifyNoInteractions(paiementRepository);
    }

    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
