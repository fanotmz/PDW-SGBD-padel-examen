package be.ephec.padel.backend.dto.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public class SiteDto {
    private Long id;
    private String nom;
    private String ville;

    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Set<DayOfWeek> joursFermeture;

    public SiteDto(Long id,
                   String nom,
                   String ville,
                   LocalTime heureOuverture,
                   LocalTime heureFermeture,
                   Set<DayOfWeek> joursFermeture) {
        this.id = id;
        this.nom = nom;
        this.ville = ville;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
        this.joursFermeture = joursFermeture;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }

    public LocalTime getHeureOuverture() { return heureOuverture; }
    public LocalTime getHeureFermeture() { return heureFermeture; }
    public Set<DayOfWeek> getJoursFermeture() { return joursFermeture; }
}