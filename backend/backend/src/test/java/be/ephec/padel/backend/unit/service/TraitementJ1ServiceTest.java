package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.service.SoldeService;
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
    @Mock ParticipationRepository participationRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock
    SoldeService soldeService;

    private Clock clock;
    private TraitementJ1Service service;

    @BeforeEach
    void setUp() {
        // now = 2026-02-27 10:00 (Europe/Brussels)
        ZoneId zone = ZoneId.of("Europe/Brussels");
        Instant fixedInstant = LocalDateTime.of(2026, 2, 27, 10, 0)
                .atZone(zone).toInstant();

        clock = Clock.fixed(fixedInstant, zone);

        service = new TraitementJ1Service(
                matchPadelRepository,
                participationRepository,
                paiementRepository,
                soldeService,
                clock
        );
    }

    @Test
    void prive_incomplet_devient_public_penalite_sans_paiement_solde_orga_a_J1() {
        LocalDateTime now = LocalDateTime.now(clock);

        // Match PRIVÉ avec seulement 2 participants (orga + 1) => incomplet => devient PUBLIC + pénalité
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

        // Tout le monde est payé (>= 15) => pas d'impayé => pas de suppression
        when(paiementRepository.sumMontantByParticipationId(100L)).thenReturn(Tarifs.PART_PAR_JOUEUR);
        when(paiementRepository.sumMontantByParticipationId(101L)).thenReturn(Tarifs.PART_PAR_JOUEUR);

        // Act
        int treated = service.traiterJ1FenetreMinutes(5);

        // Assert
        assertThat(treated).isEqualTo(1);

        // devient PUBLIC
        assertThat(match.getVisibilite()).isEqualTo(MatchVisibilite.PUBLIC);

        // pénalité 1 semaine
        assertThat(orga.getPenaliteJusqua()).isEqualTo(now.plusWeeks(1));

        // ✅ IMPORTANT : pas de paiement "solde organisateur" ici,
        // car le match n'était pas PUBLIC avant le traitement J-1.
        verify(soldeService, never()).debiter(eq("O1"), any());
        verify(soldeService, never()).crediter(eq("O1"), any());
        verify(paiementRepository, never()).save(any());

        // flag idempotent
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

        // Orga payé, impayé = 0
        when(paiementRepository.sumMontantByParticipationId(200L)).thenReturn(new BigDecimal("15.00"));
        when(paiementRepository.sumMontantByParticipationId(201L)).thenReturn(new BigDecimal("0.00"));

        // Act
        int treated = service.traiterJ1FenetreMinutes(5);

        // Assert
        assertThat(treated).isEqualTo(1);

        // match passe PUBLIC
        assertThat(match.getVisibilite()).isEqualTo(MatchVisibilite.PUBLIC);

        // la participation impayée est retirée (place libérée)
        assertThat(match.getParticipations()).hasSize(1);
        assertThat(match.getParticipations().get(0).getJoueur().getMatricule()).isEqualTo("O1");

        // dette restante annulée = 15 - 0 = 15
        verify(soldeService).crediter(eq("JX"), eq(new BigDecimal("15.00")));

        // pas de paiement solde orga à J-1 dans ce scénario non plus
        verify(soldeService, never()).debiter(eq("O1"), any());
        verify(soldeService, never()).crediter(eq("O1"), any());
        verify(paiementRepository, never()).save(any());

        // flag idempotent
        assertThat(match.getJ1TraiteLe()).isEqualTo(now);
    }

    // ---- helpers (IDs JPA) ----
    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}