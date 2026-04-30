package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.service.PaiementService;
import be.ephec.padel.backend.service.SoldeOriginContext;
import be.ephec.padel.backend.service.SoldeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaiementServiceTest {

    private PaiementRepository paiementRepo;
    private ParticipationRepository participationRepo;
    private SoldeService soldeService;
    private CurrentUserFacade currentUserFacade;
    private Clock clock;

    private PaiementService service;

    @BeforeEach
    void setup() {
        paiementRepo = mock(PaiementRepository.class);
        participationRepo = mock(ParticipationRepository.class);
        soldeService = mock(SoldeService.class);
        currentUserFacade = mock(CurrentUserFacade.class);
        clock = Clock.fixed(Instant.parse("2030-01-01T09:00:00Z"), ZoneOffset.UTC);

        service = new PaiementService(paiementRepo, participationRepo, soldeService, clock, currentUserFacade);
    }

    private Joueur stubCurrentJoueur(String matricule) {
        Joueur joueur = mock(Joueur.class);
        when(joueur.getMatricule()).thenReturn(matricule);
        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        return joueur;
    }

    @Test
    void payerParticipation_participationIntrouvable_notFound() {
        when(participationRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.payerParticipation(1L, new BigDecimal("5.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_refuse_si_joueur_courant_non_proprietaire() {
        Participation participation = mock(Participation.class);
        Joueur participant = mock(Joueur.class);
        when(participant.getMatricule()).thenReturn("G2");
        when(participation.getJoueur()).thenReturn(participant);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        stubCurrentJoueur("G1");

        assertThrows(ForbiddenException.class, () -> service.payerParticipation(1L, new BigDecimal("5.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_montantNull_refuse() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, null));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_montantNegatifOuZero_refuse() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, BigDecimal.ZERO));
        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("-1.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_matchAnnule_refuse() {
        Participation participation = mock(Participation.class);
        MatchPadel match = mock(MatchPadel.class);
        Joueur joueur = stubCurrentJoueur("G1");
        when(match.getStatut()).thenReturn(MatchStatut.ANNULE);
        when(participation.getMatch()).thenReturn(match);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("5.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_dejaPayeTotalement_refuse() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("15.00"));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("1.00")));

        verifyNoInteractions(soldeService);
    }

    @Test
    void payerParticipation_paiementTropEleve_refuse() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("10.00"));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("6.00")));

        verifyNoInteractions(soldeService);
    }

    @Test
    void payerParticipation_ok_dejaPayeNull_considererZero_et_crediter() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(50L);
        when(participation.getId()).thenReturn(1L);
        when(participation.getMatch()).thenReturn(match);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(null);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipation(1L, new BigDecimal("5.00"));

        assertSame(saved, result);
        verify(paiementRepo).save(argThat(paiement ->
                paiement.getType() == TypePaiement.ENCAISSEMENT
                        && paiement.getMontant().compareTo(new BigDecimal("5.00")) == 0
        ));
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("5.00").setScale(2)), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION
                        && Long.valueOf(1L).equals(context.getParticipationId())
                        && Long.valueOf(50L).equals(context.getMatchId())
        ));
    }

    @Test
    void payerParticipation_ok_paiementPartiel_et_crediter() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(51L);
        when(participation.getId()).thenReturn(1L);
        when(participation.getMatch()).thenReturn(match);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("7.50"));

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipation(1L, new BigDecimal("7.50"));

        assertSame(saved, result);
        verify(paiementRepo).save(argThat(paiement ->
                paiement.getType() == TypePaiement.ENCAISSEMENT
                        && paiement.getMontant().compareTo(new BigDecimal("7.50")) == 0
        ));
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("7.50")), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION
                        && Long.valueOf(1L).equals(context.getParticipationId())
                        && Long.valueOf(51L).equals(context.getMatchId())
        ));
    }

    @Test
    void payerParticipation_arrondi_scale2() {
        Participation participation = mock(Participation.class);
        Joueur joueur = stubCurrentJoueur("G1");
        MatchPadel match = mock(MatchPadel.class);
        when(match.getId()).thenReturn(52L);
        when(participation.getId()).thenReturn(1L);
        when(participation.getMatch()).thenReturn(match);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(BigDecimal.ZERO);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipation(1L, new BigDecimal("5.1"));

        assertSame(saved, result);
        verify(paiementRepo).save(argThat(paiement ->
                paiement.getType() == TypePaiement.ENCAISSEMENT
                        && paiement.getMontant().compareTo(new BigDecimal("5.10")) == 0
        ));
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("5.10")), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION
                        && Long.valueOf(1L).equals(context.getParticipationId())
                        && Long.valueOf(52L).equals(context.getMatchId())
        ));
    }

    @Test
    void payerParticipationAvecRattrapageDette_reste_flux_special_hors_moteur_cible() {
        Participation participation = mock(Participation.class);
        Joueur joueur = mock(Joueur.class);
        MatchPadel match = mock(MatchPadel.class);
        when(joueur.getMatricule()).thenReturn("G1");
        when(joueur.getSolde()).thenReturn(new BigDecimal("5.00"));
        when(match.getId()).thenReturn(70L);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(participation.getId()).thenReturn(1L);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participation.getMatch()).thenReturn(match);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(BigDecimal.ZERO);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipationAvecRattrapageDette(1L, new BigDecimal("20.00"));

        assertSame(saved, result);
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("15.00")), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION
                        && Long.valueOf(1L).equals(context.getParticipationId())
                        && Long.valueOf(70L).equals(context.getMatchId())
        ));
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("5.00")), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.RATTRAPAGE_DETTE
                        && context.getParticipationId() == null
                        && context.getMatchId() == null
        ));
    }

    @Test
    void payerParticipationAvecRattrapageDette_sansDetteAnterieure_trace_un_credit_cible_sur_la_participation() {
        Participation participation = mock(Participation.class);
        Joueur joueur = mock(Joueur.class);
        MatchPadel match = mock(MatchPadel.class);
        when(joueur.getMatricule()).thenReturn("G1");
        when(joueur.getSolde()).thenReturn(new BigDecimal("0.00"));
        when(match.getId()).thenReturn(71L);
        when(match.getStatut()).thenReturn(MatchStatut.PLANIFIE);
        when(participation.getId()).thenReturn(2L);
        when(participation.getJoueur()).thenReturn(joueur);
        when(participation.getMatch()).thenReturn(match);
        when(participationRepo.findById(2L)).thenReturn(Optional.of(participation));
        when(paiementRepo.sumMontantByParticipationId(2L)).thenReturn(BigDecimal.ZERO);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipationAvecRattrapageDette(2L, new BigDecimal("15.00"));

        assertSame(saved, result);
        verify(soldeService).crediter(eq("G1"), eq(new BigDecimal("15.00")), argThat((SoldeOriginContext context) ->
                context.getOrigineType() == OrigineMouvementSoldeType.PAIEMENT_PARTICIPATION
                        && Long.valueOf(2L).equals(context.getParticipationId())
                        && Long.valueOf(71L).equals(context.getMatchId())
        ));
        verify(soldeService, org.mockito.Mockito.never()).crediter(eq("G1"), eq(new BigDecimal("15.00")));
    }

    @Test
    void validerMontant_remboursementPositifOuZero_refuse() {
        assertThrows(BusinessException.class, () ->
                ReflectionTestUtils.invokeMethod(
                        service, "validerMontant", BigDecimal.ZERO, TypePaiement.REMBOURSEMENT
                ));

        assertThrows(BusinessException.class, () ->
                ReflectionTestUtils.invokeMethod(
                        service, "validerMontant", new BigDecimal("1.00"), TypePaiement.REMBOURSEMENT
                ));
    }

    @Test
    void validerMontant_remboursementNegatif_ok() {
        BigDecimal montant = ReflectionTestUtils.invokeMethod(
                service, "validerMontant", new BigDecimal("-5.1"), TypePaiement.REMBOURSEMENT
        );

        assertEquals(new BigDecimal("-5.10"), montant);
    }

    @Test
    void enregistrerRemboursementAnnulation_matchNonAnnule_refuse() {
        Site site = new Site("Site A", "Bruxelles");
        Terrain terrain = new Terrain("T1", site);
        Joueur organisateur = new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL);
        MatchPadel match = new MatchPadel(
                terrain,
                organisateur,
                java.time.LocalDateTime.of(2030, 1, 2, 10, 0),
                MatchVisibilite.PUBLIC
        );
        Joueur joueur = new Joueur("J001", "Alice", TypeJoueur.GLOBAL);
        Participation participation = new Participation(match, joueur);

        assertThrows(BusinessException.class, () ->
                ReflectionTestUtils.invokeMethod(
                        service,
                        "enregistrerRemboursementAnnulation",
                        participation,
                        new BigDecimal("5.00")
                ));

        verifyNoInteractions(paiementRepo);
    }

    @Test
    void enregistrerRemboursementAnnulation_matchAnnule_enregistreUnRemboursementNegatif() {
        Site site = new Site("Site A", "Bruxelles");
        Terrain terrain = new Terrain("T1", site);
        Joueur organisateur = new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL);
        MatchPadel match = new MatchPadel(
                terrain,
                organisateur,
                java.time.LocalDateTime.of(2030, 1, 2, 10, 0),
                MatchVisibilite.PUBLIC
        );
        match.setStatut(MatchStatut.ANNULE);
        Joueur joueur = new Joueur("J001", "Alice", TypeJoueur.GLOBAL);
        Participation participation = new Participation(match, joueur);
        Paiement saved = mock(Paiement.class);

        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = ReflectionTestUtils.invokeMethod(
                service,
                "enregistrerRemboursementAnnulation",
                participation,
                new BigDecimal("5.00")
        );

        assertSame(saved, result);
        verify(paiementRepo).save(argThat(paiement ->
                paiement.getType() == TypePaiement.REMBOURSEMENT
                        && paiement.getMontant().compareTo(new BigDecimal("-5.00")) == 0
        ));
    }
}
