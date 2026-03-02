package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.TypeJoueur;

import java.math.BigDecimal;

public class JoueurAdminDto {

    private final String matricule;
    private final String nom;
    private final TypeJoueur type;
    private final BigDecimal solde;

    public JoueurAdminDto(String matricule, String nom, TypeJoueur type, BigDecimal solde) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
        this.solde = solde;
    }

    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public TypeJoueur getType() { return type; }
    public BigDecimal getSolde() { return solde; }
}