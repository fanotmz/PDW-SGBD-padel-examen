package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.MatchDetailDto;
import be.ephec.padel.backend.dto.response.ParticipantDto;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;

import java.math.BigDecimal;
import java.util.List;

public final class MatchDetailMapper {

    private static final int CAPACITE_MATCH = 4;

    private MatchDetailMapper() {
    }

    public static MatchDetailDto toDto(MatchPadel m,
                                       BigDecimal montantTotal,
                                       BigDecimal montantPaye,
                                       BigDecimal resteAPayer,
                                       BigDecimal montantRembourse) {

        int nbParticipants = (m.getParticipations() != null) ? m.getParticipations().size() : 0;
        int placesRestantes = Math.max(0, CAPACITE_MATCH - nbParticipants);
        boolean complet = nbParticipants >= CAPACITE_MATCH;

        List<ParticipantDto> participants = (m.getParticipations() == null)
                ? List.of()
                : m.getParticipations().stream()
                .map(MatchDetailMapper::toParticipantDto)
                .toList();

        return new MatchDetailDto(
                m.getId(),
                m.getDateDebut().toLocalDate(),
                m.getDateDebut().toLocalTime().withSecond(0).withNano(0),
                m.getTerrain().getSite().getId(),
                m.getTerrain().getSite().getNom(),
                m.getTerrain().getId(),
                m.getTerrain().getNom(),
                m.getOrganisateur().getMatricule(),
                m.getOrganisateur().getNom(),
                m.getVisibilite(),
                m.getStatut(),
                nbParticipants,
                placesRestantes,
                complet,
                montantTotal,
                montantPaye,
                resteAPayer,
                montantRembourse,
                participants
        );
    }

    private static ParticipantDto toParticipantDto(Participation p) {
        return new ParticipantDto(
                p.getJoueur().getMatricule(),
                p.getJoueur().getNom()
        );
    }
}
