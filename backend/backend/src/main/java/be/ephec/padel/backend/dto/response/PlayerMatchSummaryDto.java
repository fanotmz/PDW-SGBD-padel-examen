package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlayerMatchSummaryDto(
        Long id,
        LocalDateTime dateDebut,
        Long siteId,
        String siteNom,
        Long terrainId,
        String terrainNom,
        MatchVisibilite visibilite,
        MatchStatut statut,
        PlayerMatchRoleDto roleJoueur,
        MatchTemporalStatusDto statutTemporel,
        Integer joursAvantMatch,
        boolean paiementJoueurEffectue,
        Long participationId,
        BigDecimal montantPayeJoueur,
        BigDecimal montantRestantJoueur,
        boolean peutPayerParticipation
) {
}
