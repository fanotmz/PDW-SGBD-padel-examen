package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.MatchDto;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;

import java.math.BigDecimal;

public final class MatchMapper {

    private MatchMapper() {}

    public static MatchDto toDtoBase(MatchPadel match) {
        return toDtoComplet(match, null, null, null);
    }

    public static MatchDto toDtoComplet(MatchPadel match,
                                        BigDecimal montantTotal,
                                        BigDecimal montantPaye,
                                        BigDecimal resteAPayer) {
        if (match == null) return null;

        Terrain terrain = match.getTerrain();
        Long terrainId = terrain != null ? terrain.getId() : null;
        String terrainNom = terrain != null ? terrain.getNom() : null;

        Site site = (terrain != null) ? terrain.getSite() : null;
        Long siteId = site != null ? site.getId() : null;

        String organisateurMatricule = match.getOrganisateur() != null
                ? match.getOrganisateur().getMatricule()
                : null;

        int nbParticipants = match.getParticipations() != null
                ? match.getParticipations().size()
                : 0;

        return new MatchDto(
                match.getId(),
                terrainId,
                terrainNom,
                siteId,
                organisateurMatricule,
                match.getDateDebut(),
                match.getVisibilite(),
                match.getStatut(),
                nbParticipants,
                montantTotal,
                montantPaye,
                resteAPayer
        );
    }
}
