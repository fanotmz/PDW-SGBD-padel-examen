package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AddPlayerToPrivateMatchRequest {

    @NotBlank
    private String organisateurMatricule;

    @NotBlank
    private String joueurMatriculeAAjouter;

    public String getOrganisateurMatricule() { return organisateurMatricule; }
    public void setOrganisateurMatricule(String organisateurMatricule) { this.organisateurMatricule = organisateurMatricule; }

    public String getJoueurMatriculeAAjouter() { return joueurMatriculeAAjouter; }
    public void setJoueurMatriculeAAjouter(String joueurMatriculeAAjouter) { this.joueurMatriculeAAjouter = joueurMatriculeAAjouter; }
}
