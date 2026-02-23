package be.ephec.padel.backend.dto.response;
import be.ephec.padel.backend.model.enums.TypeJoueur;

import java.math.BigDecimal;

public class JoueurDto {

    private String matricule;
    private String nom;
    private TypeJoueur type;
    private Long siteId;
    private BigDecimal solde;

    public JoueurDto(String matricule, String nom, TypeJoueur type, Long siteId, BigDecimal solde) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
        this.siteId = siteId;
        this.solde = solde;
    }

    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public TypeJoueur getType() { return type; }
    public Long getSiteId() { return siteId; }
    public BigDecimal getSolde() { return solde; }
}
