package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
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
import java.time.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementDebutMatchServiceTest {

    @Mock MatchPadelRepository matchPadelRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock
    SoldeService soldeService;

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

        when(matchPadelRepository.findAtraiterDebutMatchAvecDetails(any(), any()))
                .thenReturn(List.of(match));
        when(matchPadelRepository.save(any(MatchPadel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // total payé = 30 => solde = 30
        when(paiementRepository.sumMontantByMatchId(55L)).thenReturn(new BigDecimal("30.00"));

        int treated = service.traiterDebutMatchFenetreMinutes(5);

        assertThat(treated).isEqualTo(1);
        verify(soldeService).debiter(eq("O1"), eq(new BigDecimal("30.00")));

        assertThat(match.getSoldeTraiteLe()).isEqualTo(now);
    }

    private static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}