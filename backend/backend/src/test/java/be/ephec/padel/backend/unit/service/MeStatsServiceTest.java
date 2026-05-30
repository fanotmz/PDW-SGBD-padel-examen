package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.MatchPadelRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private MeStatsService service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2030-01-10T09:30:00Z"), ZoneOffset.UTC);
        service = new MeStatsService(
                currentUserFacade,
                participationRepository,
                matchPadelRepository,
                fixedClock
        );
    }

    @Test
    void getCurrentUserStats_calcule_les_statistiques_personnelles() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        Joueur autreJoueur = new Joueur("G0002", "Bob", TypeJoueur.GLOBAL);
        LocalDateTime now = LocalDateTime.now(fixedClock);

        MatchPadel organisePasse = match(joueur, now.minusDays(2), MatchStatut.PLANIFIE);
        MatchPadel organiseFutur = match(joueur, now.plusDays(1), MatchStatut.PLANIFIE);
        MatchPadel organiseAnnule = match(joueur, now.plusDays(3), MatchStatut.ANNULE);

        MatchPadel participantPasse = match(autreJoueur, now.minusDays(3), MatchStatut.PLANIFIE);
        MatchPadel participantFutur = match(autreJoueur, now.plusHours(4), MatchStatut.PLANIFIE);
        MatchPadel participantAnnule = match(autreJoueur, now.plusDays(4), MatchStatut.ANNULE);

        Participation participationOrganisateur = participation(organiseFutur, joueur, new BigDecimal("15.00"));
        Participation participationPayee = participation(participantPasse, joueur, new BigDecimal("15.00"));
        Participation participationAPayer = participation(participantFutur, joueur, new BigDecimal("5.00"));
        Participation participationAnnulee = participation(participantAnnule, joueur, new BigDecimal("15.00"), new BigDecimal("-15.00"));

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.findByJoueurMatriculeWithStatsDetails("G0001"))
                .thenReturn(List.of(
                        participationOrganisateur,
                        participationPayee,
                        participationAPayer,
                        participationAnnulee
                ));
        when(matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(organisePasse, organiseFutur, organiseAnnule));

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.prochainMatch()).isNotNull();
        assertThat(dto.prochainMatch().dateDebut()).isEqualTo(now.plusHours(4));
        assertThat(dto.prochainMatch().roleJoueur()).isEqualTo(PlayerMatchRoleDto.PARTICIPANT);

        assertThat(dto.matchsCommeOrganisateur().joues()).isEqualTo(1L);
        assertThat(dto.matchsCommeOrganisateur().aVenir()).isEqualTo(1L);
        assertThat(dto.matchsCommeOrganisateur().annules()).isEqualTo(1L);

        assertThat(dto.matchsCommeParticipant().joues()).isEqualTo(1L);
        assertThat(dto.matchsCommeParticipant().aVenir()).isEqualTo(1L);
        assertThat(dto.matchsCommeParticipant().annules()).isEqualTo(1L);

        assertThat(dto.paiements().participationsPayees()).isEqualTo(2L);
        assertThat(dto.paiements().participationsAPayer()).isEqualTo(1L);
        assertThat(dto.paiements().montantNetPaye()).isEqualByComparingTo("35.00");
        assertThat(dto.paiements().montantRembourse()).isEqualByComparingTo("15.00");

        verify(participationRepository).findByJoueurMatriculeWithStatsDetails("G0001");
        verify(matchPadelRepository).findOrganizedMatchesWithDetailsByMatricule("G0001");
    }

    @Test
    void getCurrentUserStats_retourne_null_si_aucun_prochain_match() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        LocalDateTime now = LocalDateTime.now(fixedClock);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.findByJoueurMatriculeWithStatsDetails("G0001"))
                .thenReturn(List.of(participation(match(new Joueur("G0002", "Bob", TypeJoueur.GLOBAL), now.minusDays(1), MatchStatut.PLANIFIE), joueur)));
        when(matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule("G0001")).thenReturn(List.of());

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.prochainMatch()).isNull();
    }

    @Test
    void getCurrentUserStats_ne_compte_pas_l_organisateur_comme_participant() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        LocalDateTime now = LocalDateTime.now(fixedClock);
        MatchPadel matchOrganise = match(joueur, now.minusDays(1), MatchStatut.PLANIFIE);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.findByJoueurMatriculeWithStatsDetails("G0001"))
                .thenReturn(List.of(participation(matchOrganise, joueur)));
        when(matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of(matchOrganise));

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.matchsCommeOrganisateur().joues()).isEqualTo(1L);
        assertThat(dto.matchsCommeParticipant().joues()).isZero();
    }

    @Test
    void getCurrentUserStats_plafonne_le_montant_net_aux_participations_non_annulees() {
        Joueur joueur = new Joueur("G0001", "Alice", TypeJoueur.GLOBAL);
        Joueur autreJoueur = new Joueur("G0002", "Bob", TypeJoueur.GLOBAL);
        LocalDateTime now = LocalDateTime.now(fixedClock);

        MatchPadel participationNonAnnulee = match(autreJoueur, now.minusDays(1), MatchStatut.PLANIFIE);
        MatchPadel participationAnnulee = match(autreJoueur, now.plusDays(1), MatchStatut.ANNULE);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        when(participationRepository.findByJoueurMatriculeWithStatsDetails("G0001"))
                .thenReturn(List.of(
                        participation(participationNonAnnulee, joueur, new BigDecimal("45.00")),
                        participation(participationAnnulee, joueur, new BigDecimal("15.00"), new BigDecimal("-15.00"))
                ));
        when(matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule("G0001"))
                .thenReturn(List.of());

        MeStatsDto dto = service.getCurrentUserStats();

        assertThat(dto.paiements().participationsPayees()).isEqualTo(1L);
        assertThat(dto.paiements().participationsAPayer()).isZero();
        assertThat(dto.paiements().montantNetPaye()).isEqualByComparingTo("15.00");
        assertThat(dto.paiements().montantRembourse()).isEqualByComparingTo("15.00");
    }

    @Test
    void getCurrentUserStats_propage_forbidden_si_aucun_joueur_lie() {
        when(currentUserFacade.getCurrentJoueur())
                .thenThrow(new ForbiddenException("Aucun joueur lié à l'utilisateur authentifié."));

        assertThatThrownBy(() -> service.getCurrentUserStats())
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Aucun joueur lié à l'utilisateur authentifié.");
    }

    private MatchPadel match(Joueur organisateur, LocalDateTime dateDebut, MatchStatut statut) {
        Site site = new Site("Site Delta", "Bruxelles");
        Terrain terrain = new Terrain("Terrain 1", site);
        MatchPadel match = new MatchPadel(terrain, organisateur, dateDebut, MatchVisibilite.PUBLIC);
        match.setStatut(statut);
        return match;
    }

    private Participation participation(MatchPadel match, Joueur joueur, BigDecimal... montants) {
        Participation participation = new Participation(match, joueur);
        match.addParticipation(participation);
        for (BigDecimal montant : montants) {
            TypePaiement type = montant.signum() < 0 ? TypePaiement.REMBOURSEMENT : TypePaiement.ENCAISSEMENT;
            participation.addPaiement(new Paiement(participation, montant, type, match.getDateDebut()));
        }
        return participation;
    }
}
