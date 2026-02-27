package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class JoinPublicMatchRequest {

    @NotBlank(message = "Matricule du joueur obligatoire")
    private String joueurMatricule;

    public String getJoueurMatricule() { return joueurMatricule; }
    public void setJoueurMatricule(String joueurMatricule) { this.joueurMatricule = joueurMatricule; }
}