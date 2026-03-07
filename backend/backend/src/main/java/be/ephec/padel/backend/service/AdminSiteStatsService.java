package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AdminSiteStatsService {

    private final PaiementRepository paiementRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final JoueurRepository joueurRepository;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    public AdminSiteStatsService(PaiementRepository paiementRepository,
                                 MatchPadelRepository matchPadelRepository,
                                 JoueurRepository joueurRepository,
                                 ServiceAutorisationAdmin serviceAutorisationAdmin) {
        this.paiementRepository = paiementRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.joueurRepository = joueurRepository;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
    }

    public AdminCaStatsDto getCa(Long siteId, LocalDate from, LocalDate to) {
        // Sécurité : ADMIN_GLOBAL ok partout, ADMIN_SITE seulement sur son site
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        validatePeriod(from, to);
        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        BigDecimal ca = paiementRepository.sumMontantByDatePaiementBetweenAndSiteId(fromStart, toExclusive, siteId);
        return new AdminCaStatsDto(ca, from, to);
    }

    public AdminMatchsStatsDto getNbMatchs(Long siteId, LocalDate from, LocalDate to) {
        // Sécurité : ADMIN_GLOBAL ok partout, ADMIN_SITE seulement sur son site
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        validatePeriod(from, to);
        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        long nb = matchPadelRepository.countByDateDebutBetweenAndSiteId(fromStart, toExclusive, siteId);
        return new AdminMatchsStatsDto(nb, from, to);
    }

    public AdminDettesStatsDto getDettes(Long siteId) {
        // Sécurité : ADMIN_GLOBAL ok partout, ADMIN_SITE seulement sur son site
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        BigDecimal detteTotale = joueurRepository.sumDettesBySiteId(siteId);
        long nbJoueursEnDette = joueurRepository.countJoueursEnDetteBySiteId(siteId);
        return new AdminDettesStatsDto(detteTotale, nbJoueursEnDette);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Les paramètres 'from' et 'to' sont obligatoires.");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("La date 'from' doit être <= à la date 'to'.");
        }
    }
}