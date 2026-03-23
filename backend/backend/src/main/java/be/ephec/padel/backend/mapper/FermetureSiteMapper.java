package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.FermetureSiteDto;
import be.ephec.padel.backend.model.entities.FermetureSite;

public final class FermetureSiteMapper {

    private FermetureSiteMapper() {
    }

    public static FermetureSiteDto toDto(FermetureSite f) {
        if (f == null) {
            return null;
        }

        Long siteId = null;
        if (f.getSite() != null) {
            siteId = f.getSite().getId();
        }

        return new FermetureSiteDto(
                f.getId(),
                siteId,
                f.getDate(),
                f.getDateDebut(),
                f.getDateFin(),
                f.getMotif()
        );
    }
}