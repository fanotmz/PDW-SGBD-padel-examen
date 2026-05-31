package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class PublicMatchSummaryDto {

    private Long id;
    private LocalDate dateDebut;
    private LocalTime heureDebut;


    private Long siteId;
    private String siteNom;

    private Long terrainId;
    private String terrainNom;

    private String organisateurMatricule;

    private int nbParticipants;
    private int placesRestantes;
    private boolean complet;

    private BigDecimal montantParJoueur;

    public PublicMatchSummaryDto(Long id,
                                 LocalDate dateDebut,
                                 LocalTime heureDebut,
                                 Long siteId,
                                 String siteNom,
                                 Long terrainId,
                                 String terrainNom,
                                 String organisateurMatricule,
                                 int nbParticipants,
                                 int placesRestantes,
                                 boolean complet,
                                 BigDecimal montantParJoueur) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.heureDebut = heureDebut;
        this.siteId = siteId;
        this.siteNom = siteNom;
        this.terrainId = terrainId;
        this.terrainNom = terrainNom;
        this.organisateurMatricule = organisateurMatricule;
        this.nbParticipants = nbParticipants;
        this.placesRestantes = placesRestantes;
        this.complet = complet;
        this.montantParJoueur = montantParJoueur;
    }

    public Long getId() { return id; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public Long getSiteId() { return siteId; }
    public String getSiteNom() { return siteNom; }
    public Long getTerrainId() { return terrainId; }
    public String getTerrainNom() { return terrainNom; }
    public String getOrganisateurMatricule() { return organisateurMatricule; }
    public int getNbParticipants() { return nbParticipants; }
    public int getPlacesRestantes() { return placesRestantes; }
    public boolean isComplet() { return complet; }
    public BigDecimal getMontantParJoueur() { return montantParJoueur; }
}