package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.MatchVisibilite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MatchDto {

    private Long id;
    private Long terrainId;
    private String terrainNom;
    private Long siteId;

    private String organisateurMatricule;
    private LocalDateTime dateDebut;
    private MatchVisibilite visibilite;

    private int nbParticipants;

    // ✅ nouveaux champs
    private BigDecimal montantTotal;   // 60.00
    private BigDecimal montantPaye;    // somme des paiements
    public BigDecimal resteAPayer;    // montantTotal - montantPaye (min 0)

    public MatchDto(Long id,
                    Long terrainId,
                    String terrainNom,
                    Long siteId,
                    String organisateurMatricule,
                    LocalDateTime dateDebut,
                    MatchVisibilite visibilite,
                    int nbParticipants,
                    BigDecimal montantTotal,
                    BigDecimal montantPaye,
                    BigDecimal resteAPayer) {

        this.id = id;
        this.terrainId = terrainId;
        this.terrainNom = terrainNom;
        this.siteId = siteId;
        this.organisateurMatricule = organisateurMatricule;
        this.dateDebut = dateDebut;
        this.visibilite = visibilite;
        this.nbParticipants = nbParticipants;

        this.montantTotal = montantTotal;
        this.montantPaye = montantPaye;
        this.resteAPayer = resteAPayer;
    }

    public Long getId() { return id; }
    public Long getTerrainId() { return terrainId; }
    public String getTerrainNom() { return terrainNom; }
    public Long getSiteId() { return siteId; }
    public String getOrganisateurMatricule() { return organisateurMatricule; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public MatchVisibilite getVisibilite() { return visibilite; }
    public int getNbParticipants() { return nbParticipants; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public BigDecimal getResteAPayer() { return resteAPayer; }
}