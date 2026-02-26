package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.FermetureGlobaleDto;
import be.ephec.padel.backend.model.entities.FermetureGlobale;

public final class FermetureGlobaleMapper {

    private FermetureGlobaleMapper() {}

    public static FermetureGlobaleDto toDto(FermetureGlobale f) {
        if (f == null) return null;
        return new FermetureGlobaleDto(f.getId(), f.getDate(), f.getMotif());
    }
}