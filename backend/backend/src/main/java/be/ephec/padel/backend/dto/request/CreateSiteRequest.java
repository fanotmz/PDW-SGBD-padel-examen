package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class CreateSiteRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String ville;

    @NotNull
    private Integer annee;

    @NotNull
    private LocalTime heureOuverture;

    @NotNull
    private LocalTime heureFermeture;

    public CreateSiteRequest() {
    }

    public String getNom() {
        return nom;
    }

    public String getVille() {
        return ville;
    }

    public Integer getAnnee() {
        return annee;
    }

    public LocalTime getHeureOuverture() {
        return heureOuverture;
    }

    public LocalTime getHeureFermeture() {
        return heureFermeture;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public void setHeureOuverture(LocalTime heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public void setHeureFermeture(LocalTime heureFermeture) {
        this.heureFermeture = heureFermeture;
    }
}