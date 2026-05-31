package be.ephec.padel.backend.dto.request;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String nom;

    @NotNull
    private TypeJoueur typeAbonnementDemande;

    private Long siteIdDemande;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypeJoueur getTypeAbonnementDemande() {
        return typeAbonnementDemande;
    }

    public void setTypeAbonnementDemande(TypeJoueur typeAbonnementDemande) {
        this.typeAbonnementDemande = typeAbonnementDemande;
    }

    public Long getSiteIdDemande() {
        return siteIdDemande;
    }

    public void setSiteIdDemande(Long siteIdDemande) {
        this.siteIdDemande = siteIdDemande;
    }
}
