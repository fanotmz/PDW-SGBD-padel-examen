package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;

public class RegistrationDecisionResponse {

    private Long userId;
    private String username;
    private UserStatus status;
    private String message;
    private String joueurMatricule;
    private String joueurNom;
    private TypeJoueur joueurType;
    private Long joueurSiteId;

    public RegistrationDecisionResponse(Long userId,
                                        String username,
                                        UserStatus status,
                                        String message,
                                        String joueurMatricule,
                                        String joueurNom,
                                        TypeJoueur joueurType,
                                        Long joueurSiteId) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.message = message;
        this.joueurMatricule = joueurMatricule;
        this.joueurNom = joueurNom;
        this.joueurType = joueurType;
        this.joueurSiteId = joueurSiteId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getJoueurMatricule() {
        return joueurMatricule;
    }

    public String getJoueurNom() {
        return joueurNom;
    }

    public TypeJoueur getJoueurType() {
        return joueurType;
    }

    public Long getJoueurSiteId() {
        return joueurSiteId;
    }
}
