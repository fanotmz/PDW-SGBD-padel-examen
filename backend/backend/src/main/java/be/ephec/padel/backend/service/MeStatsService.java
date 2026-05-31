package be.ephec.padel.backend.service;

import be.ephec.padel.backend.common.Tarifs;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.MeMatchRoleStatsDto;
import be.ephec.padel.backend.dto.response.MeNextMatchDto;
import be.ephec.padel.backend.dto.response.MePaymentStatsDto;
import be.ephec.padel.backend.dto.response.MeStatsDto;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class MeStatsService {

    private static final BigDecimal PART_JOUEUR = Tarifs.PART_PAR_JOUEUR;

    private final CurrentUserFacade currentUserFacade;
    private final ParticipationRepository participationRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final Clock clock;

    public MeStatsService(CurrentUserFacade currentUserFacade,
                          ParticipationRepository participationRepository,
                          MatchPadelRepository matchPadelRepository,
                          Clock clock) {
        this.currentUserFacade = currentUserFacade;
        this.participationRepository = participationRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.clock = clock;
    }

    public MeStatsDto getCurrentUserStats() {
        Joueur joueur = currentUserFacade.getCurrentJoueur();
        String matricule = joueur.getMatricule();
        LocalDateTime now = LocalDateTime.now(clock);

        List<Participation> participations =
                participationRepository.findByJoueurMatriculeWithStatsDetails(matricule);
        List<MatchPadel> matchsOrganises =
                matchPadelRepository.findOrganizedMatchesWithDetailsByMatricule(matricule);

        return new MeStatsDto(
                getProchainMatch(matricule, matchsOrganises, participations, now).orElse(null),
                getStatsMatchsOrganisateur(matchsOrganises, now),
                getStatsMatchsParticipant(matricule, participations, now),
                getStatsPaiements(participations)
        );
    }

    private Optional<MeNextMatchDto> getProchainMatch(String matricule,
                                                     List<MatchPadel> matchsOrganises,
                                                     List<Participation> participations,
                                                     LocalDateTime now) {
        return Stream.concat(
                        nullSafe(matchsOrganises).stream(),
                        nullSafe(participations).stream()
                                .map(Participation::getMatch)
                )
                .filter(Objects::nonNull)
                .filter(match -> match.getDateDebut() != null)
                .filter(match -> match.getStatut() != MatchStatut.ANNULE)
                .filter(match -> match.getDateDebut().isAfter(now))
                .filter(match -> joueurConcerne(matricule, match))
                .distinct()
                .min(Comparator
                        .comparing(MatchPadel::getDateDebut)
                        .thenComparing(MatchPadel::getId, Comparator.nullsLast(Long::compareTo)))
                .map(match -> toNextMatchDto(matricule, match));
    }

    private MeMatchRoleStatsDto getStatsMatchsOrganisateur(List<MatchPadel> matchsOrganises,
                                                           LocalDateTime now) {
        return new MeMatchRoleStatsDto(
                countJoues(nullSafe(matchsOrganises), now),
                countAVenir(nullSafe(matchsOrganises), now),
                countAnnules(nullSafe(matchsOrganises))
        );
    }

    private MeMatchRoleStatsDto getStatsMatchsParticipant(String matricule,
                                                          List<Participation> participations,
                                                          LocalDateTime now) {
        List<MatchPadel> matchsCommeParticipant = nullSafe(participations).stream()
                .map(Participation::getMatch)
                .filter(Objects::nonNull)
                .filter(match -> !isOrganisateur(matricule, match))
                .distinct()
                .toList();

        return new MeMatchRoleStatsDto(
                countJoues(matchsCommeParticipant, now),
                countAVenir(matchsCommeParticipant, now),
                countAnnules(matchsCommeParticipant)
        );
    }

    private MePaymentStatsDto getStatsPaiements(List<Participation> participations) {
        long participationsPayees = 0;
        long participationsAPayer = 0;
        BigDecimal montantNetPaye = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantRembourse = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Participation participation : nullSafe(participations)) {
            MatchPadel match = participation.getMatch();

            montantRembourse = montantRembourse
                    .add(sumPaiementsParticipationByType(participation, TypePaiement.REMBOURSEMENT).abs())
                    .setScale(2, RoundingMode.HALF_UP);

            if (match == null || match.getStatut() == MatchStatut.ANNULE) {
                continue;
            }

            BigDecimal montantPaye = sumPaiementsParticipation(participation);
            if (montantPaye.compareTo(PART_JOUEUR) >= 0) {
                participationsPayees++;
            } else {
                participationsAPayer++;
            }

            montantNetPaye = montantNetPaye
                    .add(montantPaye.max(BigDecimal.ZERO).min(PART_JOUEUR))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new MePaymentStatsDto(
                participationsPayees,
                participationsAPayer,
                montantNetPaye,
                montantRembourse
        );
    }

    private long countJoues(List<MatchPadel> matchs, LocalDateTime now) {
        return matchs.stream()
                .filter(match -> match.getStatut() != MatchStatut.ANNULE)
                .filter(match -> match.getDateDebut() != null && match.getDateDebut().isBefore(now))
                .count();
    }

    private long countAVenir(List<MatchPadel> matchs, LocalDateTime now) {
        return matchs.stream()
                .filter(match -> match.getStatut() != MatchStatut.ANNULE)
                .filter(match -> match.getDateDebut() != null && match.getDateDebut().isAfter(now))
                .count();
    }

    private long countAnnules(List<MatchPadel> matchs) {
        return matchs.stream()
                .filter(match -> match.getStatut() == MatchStatut.ANNULE)
                .count();
    }

    private boolean joueurConcerne(String matricule, MatchPadel match) {
        return isOrganisateur(matricule, match)
                || nullSafe(match.getParticipations()).stream()
                .map(Participation::getJoueur)
                .filter(Objects::nonNull)
                .anyMatch(joueur -> matricule.equals(joueur.getMatricule()));
    }

    private boolean isOrganisateur(String matricule, MatchPadel match) {
        return match.getOrganisateur() != null
                && matricule.equals(match.getOrganisateur().getMatricule());
    }

    private MeNextMatchDto toNextMatchDto(String matricule, MatchPadel match) {
        return new MeNextMatchDto(
                match.getId(),
                match.getDateDebut(),
                match.getTerrain().getSite().getNom(),
                match.getTerrain().getNom(),
                isOrganisateur(matricule, match)
                        ? PlayerMatchRoleDto.ORGANISATEUR
                        : PlayerMatchRoleDto.PARTICIPANT
        );
    }

    private BigDecimal sumPaiementsParticipation(Participation participation) {
        return nullSafe(participation.getPaiements()).stream()
                .map(Paiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumPaiementsParticipationByType(Participation participation, TypePaiement type) {
        return nullSafe(participation.getPaiements()).stream()
                .filter(paiement -> paiement.getType() == type)
                .map(Paiement::getMontant)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private <T> List<T> nullSafe(List<T> items) {
        return items == null ? List.of() : items;
    }
}
