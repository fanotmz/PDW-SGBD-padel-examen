package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaiementServiceTest {

    private PaiementRepository paiementRepo;
    private ParticipationRepository participationRepo;
    private SoldeService soldeService;

    private PaiementService service;

    @BeforeEach
    void setup() {
        paiementRepo = mock(PaiementRepository.class);
        participationRepo = mock(ParticipationRepository.class);
        soldeService = mock(SoldeService.class);

        service = new PaiementService(paiementRepo, participationRepo, soldeService);
    }

    // ----------------
    // payerParticipation
    // ----------------

    @Test
    void payerParticipation_participationIntrouvable_notFound() {
        when(participationRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.payerParticipation(1L, new BigDecimal("5.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_montantNull_refuse() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, null));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_montantNegatifOuZero_refuse() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, BigDecimal.ZERO));
        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("-1.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerParticipation_dejaPayeTotalement_refuse() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        // PART_JOUEUR = 15.00, si dejaPaye >= 15 => reste <= 0
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("15.00"));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("1.00")));

        verify(paiementRepo, never()).save(any(Paiement.class));
        verifyNoInteractions(soldeService);
    }

    @Test
    void payerParticipation_paiementTropEleve_refuse() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        // deja payé 10 => reste 5
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("10.00"));

        assertThrows(BusinessException.class, () -> service.payerParticipation(1L, new BigDecimal("6.00")));

        verify(paiementRepo, never()).save(any(Paiement.class));
        verifyNoInteractions(soldeService);
    }

    @Test
    void payerParticipation_ok_dejaPayeNull_considererZero_et_crediter() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        Joueur j = mock(Joueur.class);
        when(j.getMatricule()).thenReturn("G1");
        when(p.getJoueur()).thenReturn(j);

        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(null);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipation(1L, new BigDecimal("5.00"));

        assertSame(saved, result);
        verify(paiementRepo).save(any(Paiement.class));
        verify(soldeService).crediter("G1", new BigDecimal("5.00").setScale(2));
    }

    @Test
    void payerParticipation_ok_paiementPartiel_et_crediter() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        Joueur j = mock(Joueur.class);
        when(j.getMatricule()).thenReturn("G1");
        when(p.getJoueur()).thenReturn(j);

        // deja payé 7.50 => reste 7.50
        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(new BigDecimal("7.50"));

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerParticipation(1L, new BigDecimal("7.50"));

        assertSame(saved, result);
        verify(soldeService).crediter("G1", new BigDecimal("7.50"));
    }

    @Test
    void payerParticipation_arrondi_scale2() {
        Participation p = mock(Participation.class);
        when(participationRepo.findById(1L)).thenReturn(Optional.of(p));

        Joueur j = mock(Joueur.class);
        when(j.getMatricule()).thenReturn("G1");
        when(p.getJoueur()).thenReturn(j);

        when(paiementRepo.sumMontantByParticipationId(1L)).thenReturn(BigDecimal.ZERO);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        // 5.1 doit devenir 5.10
        Paiement result = service.payerParticipation(1L, new BigDecimal("5.1"));

        assertSame(saved, result);
        verify(soldeService).crediter("G1", new BigDecimal("5.10"));
    }

    // ----------------
    // payerPourMatch
    // ----------------

    @Test
    void payerPourMatch_participationIntrouvable_notFound() {
        when(participationRepo.findByMatch_IdAndJoueur_Matricule(10L, "G1"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.payerPourMatch(10L, "G1", new BigDecimal("5.00")));

        verifyNoInteractions(paiementRepo, soldeService);
    }

    @Test
    void payerPourMatch_ok_appelle_payerParticipation() {
        Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(77L);

        when(participationRepo.findByMatch_IdAndJoueur_Matricule(10L, "G1"))
                .thenReturn(Optional.of(p));

        // pour payerParticipation : il faut que findById(77) renvoie la même participation
        when(participationRepo.findById(77L)).thenReturn(Optional.of(p));

        Joueur j = mock(Joueur.class);
        when(j.getMatricule()).thenReturn("G1");
        when(p.getJoueur()).thenReturn(j);

        when(paiementRepo.sumMontantByParticipationId(77L)).thenReturn(BigDecimal.ZERO);

        Paiement saved = mock(Paiement.class);
        when(paiementRepo.save(any(Paiement.class))).thenReturn(saved);

        Paiement result = service.payerPourMatch(10L, "G1", new BigDecimal("5.00"));

        assertSame(saved, result);
        verify(soldeService).crediter("G1", new BigDecimal("5.00"));
    }
}