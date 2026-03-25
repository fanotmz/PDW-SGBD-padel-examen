package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.exception.BusinessException;
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
import java.time.LocalDateTime;

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
    void getCa_conserve_un_mode_cash_base() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        when(paiementRepository.sumMontantByDatePaiementBetween(fromStart, toExclusive))
                .thenReturn(new BigDecimal("42.50"));

        AdminCaStatsDto dto = service.getCa(from, to);

        assertThat(dto.getCaTotal()).isEqualByComparingTo("42.50");
    }

    @Test
    void getNbMatchs_refuse_une_periode_invalide() {
        LocalDate from = LocalDate.of(2026, 4, 2);
        LocalDate to = LocalDate.of(2026, 4, 1);

        assertThatThrownBy(() -> service.getNbMatchs(from, to))
                .isInstanceOf(BusinessException.class);
    }
}
