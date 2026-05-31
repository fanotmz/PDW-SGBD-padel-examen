package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class MatchDetailDto {

    private Long id;

    private LocalDate dateDebut;
    private LocalTime heureDebut;

    private Long siteId;
    private String siteNom;

    private Long terrainId;
    private String terrainNom;

    private String organisateurMatricule;
    private String organisateurNom;

    private MatchVisibilite visibilite;
    private MatchStatut statut;

    private int nbParticipants;
    private int placesRestantes;
    private boolean complet;
    private boolean peutAjouterJoueurPrive;
    private boolean peutAnnuler;

    private BigDecimal montantTotal;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private BigDecimal montantRembourse;

    private List<ParticipantDto> participants;

    public MatchDetailDto(Long id,
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
                          boolean complet,
                          boolean peutAjouterJoueurPrive,
                          boolean peutAnnuler,
                          BigDecimal montantTotal,
                          BigDecimal montantPaye,
                          BigDecimal resteAPayer,
                          BigDecimal montantRembourse,
                          List<ParticipantDto> participants) {
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
        this.complet = complet;
        this.peutAjouterJoueurPrive = peutAjouterJoueurPrive;
        this.peutAnnuler = peutAnnuler;
        this.montantTotal = montantTotal;
        this.montantPaye = montantPaye;
        this.resteAPayer = resteAPayer;
        this.montantRembourse = montantRembourse;
        this.participants = participants;
    }

    public MatchDetailDto(Long id,
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
                          boolean complet,
                          boolean peutAjouterJoueurPrive,
                          BigDecimal montantTotal,
                          BigDecimal montantPaye,
                          BigDecimal resteAPayer,
                          BigDecimal montantRembourse,
                          List<ParticipantDto> participants) {
        this(
                id,
                dateDebut,
                heureDebut,
                siteId,
                siteNom,
                terrainId,
                terrainNom,
                organisateurMatricule,
                organisateurNom,
                visibilite,
                statut,
                nbParticipants,
                placesRestantes,
                complet,
                peutAjouterJoueurPrive,
                false,
                montantTotal,
                montantPaye,
                resteAPayer,
                montantRembourse,
                participants
        );
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public Long getSiteId() {
        return siteId;
    }

    public String getSiteNom() {
        return siteNom;
    }

    public Long getTerrainId() {
        return terrainId;
    }

    public String getTerrainNom() {
        return terrainNom;
    }

    public String getOrganisateurMatricule() {
        return organisateurMatricule;
    }

    public String getOrganisateurNom() {
        return organisateurNom;
    }

    public MatchVisibilite getVisibilite() {
        return visibilite;
    }

    public MatchStatut getStatut() {
        return statut;
    }

    public int getNbParticipants() {
        return nbParticipants;
    }

    public int getPlacesRestantes() {
        return placesRestantes;
    }

    public boolean isComplet() {
        return complet;
    }

    public boolean isPeutAjouterJoueurPrive() {
        return peutAjouterJoueurPrive;
    }

    public boolean isPeutAnnuler() {
        return peutAnnuler;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public BigDecimal getResteAPayer() {
        return resteAPayer;
    }

    public BigDecimal getMontantRembourse() {
        return montantRembourse;
    }

    public List<ParticipantDto> getParticipants() {
        return participants;
    }
}
