package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.SiteRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
    SiteRepository siteRepository;
    @Mock
    ServiceAutorisationAdmin serviceAutorisationAdmin;

    private AdminSiteStatsService service;

    @BeforeEach
    void setUp() {
        service = new AdminSiteStatsService(
                paiementRepository,
                matchPadelRepository,
                joueurRepository,
                siteRepository,
                serviceAutorisationAdmin
        );
    }

    @Test
    void getCa_utilise_uniquement_les_encaissements_du_site() {
        Long siteId = 3L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(siteRepository.existsById(siteId)).thenReturn(true);
        when(paiementRepository.sumMontantByDatePaiementBetweenAndSiteIdAndType(
                eq(from.atStartOfDay()),
                eq(to.plusDays(1).atStartOfDay()),
                eq(siteId),
                eq(TypePaiement.ENCAISSEMENT)
        )).thenReturn(new BigDecimal("18.00"));

        AdminCaStatsDto dto = service.getCa(siteId, from, to);

        assertThat(dto.getCaTotal()).isEqualByComparingTo("18.00");
        verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
        verify(siteRepository).existsById(siteId);
    }

    @Test
    void getNbMatchs_utilise_le_compteur_filtre_par_site_et_statut() {
        Long siteId = 3L;
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        when(siteRepository.existsById(siteId)).thenReturn(true);
        when(matchPadelRepository.countByDateDebutBetweenAndSiteId(
                eq(from.atStartOfDay()),
                eq(to.plusDays(1).atStartOfDay()),
                eq(siteId)
        )).thenReturn(4L);

        AdminMatchsStatsDto dto = service.getNbMatchs(siteId, from, to);

        assertThat(dto.getNbMatchs()).isEqualTo(4L);
        verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
        verify(siteRepository).existsById(siteId);
        verify(matchPadelRepository).countByDateDebutBetweenAndSiteId(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                siteId
        );
    }

    @Test
    void getDettes_retourne_la_somme_et_le_nombre_de_joueurs_en_dette_du_site() {
        Long siteId = 3L;

        when(siteRepository.existsById(siteId)).thenReturn(true);
        when(joueurRepository.sumDettesBySiteId(siteId)).thenReturn(new BigDecimal("12.00"));
        when(joueurRepository.countJoueursEnDetteBySiteId(siteId)).thenReturn(2L);

        AdminDettesStatsDto dto = service.getDettes(siteId);

        assertThat(dto.getDetteTotale()).isEqualByComparingTo("12.00");
        assertThat(dto.getNbJoueursEnDette()).isEqualTo(2L);
        verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
        verify(siteRepository).existsById(siteId);
    }

    @Test
    void getDettes_refuse_un_site_inexistant() {
        Long siteId = 99L;

        when(siteRepository.existsById(siteId)).thenReturn(false);

        assertThatThrownBy(() -> service.getDettes(siteId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Site introuvable: 99");

        var inOrder = inOrder(serviceAutorisationAdmin, siteRepository);
        inOrder.verify(serviceAutorisationAdmin).verifierAccesAuSite(siteId);
        inOrder.verify(siteRepository).existsById(siteId);
    }
}
