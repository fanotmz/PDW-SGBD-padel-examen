package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.request.CreateTerrainRequest;
import be.ephec.padel.backend.dto.response.TerrainDto;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;

public final class TerrainMapper {

    private TerrainMapper() {}

    public static TerrainDto toDto(Terrain terrain) {
        if (terrain == null) return null;

        Long siteId = terrain.getSite() != null
                ? terrain.getSite().getId()
                : null;

        return new TerrainDto(
                terrain.getId(),
                terrain.getNom(),
                siteId
        );
    }

    /**
     * Ici on suppose que le service a déjà récupéré le Site
     * à partir du siteId (lookup + validation).
     */
    public static Terrain toEntity(CreateTerrainRequest req, Site site) {
        if (req == null) return null;

        return new Terrain(
                req.getNom(),
                site
        );
    }
}