package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ParticipationServiceTest {

    private ParticipationRepository participationRepo;
    private MatchPadelRepository matchRepo;
    private JoueurRepository joueurRepo;
    private SoldeService soldeService;
    private PaiementService paiementService;

    private ParticipationService service;

    @BeforeEach
    void setup() {
        participationRepo = mock(ParticipationRepository.class);
        matchRepo = mock(MatchPadelRepository.class);
        joueurRepo = mock(JoueurRepository.class);
        soldeService = mock(SoldeService.class);
        paiementService = mock(PaiementService.class);

        service = new ParticipationService(participationRepo, matchRepo, joueurRepo, soldeService, paiementService);
    }

    // -------------------------
    // rejoindreEtPayerMatchPublic
    // -------------------------

    @Test
    void rejoindreEtPayerMatchPublic_matchIntrouvable_notFound() {
        when(matchRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00")));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchPrive_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00")));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_montantNull_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", null));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_montantNegatifOuZero_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", BigDecimal.ZERO));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("-1.00")));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_montantDifferentDe15_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("10.00")));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    // ✅ NOUVEAU TEST (point 1)
    @Test
    void rejoindreEtPayerMatchPublic_montant15Point0_accepte() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G1")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G1")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(2);

        Participation saved = mock(Participation.class);
        when(saved.getId()).thenReturn(99L);
        when(participationRepo.save(any(Participation.class))).thenReturn(saved);

        BigDecimal montant = new BigDecimal("15.0"); // scale différent de 15.00
        Participation result = service.rejoindreEtPayerMatchPublic(1L, "G1", montant);

        assertSame(saved, result);

        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G1"), argThat(bd -> bd != null && bd.compareTo(new BigDecimal("15.00")) == 0));
        verify(paiementService).payerParticipation(eq(99L), argThat(bd -> bd != null && bd.compareTo(new BigDecimal("15.00")) == 0));
    }

    @Test
    void rejoindreEtPayerMatchPublic_joueurIntrouvable_notFound() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        when(joueurRepo.findById("G1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00")));

        verifyNoInteractions(participationRepo, soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_dejaInscrit_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G1")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G1")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00")));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_matchComplet_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G1")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G1")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(4);

        assertThrows(BusinessException.class,
                () -> service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00")));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void rejoindreEtPayerMatchPublic_ok_creeParticipation_debite_et_paye() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G1")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G1")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(2);

        Participation saved = mock(Participation.class);
        when(saved.getId()).thenReturn(99L);
        when(participationRepo.save(any(Participation.class))).thenReturn(saved);

        Participation result = service.rejoindreEtPayerMatchPublic(1L, "G1", new BigDecimal("15.00"));

        assertSame(saved, result);

        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G1"), any(BigDecimal.class));
        verify(paiementService).payerParticipation(eq(99L), eq(new BigDecimal("15.00")));
    }

    // -------------------------
    // ajouterJoueurParOrganisateur
    // -------------------------

    @Test
    void ajouterJoueurParOrganisateur_matchIntrouvable_notFound() {
        when(matchRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_matchPublic_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PUBLIC);
        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_organisateurDifferent_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G999");
        when(match.getOrganisateur()).thenReturn(orga);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verifyNoInteractions(joueurRepo, participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_joueurAAjouterIntrouvable_notFound() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G1");
        when(match.getOrganisateur()).thenReturn(orga);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));
        when(joueurRepo.findById("G2")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verifyNoInteractions(participationRepo, soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_dejaInscrit_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G1");
        when(match.getOrganisateur()).thenReturn(orga);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_matchComplet_refuse() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G1");
        when(match.getOrganisateur()).thenReturn(orga);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(4);

        assertThrows(BusinessException.class,
                () -> service.ajouterJoueurParOrganisateur(1L, "G1", "G2"));

        verify(participationRepo, never()).save(any());
        verifyNoInteractions(soldeService, paiementService);
    }

    @Test
    void ajouterJoueurParOrganisateur_ok_creeParticipation_et_debite_sans_payer() {
        MatchPadel match = mock(MatchPadel.class);
        when(match.getVisibilite()).thenReturn(MatchVisibilite.PRIVE);

        Joueur orga = mock(Joueur.class);
        when(orga.getMatricule()).thenReturn("G1");
        when(match.getOrganisateur()).thenReturn(orga);

        when(matchRepo.findById(1L)).thenReturn(Optional.of(match));

        Joueur joueur = mock(Joueur.class);
        when(joueurRepo.findById("G2")).thenReturn(Optional.of(joueur));

        when(participationRepo.existsByMatch_IdAndJoueur_Matricule(1L, "G2")).thenReturn(false);
        when(participationRepo.countByMatch_Id(1L)).thenReturn(1);

        Participation saved = mock(Participation.class);
        when(participationRepo.save(any(Participation.class))).thenReturn(saved);

        Participation result = service.ajouterJoueurParOrganisateur(1L, "G1", "G2");

        assertSame(saved, result);

        verify(participationRepo).save(any(Participation.class));
        verify(soldeService).debiter(eq("G2"), any(BigDecimal.class));
        verifyNoInteractions(paiementService);
    }
}