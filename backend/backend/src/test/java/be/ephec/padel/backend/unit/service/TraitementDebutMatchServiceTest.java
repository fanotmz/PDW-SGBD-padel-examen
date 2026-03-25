package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.service.SoldeService;
import be.ephec.padel.backend.service.TraitementDebutMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraitementDebutMatchServiceTest {

    @Mock MatchPadelRepository matchPadelRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock SoldeService soldeService;

    private Clock clock;
    private TraitementDebutMatchService service;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Europe/Brussels");
        Instant fixedInstant = LocalDateTime.of(2026, 2, 27, 10, 0).atZone(zone).toInstant();
        clock = Clock.fixed(fixedInstant, zone);

        service = new TraitementDebutMatchService(matchPadelRepository, paiementRepository, soldeService, clock);
    }

    @Test
    void debut_match_incomplet_cree_dette_sur_organisateur() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.minusMinutes(1), MatchVisibilite.PUBLIC);
        setId(match, 55L);

        Joueur orga = new Joueur("O1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        Participation p1 = new Participation(match, orga);
        Participation p2 = new Participation(match, new Joueur("J2", "Joueur 2", TypeJoueur.GLOBAL));
        setId(p1, 100L);
        setId(p2, 101L);
        match.addParticipation(p1);
        match.addParticipation(p2);

        when(matchPadelRepository.findAtraiterDebutMatchAvecDetails(any(), any()))
                .thenReturn(List.of(match));
        when(matchPadelRepository.save(any(MatchPadel.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.sumMontantByParticipationIdAndType(100L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("15.00"));
        when(paiementRepository.sumMontantByParticipationIdAndType(101L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("15.00"));

        int treated = service.traiterDebutMatchFenetreMinutes(5);

        assertThat(treated).isEqualTo(1);
        verify(soldeService).debiter(eq("O1"), eq(new BigDecimal("30.00")));
        assertThat(match.getSoldeTraiteLe()).isEqualTo(now);
    }

    @Test
    void match_annule_ignore_meme_si_retourne_par_le_repository() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.minusMinutes(1), MatchVisibilite.PUBLIC);
        setId(match, 56L);
        match.setStatut(MatchStatut.ANNULE);

        when(matchPadelRepository.findAtraiterDebutMatchAvecDetails(any(), any()))
                .thenReturn(List.of(match));

        int treated = service.traiterDebutMatchFenetreMinutes(5);

        assertThat(treated).isZero();
        assertThat(match.getSoldeTraiteLe()).isNull();

        verifyNoInteractions(soldeService);
        verify(matchPadelRepository, never()).save(any(MatchPadel.class));
        verifyNoInteractions(paiementRepository);
    }

    @Test
    void debut_match_ignore_le_rattrapage_de_dette_dans_le_total_paye_du_match() {
        LocalDateTime now = LocalDateTime.now(clock);

        MatchPadel match = new MatchPadel(null, null, now.minusMinutes(1), MatchVisibilite.PUBLIC);
        setId(match, 57L);

        Joueur orga = new Joueur("O1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        Participation p1 = new Participation(match, orga);
        Participation p2 = new Participation(match, new Joueur("J2", "Joueur 2", TypeJoueur.GLOBAL));
        setId(p1, 110L);
        setId(p2, 111L);
        match.addParticipation(p1);
        match.addParticipation(p2);

        when(matchPadelRepository.findAtraiterDebutMatchAvecDetails(any(), any()))
                .thenReturn(List.of(match));
        when(matchPadelRepository.save(any(MatchPadel.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(paiementRepository.sumMontantByParticipationIdAndType(110L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("25.00"));
        when(paiementRepository.sumMontantByParticipationIdAndType(111L, TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("10.00"));

        int treated = service.traiterDebutMatchFenetreMinutes(5);

        assertThat(treated).isEqualTo(1);
        verify(soldeService).debiter(eq("O1"), eq(new BigDecimal("35.00")));
    }

    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
