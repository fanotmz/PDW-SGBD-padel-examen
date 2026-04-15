package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;

public class PendingRegistrationDto {

    private Long userId;
    private String username;
    private String nomDemande;
    private TypeJoueur typeAbonnementDemande;
    private Long siteIdDemande;
    private String siteNomDemande;
    private UserStatus status;

    public PendingRegistrationDto(Long userId,
                                  String username,
                                  String nomDemande,
                                  TypeJoueur typeAbonnementDemande,
                                  Long siteIdDemande,
                                  String siteNomDemande,
                                  UserStatus status) {
        this.userId = userId;
        this.username = username;
        this.nomDemande = nomDemande;
        this.typeAbonnementDemande = typeAbonnementDemande;
        this.siteIdDemande = siteIdDemande;
        this.siteNomDemande = siteNomDemande;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNomDemande() {
        return nomDemande;
    }

    public TypeJoueur getTypeAbonnementDemande() {
        return typeAbonnementDemande;
    }

    public Long getSiteIdDemande() {
        return siteIdDemande;
    }

    public String getSiteNomDemande() {
        return siteNomDemande;
    }

    public UserStatus getStatus() {
        return status;
    }
}
