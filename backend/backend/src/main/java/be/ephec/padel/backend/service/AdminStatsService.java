package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.AdminCaStatsDto;
import be.ephec.padel.backend.dto.response.AdminDettesStatsDto;
import be.ephec.padel.backend.dto.response.AdminMatchsStatsDto;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.model.enums.TypePaiement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AdminStatsService {

    private final PaiementRepository paiementRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final JoueurRepository joueurRepository;

    public AdminStatsService(PaiementRepository paiementRepository,
                             MatchPadelRepository matchPadelRepository,
                             JoueurRepository joueurRepository) {
        this.paiementRepository = paiementRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.joueurRepository = joueurRepository;
    }

    public AdminCaStatsDto getCa(LocalDate from, LocalDate to) {
        validatePeriod(from, to);

        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        BigDecimal ca = paiementRepository.sumMontantByDatePaiementBetweenAndType(
                fromStart,
                toExclusive,
                TypePaiement.ENCAISSEMENT
        );
        return new AdminCaStatsDto(ca, from, to);
    }

    public AdminMatchsStatsDto getNbMatchs(LocalDate from, LocalDate to) {
        validatePeriod(from, to);

        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        long nb = matchPadelRepository.countByDateDebutBetween(fromStart, toExclusive);
        return new AdminMatchsStatsDto(nb, from, to);
    }

    public AdminDettesStatsDto getDettes() {
        BigDecimal detteTotale = joueurRepository.sumDettes();
        long nbJoueursEnDette = joueurRepository.countJoueursEnDette();
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
