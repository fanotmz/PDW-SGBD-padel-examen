package be.ephec.padel.backend.dto.response;
import be.ephec.padel.backend.model.enums.TypeJoueur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class JoueurDto {

    private String matricule;
    private String nom;
    private TypeJoueur type;
    private Long siteId;
    private BigDecimal solde;
    private LocalDateTime penaliteJusqua;

    public JoueurDto(String matricule,
                     String nom,
                     TypeJoueur type,
                     Long siteId,
                     BigDecimal solde,
                     LocalDateTime penaliteJusqua) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
        this.siteId = siteId;
        this.solde = solde;
        this.penaliteJusqua = penaliteJusqua;
    }

    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public TypeJoueur getType() { return type; }
    public Long getSiteId() { return siteId; }
    public BigDecimal getSolde() { return solde; }
    public LocalDateTime getPenaliteJusqua() { return penaliteJusqua; }
}
