package be.ephec.padel.backend.dto.response;

public class ParticipantDto {

    private String matricule;
    private String nom;

    public ParticipantDto(String matricule, String nom) {
        this.matricule = matricule;
        this.nom = nom;
    }

    public String getMatricule() {
        return matricule;
    }

    public String getNom() {
        return nom;
    }
}