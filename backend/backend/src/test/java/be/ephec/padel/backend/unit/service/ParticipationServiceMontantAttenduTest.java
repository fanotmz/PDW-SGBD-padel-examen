package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.service.PaiementService;
import be.ephec.padel.backend.service.ParticipationService;
import be.ephec.padel.backend.service.SoldeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceMontantAttenduTest {

    @Mock
    ParticipationRepository participationRepository;
    @Mock
    MatchPadelRepository matchPadelRepository;
    @Mock
    JoueurRepository joueurRepository;
    @Mock
    SoldeService soldeService;
    @Mock
    PaiementService paiementService;

    private ParticipationService participationService;

    @BeforeEach
    void setUp() {
        participationService = new ParticipationService(
                participationRepository,
                matchPadelRepository,
                joueurRepository,
                soldeService,
                paiementService
        );
    }

    @Test
    void montantAttendu_public_sansDette_retourne15() {
        Long matchId = 1L;
        String mat = "J1";

        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = new Joueur(mat, "Nom", TypeJoueur.GLOBAL);

        when(matchPadelRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(joueurRepository.findById(mat)).thenReturn(Optional.of(joueur));

        BigDecimal montant = participationService.calculerMontantAttenduPourMatchPublic(matchId, mat);

        assertThat(montant).isEqualByComparingTo(Tarifs.PART_PAR_JOUEUR.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void montantAttendu_public_avecDette15_retourne30() {
        Long matchId = 2L;
        String mat = "J2";

        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = new Joueur(mat, "Nom", TypeJoueur.GLOBAL);
        joueur.setSolde(new BigDecimal("15.00"));

        when(matchPadelRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(joueurRepository.findById(mat)).thenReturn(Optional.of(joueur));

        BigDecimal montant = participationService.calculerMontantAttenduPourMatchPublic(matchId, mat);

        assertThat(montant).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void montantAttendu_matchPrive_lanceBusinessException() {
        Long matchId = 3L;
        String mat = "J3";

        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);

        when(matchPadelRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> participationService.calculerMontantAttenduPourMatchPublic(matchId, mat))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Match prive");

        verifyNoInteractions(joueurRepository);
    }
}
