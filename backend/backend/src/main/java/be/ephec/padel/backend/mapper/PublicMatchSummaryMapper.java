package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.PublicMatchSummaryDto;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;

import java.math.BigDecimal;

public final class PublicMatchSummaryMapper {

    private static final int CAPACITE_MATCH = 4;

    private PublicMatchSummaryMapper() {
    }

    public static PublicMatchSummaryDto toDto(PublicMatchSummaryProjection p, BigDecimal montantParJoueur) {
        int nbParticipants = (int) p.getNbParticipants();
        int placesRestantes = Math.max(0, CAPACITE_MATCH - nbParticipants);
        boolean complet = nbParticipants >= CAPACITE_MATCH;

        return new PublicMatchSummaryDto(
                p.getId(),
                p.getDateDebut().toLocalDate(),
                p.getDateDebut().toLocalTime().withSecond(0).withNano(0),
                p.getSiteId(),
                p.getSiteNom(),
                p.getTerrainId(),
                p.getTerrainNom(),
                p.getOrganisateurMatricule(),
                nbParticipants,
                placesRestantes,
                complet,
                montantParJoueur
        );

    }
}