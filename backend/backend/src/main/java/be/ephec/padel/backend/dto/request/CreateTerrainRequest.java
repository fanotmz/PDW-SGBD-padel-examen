package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateTerrainRequest {

    @NotBlank
    private String nom;

    @NotNull
    private Long siteId;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
}
