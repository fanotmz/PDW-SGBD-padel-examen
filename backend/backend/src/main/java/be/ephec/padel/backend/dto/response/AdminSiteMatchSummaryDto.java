package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;

import java.time.LocalDate;
import java.time.LocalTime;

public class AdminSiteMatchSummaryDto {

    private final Long id;
    private final LocalDate dateDebut;
    private final LocalTime heureDebut;
    private final Long siteId;
    private final String siteNom;
    private final Long terrainId;
    private final String terrainNom;
    private final String organisateurMatricule;
    private final String organisateurNom;
    private final MatchVisibilite visibilite;
    private final MatchStatut statut;
    private final int nbParticipants;
    private final int placesRestantes;
    private final boolean peutAnnuler;
    private final boolean passe;

    public AdminSiteMatchSummaryDto(Long id,
                                    LocalDate dateDebut,
                                    LocalTime heureDebut,
                                    Long siteId,
                                    String siteNom,
                                    Long terrainId,
                                    String terrainNom,
                                    String organisateurMatricule,
                                    String organisateurNom,
                                    MatchVisibilite visibilite,
                                    MatchStatut statut,
                                    int nbParticipants,
                                    int placesRestantes,
                                    boolean peutAnnuler,
                                    boolean passe) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.heureDebut = heureDebut;
        this.siteId = siteId;
        this.siteNom = siteNom;
        this.terrainId = terrainId;
        this.terrainNom = terrainNom;
        this.organisateurMatricule = organisateurMatricule;
        this.organisateurNom = organisateurNom;
        this.visibilite = visibilite;
        this.statut = statut;
        this.nbParticipants = nbParticipants;
        this.placesRestantes = placesRestantes;
        this.peutAnnuler = peutAnnuler;
        this.passe = passe;
    }

    public Long getId() { return id; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalTime getHeureDebut() { return heureDebut; }
    public Long getSiteId() { return siteId; }
    public String getSiteNom() { return siteNom; }
    public Long getTerrainId() { return terrainId; }
    public String getTerrainNom() { return terrainNom; }
    public String getOrganisateurMatricule() { return organisateurMatricule; }
    public String getOrganisateurNom() { return organisateurNom; }
    public MatchVisibilite getVisibilite() { return visibilite; }
    public MatchStatut getStatut() { return statut; }
    public int getNbParticipants() { return nbParticipants; }
    public int getPlacesRestantes() { return placesRestantes; }
    public boolean isPeutAnnuler() { return peutAnnuler; }
    public boolean isPasse() { return passe; }
}
