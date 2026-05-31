package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.dto.enums.PlayerMatchRoleDto;

import java.time.LocalDateTime;

public record MeNextMatchDto(
        Long id,
        LocalDateTime dateDebut,
        String siteNom,
        String terrainNom,
        PlayerMatchRoleDto roleJoueur
) {
}
