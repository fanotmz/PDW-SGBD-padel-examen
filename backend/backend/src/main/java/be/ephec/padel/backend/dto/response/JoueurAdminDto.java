package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.TypeJoueur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class JoueurAdminDto {

    private final String matricule;
    private final String nom;
    private final TypeJoueur type;
    private final BigDecimal solde;
    private final LocalDateTime penaliteJusqua;

    public JoueurAdminDto(String matricule,
                          String nom,
                          TypeJoueur type,
                          BigDecimal solde,
                          LocalDateTime penaliteJusqua) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
        this.solde = solde;
        this.penaliteJusqua = penaliteJusqua;
    }

    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public TypeJoueur getType() { return type; }
    public BigDecimal getSolde() { return solde; }
    public LocalDateTime getPenaliteJusqua() { return penaliteJusqua; }
}
