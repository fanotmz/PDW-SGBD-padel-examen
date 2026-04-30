package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.dto.response.RegularisationDto;
import be.ephec.padel.backend.dto.response.RegularisationsResponseDto;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.security.CurrentUserFacade;
import be.ephec.padel.backend.service.model.ImputationResult;
import be.ephec.padel.backend.service.model.OpenDebtLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RegularisationService {

    private final CurrentUserFacade currentUserFacade;
    private final SoldeImputationService soldeImputationService;
    private final ParticipationRepository participationRepository;

    public RegularisationService(CurrentUserFacade currentUserFacade,
                                 SoldeImputationService soldeImputationService,
                                 ParticipationRepository participationRepository) {
        this.currentUserFacade = currentUserFacade;
        this.soldeImputationService = soldeImputationService;
        this.participationRepository = participationRepository;
    }

    public RegularisationsResponseDto getCurrentUserRegularisations() {
        Joueur joueur = currentUserFacade.getCurrentJoueur();
        ImputationResult imputationResult = soldeImputationService.reconstruirePourJoueur(joueur.getMatricule());

        List<OpenDebtLine> openDebtLines = imputationResult.getOpenDebtLines();
        if (openDebtLines.isEmpty()) {
            return new RegularisationsResponseDto(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of()
            );
        }

        List<Long> participationIds = openDebtLines.stream()
                .map(OpenDebtLine::getParticipationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Participation> participationById = new LinkedHashMap<>();
        participationRepository.findByIdInWithDetails(participationIds)
                .forEach(participation -> participationById.put(participation.getId(), participation));

        List<RegularisationDto> items = openDebtLines.stream()
                .filter(line -> line.getMontantRestant().signum() > 0)
                .map(line -> toRegularisationDto(line, participationById.get(line.getParticipationId()), joueur.getMatricule()))
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator
                        .comparing(RegularisationDto::dateMatch)
                        .thenComparing(RegularisationDto::participationId))
                .toList();

        BigDecimal totalTracable = items.stream()
                .map(RegularisationDto::montantRestant)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new RegularisationsResponseDto(totalTracable, items);
    }

    private RegularisationDto toRegularisationDto(OpenDebtLine line,
                                                  Participation participation,
                                                  String joueurCourantMatricule) {
        if (line == null || participation == null || participation.getMatch() == null) {
            return null;
        }

        MatchPadel match = participation.getMatch();
        PlayerMatchRoleDto roleJoueur =
                match.getOrganisateur() != null
                        && match.getOrganisateur().getMatricule().equals(joueurCourantMatricule)
                        ? PlayerMatchRoleDto.ORGANISATEUR
                        : PlayerMatchRoleDto.PARTICIPANT;

        BigDecimal montantInitial = scale(line.getMontantInitial());
        BigDecimal montantRestant = scale(line.getMontantRestant());
        BigDecimal montantDejaPaye = montantInitial.subtract(montantRestant).setScale(2, RoundingMode.HALF_UP);
        boolean payable = match.getStatut() != MatchStatut.ANNULE && montantRestant.signum() > 0;

        return new RegularisationDto(
                participation.getId(),
                match.getId(),
                match.getDateDebut(),
                match.getTerrain().getSite().getNom(),
                match.getTerrain().getNom(),
                match.getVisibilite(),
                roleJoueur,
                line.getOrigineType(),
                montantInitial,
                montantDejaPaye,
                montantRestant,
                line.getDescription(),
                payable
        );
    }

    private BigDecimal scale(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }
}
