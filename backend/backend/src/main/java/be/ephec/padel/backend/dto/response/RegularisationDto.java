package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegularisationDto(
        Long participationId,
        Long matchId,
        LocalDateTime dateMatch,
        String siteNom,
        String terrainNom,
        MatchVisibilite visibilite,
        PlayerMatchRoleDto roleJoueur,
        OrigineMouvementSoldeType origineType,
        BigDecimal montantInitial,
        BigDecimal montantDejaPaye,
        BigDecimal montantRestant,
        String description,
        boolean payable
) {
}
