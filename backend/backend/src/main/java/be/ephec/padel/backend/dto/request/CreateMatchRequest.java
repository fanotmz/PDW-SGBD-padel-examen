package be.ephec.padel.backend.dto.request;

import be.ephec.padel.backend.model.enums.MatchVisibilite;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class CreateMatchRequest {

    @NotNull
    @Positive
    private Long terrainId;

    @NotNull(message = "La date de debut est obligatoire")
    @Future(message = "La date de debut doit être dans le futur")
    private LocalDateTime dateDebut;

    @NotNull
    private MatchVisibilite visibilite;

    public Long getTerrainId() {
        return terrainId;
    }

    public void setTerrainId(Long terrainId) {
        this.terrainId = terrainId;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public MatchVisibilite getVisibilite() {
        return visibilite;
    }

    public void setVisibilite(MatchVisibilite visibilite) {
        this.visibilite = visibilite;
    }
}
