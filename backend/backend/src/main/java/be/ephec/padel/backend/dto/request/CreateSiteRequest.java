package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateSiteRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String ville;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
}
