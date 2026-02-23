package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.ParticipationDto;
import be.ephec.padel.backend.model.entities.Participation;

public final class ParticipationMapper {

    private ParticipationMapper() {}

    public static ParticipationDto toDto(Participation participation) {
        if (participation == null) return null;

        Long matchId = participation.getMatch() != null
                ? participation.getMatch().getId()
                : null;

        String joueurMatricule = participation.getJoueur() != null
                ? participation.getJoueur().getMatricule()
                : null;

        return new ParticipationDto(
                participation.getId(),
                matchId,
                joueurMatricule
        );
    }
}