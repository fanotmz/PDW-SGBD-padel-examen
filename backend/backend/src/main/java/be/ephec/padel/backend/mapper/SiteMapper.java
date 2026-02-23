package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.request.CreateSiteRequest;
import be.ephec.padel.backend.dto.response.SiteDto;
import be.ephec.padel.backend.model.entities.Site;

public final class SiteMapper {

    private SiteMapper() {}

    public static SiteDto toDto(Site site) {
        if (site == null) return null;

        return new SiteDto(
                site.getId(),
                site.getNom(),
                site.getVille()
        );
    }

    public static Site toEntity(CreateSiteRequest req) {
        if (req == null) return null;

        return new Site(
                req.getNom(),
                req.getVille()
        );
    }
}