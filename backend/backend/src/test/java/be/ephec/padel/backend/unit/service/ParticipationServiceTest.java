package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.service.PaiementService;
import be.ephec.padel.backend.service.ParticipationService;
import be.ephec.padel.backend.service.SoldeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ParticipationServiceTest {

    private ParticipationRepository participationRepo;
    private MatchPadelRepository matchRepo;
    private JoueurRepository joueurRepo;
    private SoldeService soldeService;
    private PaiementService paiementService;
    private CurrentUserFacade currentUserFacade;

    private ParticipationService service;

    @BeforeEach
    void setup() {
        participationRepo = mock(ParticipationRepository.class);
        matchRepo = mock(MatchPadelRepository.class);
        joueurRepo = mock(JoueurRepository.class);
        soldeService = mock(SoldeService.class);
        paiementService = mock(PaiementService.class);
        currentUserFacade = mock(CurrentUserFacade.class);

        service = new ParticipationService(
                participationRepo,
                matchRepo,
                joueurRepo,
                soldeService,
                paiementService,
                currentUserFacade
        );
    }

    private Joueur stubCurrentJoueur(String matricule) {
        Joueur joueur = new Joueur(matricule, "Nom", TypeJoueur.GLOBAL);
        when(currentUserFacade.getCurrentJoueur()).thenReturn(joueur);
        return joueur;
    }

    @Test
    void calculerMontantAttendu_matchIntrouvable_notFound() {
        when(matchRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.calculerMontantAttenduPourMatchPublic(1L));

        verifyNoInteractions(joueurRepo);
    }

    @Test
    void calculerMontantAttendu_matchPrive_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.calculerMontantAttenduPourMatchPublic(1L));

        verifyNoInteractions(joueurRepo);
    }

    @Test
    void calculerMontantAttendu_matchAnnule_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);
        match.setStatut(MatchStatut.ANNULE);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.calculerMontantAttenduPourMatchPublic(1L));

        verifyNoInteractions(joueurRepo);
    }

    @Test
    void calculerMontantAttendu_public_sansDette_retourne15() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = stubCurrentJoueur("G1");

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        BigDecimal montant = service.calculerMontantAttenduPourMatchPublic(1L);

        assertEquals(new BigDecimal("15.00"), montant);
        assertEquals(BigDecimal.ZERO, joueur.getSolde());
    }

    @Test
    void calculerMontantAttendu_public_avecDette15_retourne30() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = stubCurrentJoueur("G1");
        joueur.setSolde(new BigDecimal("15.00"));

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        BigDecimal montant = service.calculerMontantAttenduPourMatchPublic(1L);

        assertEquals(new BigDecimal("30.00"), montant);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchIntrouvable_notFound() {
        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchPrive_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchAnnule_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);
        match.setStatut(MatchStatut.ANNULE);

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_dejaInscrit_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = stubCurrentJoueur("G1");

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, joueur.getMatricule())).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchComplet_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);
        match.setDateDebut(LocalDateTime.now());
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());
        match.getParticipations().add(new Participation());

        Joueur joueur = stubCurrentJoueur("G1");

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, joueur.getMatricule())).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_ok_sansDette_paie15_et_debite15() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = stubCurrentJoueur("G1");

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, joueur.getMatricule())).thenReturn(false);
        when(participationRepo.save(any(Participation.class)))
                .thenAnswer(inv -> {
                    Participation participation = inv.getArgument(0);
                    ReflectionTestUtils.setField(participation, "id", 99L);
                    return participation;
                });

        Participation result = service.rejoindreEtPayerMatchPublic(1L);
        assertNotNull(result);

        verify(soldeService).debiter(eq("G1"), eq(Tarifs.PART_PAR_JOUEUR));
        verify(paiementService).payerParticipationAvecRattrapageDette(eq(99L), eq(new BigDecimal("15.00")));
    }

    @Test
    void rejoindreEtPayerMatchPublic_ok_avecDette15_paie30_et_debite15() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        Joueur joueur = stubCurrentJoueur("G1");
        joueur.setSolde(new BigDecimal("15.00"));

        when(matchRepo.findByIdForUpdateWithParticipations(1L)).thenReturn(Optional.of(match));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, joueur.getMatricule())).thenReturn(false);
        when(participationRepo.save(any(Participation.class)))
                .thenAnswer(inv -> {
                    Participation participation = inv.getArgument(0);
                    ReflectionTestUtils.setField(participation, "id", 100L);
                    return participation;
                });

        Participation result = service.rejoindreEtPayerMatchPublic(1L);
        assertNotNull(result);

        verify(soldeService).debiter(eq("G1"), eq(Tarifs.PART_PAR_JOUEUR));
        verify(paiementService).payerParticipationAvecRattrapageDette(eq(100L), eq(new BigDecimal("30.00")));
    }

    @Test
    void ajouterJoueurParOrganisateur_matchIntrouvable_notFound() {
        when(matchRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_matchPublic_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PUBLIC);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_matchAnnule_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        match.setStatut(MatchStatut.ANNULE);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_organisateurDifferent_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        match.setOrganisateur(new Joueur("G999", "Orga", TypeJoueur.GLOBAL));

        when(currentUserFacade.getCurrentJoueur()).thenReturn(new Joueur("G1", "Current", TypeJoueur.GLOBAL));
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_joueurAAjouterIntrouvable_notFound() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        Joueur orga = new Joueur("G1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));
        when(joueurRepo.findById("G2")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verifyNoInteractions(participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_dejaInscrit_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        Joueur orga = new Joueur("G1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(new Joueur("G2", "Joueur", TypeJoueur.GLOBAL)));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_matchComplet_refuse() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        Joueur orga = new Joueur("G1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(new Joueur("G2", "Joueur", TypeJoueur.GLOBAL)));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(4);

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G2"));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_ok_creeParticipation_et_debite_sans_payer() {
        MatchPadel match = new MatchPadel();
        match.setVisibilite(MatchVisibilite.PRIVE);
        Joueur orga = new Joueur("G1", "Orga", TypeJoueur.GLOBAL);
        match.setOrganisateur(orga);

        when(currentUserFacade.getCurrentJoueur()).thenReturn(orga);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(new Joueur("G2", "Joueur", TypeJoueur.GLOBAL)));
        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(1);

        Participation saved = new Participation();
        when(participationRepo.save(any(Participation.class))).thenReturn(saved);

        Participation result = service.ajouterJoueurParOrganisateur(1L, "G2");

        assertSame(saved, result);
        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G2"), eq(Tarifs.PART_PAR_JOUEUR));
        verifyNoInteractions(paiementService);
    }
}
