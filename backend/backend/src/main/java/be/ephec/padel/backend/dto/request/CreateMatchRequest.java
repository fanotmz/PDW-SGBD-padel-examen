package be.ephec.padel.backend.dto.request;

import be.ephec.padel.backend.model.enums.MatchVisibilite;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class CreateMatchRequest {

    @NotNull
    @Positive
    private Long terrainId;

    @NotBlank
    private String organisateurMatricule;

    @NotNull(message = "Date de début obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDateTime dateDebut;

    @NotNull
    private MatchVisibilite visibilite;

    public Long getTerrainId() { return terrainId; }
    public void setTerrainId(Long terrainId) { this.terrainId = terrainId; }

    public String getOrganisateurMatricule() { return organisateurMatricule; }
    public void setOrganisateurMatricule(String organisateurMatricule) { this.organisateurMatricule = organisateurMatricule; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public MatchVisibilite getVisibilite() { return visibilite; }
    public void setVisibilite(MatchVisibilite visibilite) { this.visibilite = visibilite; }
}
