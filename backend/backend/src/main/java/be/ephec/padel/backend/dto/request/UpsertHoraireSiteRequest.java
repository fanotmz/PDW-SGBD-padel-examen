package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class UpsertHoraireSiteRequest {

    @NotNull
    private Integer annee;

    @NotNull
    private LocalTime heureOuverture;

    @NotNull
    private LocalTime heureFermeture;

    public UpsertHoraireSiteRequest() {
    }

    public Integer getAnnee() {
        return annee;
    }

    public LocalTime getHeureOuverture() {
        return heureOuverture;
    }

    public LocalTime getHeureFermeture() {
        return heureFermeture;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public void setHeureOuverture(LocalTime heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public void setHeureFermeture(LocalTime heureFermeture) {
        this.heureFermeture = heureFermeture;
    }
}