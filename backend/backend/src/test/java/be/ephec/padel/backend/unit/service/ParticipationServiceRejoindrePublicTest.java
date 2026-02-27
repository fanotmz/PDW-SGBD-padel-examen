package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceRejoindrePublicTest {

    @Mock ParticipationRepository participationRepository;
    @Mock MatchPadelRepository matchPadelRepository;
    @Mock JoueurRepository joueurRepository;
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
    void rejoindrePublic_avecDette15_paie30_et_debite15() {
        Long matchId = 10L;
        String mat = "J1";

        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);
        match.setDateDebut(LocalDateTime.now());
        // pas de participations => place dispo

        Joueur joueur = new Joueur(mat, "Nom", TypeJoueur.GLOBAL);
        joueur.setSolde(new BigDecimal("15.00")); // dette existante

        when(matchPadelRepository.findByIdForUpdateWithParticipations(matchId))
                .thenReturn(Optional.of(match));
        when(joueurRepository.findById(mat))
                .thenReturn(Optional.of(joueur));

        when(participationRepository.existsByMatch_IdAndJoueur_Matricule(matchId, mat))
                .thenReturn(false);

        // Simule save() qui renvoie une Participation avec un id
        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(inv -> {
                    Participation p = inv.getArgument(0);
                    ReflectionTestUtils.setField(p, "id", 99L);
                    return p;
                });

        Participation saved = participationService.rejoindreEtPayerMatchPublic(matchId, mat);

        assertThat(saved).isNotNull();

        // debit de la part (15)
        verify(soldeService).debiter(eq(mat), eq(Tarifs.PART_PAR_JOUEUR));

        // paiement total = 15 + dette (15) = 30
        verify(paiementService).payerParticipationAvecRattrapageDette(eq(99L), eq(new BigDecimal("30.00")));
    }

    @Test
    void rejoindrePublic_matchDejaComplet_refuse() {
        Long matchId = 11L;
        String mat = "J2";

        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        // 4 participations => complet
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());

        when(matchPadelRepository.findByIdForUpdateWithParticipations(matchId))
                .thenReturn(Optional.of(match));

        // ✅ le joueur doit exister sinon NotFoundException avant d'arriver à "complet"
        when(joueurRepository.findById(mat))
                .thenReturn(Optional.of(new Joueur(mat, "Nom", TypeJoueur.GLOBAL)));

        // ✅ éviter de tomber sur "déjà inscrit" (selon ton ordre de vérifs)
        when(participationRepository.existsByMatch_IdAndJoueur_Matricule(matchId, mat))
                .thenReturn(false);

        assertThatThrownBy(() -> participationService.rejoindreEtPayerMatchPublic(matchId, mat))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Match déjà complet");

        verifyNoInteractions(paiementService);
        verifyNoInteractions(soldeService);
    }
}