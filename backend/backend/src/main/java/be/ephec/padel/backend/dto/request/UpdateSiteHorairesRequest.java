package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public class UpdateSiteHorairesRequest {

    @NotNull
    private LocalTime heureOuverture;

    @NotNull
    private LocalTime heureFermeture;

    private Set<DayOfWeek> joursFermeture;

    public LocalTime getHeureOuverture() {
        return heureOuverture;
    }

    public void setHeureOuverture(LocalTime heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public LocalTime getHeureFermeture() {
        return heureFermeture;
    }

    public void setHeureFermeture(LocalTime heureFermeture) {
        this.heureFermeture = heureFermeture;
    }

    public Set<DayOfWeek> getJoursFermeture() {
        return joursFermeture;
    }

    public void setJoursFermeture(Set<DayOfWeek> joursFermeture) {
        this.joursFermeture = joursFermeture;
    }
}