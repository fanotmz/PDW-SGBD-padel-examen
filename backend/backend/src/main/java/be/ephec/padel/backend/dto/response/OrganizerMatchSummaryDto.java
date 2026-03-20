package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.dto.enums.MatchTemporalStatusDto;
import be.ephec.padel.backend.model.enums.MatchVisibilite;

import java.time.LocalDateTime;

public record OrganizerMatchSummaryDto(
        Long id,
        LocalDateTime dateDebut,
        Long siteId,
        String siteNom,
        Long terrainId,
        String terrainNom,
        MatchVisibilite visibilite,
        int nbParticipants,
        int placesRestantes,
        boolean complet,
        MatchTemporalStatusDto statutTemporel,
        Integer joursAvantMatch,
        boolean risquePenaliteJ1
) {
}