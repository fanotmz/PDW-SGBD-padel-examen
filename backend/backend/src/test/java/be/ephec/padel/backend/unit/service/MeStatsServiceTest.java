package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.service.MeStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeStatsServiceTest {

    @Mock
    CurrentUserFacade currentUserFacade;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    MatchPadelRepository matchPadelRepository;
    @Mock
    PaiementRepository paiementRepository;

    private MeStatsService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2030-01-10T09:30:00Z"), ZoneOffset.UTC);
        service = new MeStatsService(
                currentUserFacade,
                participationRepository,
                matchPadelRepository,
                paiementRepository,
                fixedClock
        );
    }

    @Test
    void getCurrentUserStats_calcule_le_snapshot_nominal() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        joueur.setSolde(new BigDecimal("12.50"));

        LocalDateTime now = LocalDateTime.now(fixedClock);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.countByJoueur_Matricule("G0001")).thenReturn(5L);
        when(matchPadelRepository.countByOrganisateur_Matricule("G0001")).thenReturn(3L);
        when(matchPadelRepository.countDistinctLinkedPastMatchesByMatricule("G0001", now)).thenReturn(2L);
        when(matchPadelRepository.countDistinctLinkedFutureMatchesByMatricule("G0001", now)).thenReturn(4L);
        when(matchPadelRepository.countDistinctLinkedCancelledMatchesByMatricule("G0001")).thenReturn(1L);
        when(paiementRepository.sumMontantByParticipationJoueurMatriculeAndType("G0001", TypePaiement.ENCAISSEMENT))
                .thenReturn(new BigDecimal("42.00"));

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.getNbMatchsParticipes()).isEqualTo(5L);
        assertThat(dto.getNbMatchsOrganises()).isEqualTo(3L);
        assertThat(dto.getNbMatchsPasses()).isEqualTo(2L);
        assertThat(dto.getNbMatchsFuturs()).isEqualTo(4L);
        assertThat(dto.getNbMatchsAnnules()).isEqualTo(1L);
        assertThat(dto.getMontantTotalPaye()).isEqualByComparingTo("42.00");
        assertThat(dto.getDetteActuelle()).isEqualByComparingTo("12.50");

        verify(matchPadelRepository).countDistinctLinkedPastMatchesByMatricule("G0001", now);
        verify(matchPadelRepository).countDistinctLinkedFutureMatchesByMatricule("G0001", now);
        verify(paiementRepository).sumMontantByParticipationJoueurMatriculeAndType("G0001", TypePaiement.ENCAISSEMENT);
    }

    @Test
    void getCurrentUserStats_utilise_le_solde_courant_du_joueur() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        joueur.setSolde(new BigDecimal("7.25"));

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.countByJoueur_Matricule("G0001")).thenReturn(0L);
        when(matchPadelRepository.countByOrganisateur_Matricule("G0001")).thenReturn(0L);
        when(matchPadelRepository.countDistinctLinkedPastMatchesByMatricule(eq("G0001"), eq(LocalDateTime.now(fixedClock))))
                .thenReturn(0L);
        when(matchPadelRepository.countDistinctLinkedFutureMatchesByMatricule(eq("G0001"), eq(LocalDateTime.now(fixedClock))))
                .thenReturn(0L);
        when(matchPadelRepository.countDistinctLinkedCancelledMatchesByMatricule("G0001")).thenReturn(0L);
        when(paiementRepository.sumMontantByParticipationJoueurMatriculeAndType("G0001", TypePaiement.ENCAISSEMENT))
                .thenReturn(BigDecimal.ZERO);

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.getDetteActuelle()).isEqualByComparingTo("7.25");
    }

    @Test
    void getCurrentUserStats_passe_et_futur_sont_bases_sur_clock() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        LocalDateTime now = LocalDateTime.now(fixedClock);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.countByJoueur_Matricule("G0001")).thenReturn(0L);
        when(matchPadelRepository.countByOrganisateur_Matricule("G0001")).thenReturn(0L);
        when(matchPadelRepository.countDistinctLinkedPastMatchesByMatricule("G0001", now)).thenReturn(1L);
        when(matchPadelRepository.countDistinctLinkedFutureMatchesByMatricule("G0001", now)).thenReturn(2L);
        when(matchPadelRepository.countDistinctLinkedCancelledMatchesByMatricule("G0001")).thenReturn(0L);
        when(paiementRepository.sumMontantByParticipationJoueurMatriculeAndType("G0001", TypePaiement.ENCAISSEMENT))
                .thenReturn(BigDecimal.ZERO);

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.getNbMatchsPasses()).isEqualTo(1L);
        assertThat(dto.getNbMatchsFuturs()).isEqualTo(2L);
    }

    @Test
    void getCurrentUserStats_propage_forbidden_si_aucun_joueur_lie() {
        when(currentUserFacade.getCurrentJoueur())
                .thenThrow(new ForbiddenException("Aucun joueur lie a l'utilisateur authentifie."));

        assertThatThrownBy(() -> service.getCurrentUserStats())
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Aucun joueur lie a l'utilisateur authentifie.");
    }
}
