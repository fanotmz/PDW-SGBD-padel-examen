package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import be.ephec.padel.backend.service.AdminSiteStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSiteStatsServiceTest {

    @Mock
    PaiementRepository paiementRepository;
    @Mock
    MatchPadelRepository matchPadelRepository;
    @Mock
    JoueurRepository joueurRepository;
    @Mock
    ServiceAutorisationAdmin serviceAutorisationAdmin;

    private AdminSiteStatsService service;

    @BeforeEach
    void setUp() {
        service = new AdminSiteStatsService(
                paiementRepository,
                matchPadelRepository,
                joueurRepository,
                serviceAutorisationAdmin
        );
    }

    @Test
    void getNbMatchs_utilise_le_compteur_filtre_par_site_et_statut() {
        Long siteId = 3L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(matchPadelRepository.countByDateDebutBetweenAndSiteId(
                eq(from.atStartOfDay()),
                eq(to.plusDays(1).atStartOfDay()),
                eq(siteId)
        )).thenReturn(4L);

        AdminMatchsStatsDto dto = service.getNbMatchs(siteId, from, to);

        assertThat(dto.getNbMatchs()).isEqualTo(4L);
        verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
        verify(matchPadelRepository).countByDateDebutBetweenAndSiteId(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                siteId
        );
    }

    @Test
    void getCa_conserve_un_mode_cash_base_par_site() {
        Long siteId = 3L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(paiementRepository.sumMontantByDatePaiementBetweenAndSiteId(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                siteId
        )).thenReturn(new BigDecimal("18.00"));

        AdminCaStatsDto dto = service.getCa(siteId, from, to);

        assertThat(dto.getCaTotal()).isEqualByComparingTo("18.00");
        verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
    }
}
