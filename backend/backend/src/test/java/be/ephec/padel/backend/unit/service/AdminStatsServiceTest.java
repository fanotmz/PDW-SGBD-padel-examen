package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.service.AdminStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    PaiementRepository paiementRepository;
    @Mock
    MatchPadelRepository matchPadelRepository;
    @Mock
    JoueurRepository joueurRepository;

    private AdminStatsService service;

    @BeforeEach
    void setUp() {
        service = new AdminStatsService(paiementRepository, matchPadelRepository, joueurRepository);
    }

    @Test
    void getCa_utilise_uniquement_les_encaissements() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(paiementRepository.sumMontantByDatePaiementBetweenAndType(
                eq(from.atStartOfDay()),
                eq(to.plusDays(1).atStartOfDay()),
                eq(TypePaiement.ENCAISSEMENT)
        )).thenReturn(new BigDecimal("42.50"));

        AdminCaStatsDto dto = service.getCa(from, to);

        assertThat(dto.getCaTotal()).isEqualByComparingTo("42.50");
        verify(paiementRepository).sumMontantByDatePaiementBetweenAndType(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                TypePaiement.ENCAISSEMENT
        );
    }

    @Test
    void getNbMatchs_utilise_le_compteur_filtre_par_statut() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(matchPadelRepository.countByDateDebutBetween(
                eq(from.atStartOfDay()),
                eq(to.plusDays(1).atStartOfDay())
        )).thenReturn(7L);

        AdminMatchsStatsDto dto = service.getNbMatchs(from, to);

        assertThat(dto.getNbMatchs()).isEqualTo(7L);
        verify(matchPadelRepository).countByDateDebutBetween(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void getDettes_retourne_la_somme_et_le_nombre_de_joueurs_en_dette() {
        when(joueurRepository.sumDettes()).thenReturn(new BigDecimal("50.00"));
        when(joueurRepository.countJoueursEnDette()).thenReturn(3L);

        AdminDettesStatsDto dto = service.getDettes();

        assertThat(dto.getDetteTotale()).isEqualByComparingTo("50.00");
        assertThat(dto.getNbJoueursEnDette()).isEqualTo(3L);
    }

    @Test
    void getNbMatchs_refuse_une_periode_invalide() {
        LocalDate from = LocalDate.of(2026, 4, 2);
        LocalDate to = LocalDate.of(2026, 4, 1);

        assertThatThrownBy(() -> service.getNbMatchs(from, to))
                .isInstanceOf(BusinessException.class);
    }
}
