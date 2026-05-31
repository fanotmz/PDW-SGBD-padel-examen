package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MouvementSolde;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.service.SoldeOriginContext;
import be.ephec.padel.backend.service.SoldeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SoldeServiceTest {

    private JoueurRepository joueurRepo;
    private MouvementSoldeRepository mouvementRepo;

    private SoldeService service;

    @BeforeEach
    void setup() {
        joueurRepo = mock(JoueurRepository.class);
        mouvementRepo = mock(MouvementSoldeRepository.class);
        service = new SoldeService(joueurRepo, mouvementRepo);
    }

    @Test
    void debiter_montantNull_refuse() {
        assertThrows(BusinessException.class, () -> service.debiter("G1", null));
        verifyNoInteractions(joueurRepo, mouvementRepo);
    }

    @Test
    void debiter_montantNegatifOuZero_refuse() {
        assertThrows(BusinessException.class, () -> service.debiter("G1", BigDecimal.ZERO));
        assertThrows(BusinessException.class, () -> service.debiter("G1", new BigDecimal("-1.00")));
        verifyNoInteractions(joueurRepo, mouvementRepo);
    }

    @Test
    void debiter_joueurIntrouvable_notFound() {
        when(joueurRepo.findById("G1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.debiter("G1", new BigDecimal("15.00")));

        verifyNoInteractions(mouvementRepo);
        verify(joueurRepo, never()).save(any());
    }

    @Test
    void debiter_ok_soldeNull_devientDette() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(null);

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));
        when(joueurRepo.save(any(Joueur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementRepo.save(any(MouvementSolde.class))).thenAnswer(inv -> inv.getArgument(0));

        service.debiter("G1", new BigDecimal("15.00"));

        assertEquals(new BigDecimal("15.00"), j.getSolde());
        verify(joueurRepo).save(j);
        verify(mouvementRepo).save(argThat(mouvement ->
                mouvement.getOrigineType() == OrigineMouvementSoldeType.LEGACY
                        && mouvement.getParticipationId() == null
                        && mouvement.getMatchId() == null
                        && mouvement.getDescription() == null
        ));
    }

    @Test
    void debiter_ok_ajoute_a_dette_existante() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(new BigDecimal("10.00"));

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));

        service.debiter("G1", new BigDecimal("5.00"));

        assertEquals(new BigDecimal("15.00"), j.getSolde());
        verify(joueurRepo).save(j);
        verify(mouvementRepo).save(any(MouvementSolde.class));
    }

    @Test
    void crediter_montantNull_refuse() {
        assertThrows(BusinessException.class, () -> service.crediter("G1", null));
        verifyNoInteractions(joueurRepo, mouvementRepo);
    }

    @Test
    void crediter_joueurIntrouvable_notFound() {
        when(joueurRepo.findById("G1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.crediter("G1", new BigDecimal("1.00")));

        verifyNoInteractions(mouvementRepo);
        verify(joueurRepo, never()).save(any());
    }

    @Test
    void crediter_paiementTropEleve_refuse() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(new BigDecimal("5.00"));

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));

        assertThrows(BusinessException.class, () -> service.crediter("G1", new BigDecimal("6.00")));

        verify(joueurRepo, never()).save(any());
        verifyNoInteractions(mouvementRepo);
    }

    @Test
    void crediter_ok_diminue_dette() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(new BigDecimal("10.00"));

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));

        service.crediter("G1", new BigDecimal("4.00"));

        assertEquals(new BigDecimal("6.00"), j.getSolde());
        verify(joueurRepo).save(j);
        verify(mouvementRepo).save(any(MouvementSolde.class));
    }

    @Test
    void crediter_ok_arrondi_scale2() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(new BigDecimal("10.00"));

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));

        service.crediter("G1", new BigDecimal("1.1"));

        assertEquals(new BigDecimal("8.90"), j.getSolde());
        verify(joueurRepo).save(j);
        verify(mouvementRepo).save(any(MouvementSolde.class));
    }

    @Test
    void debiter_avecContexte_persiste_origine_complete() {
        Joueur j = new Joueur("G1", "Nom", null);
        j.setSolde(BigDecimal.ZERO);

        when(joueurRepo.findById("G1")).thenReturn(Optional.of(j));
        when(joueurRepo.save(any(Joueur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mouvementRepo.save(any(MouvementSolde.class))).thenAnswer(inv -> inv.getArgument(0));

        SoldeOriginContext context = new SoldeOriginContext(
                OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE,
                123L,
                456L,
                "Ajout manuel a un match prive"
        );

        service.debiter("G1", new BigDecimal("15.00"), context);

        verify(mouvementRepo).save(argThat(mouvement ->
                mouvement.getType() == be.ephec.padel.backend.model.enums.TypeMouvement.DEBIT
                        && mouvement.getParticipationId().equals(123L)
                        && mouvement.getMatchId().equals(456L)
                        && mouvement.getOrigineType() == OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE
                        && "Ajout manuel a un match prive".equals(mouvement.getDescription())
        ));
    }
}
