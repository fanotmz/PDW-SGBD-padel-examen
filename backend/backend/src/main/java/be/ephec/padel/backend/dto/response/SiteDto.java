package be.ephec.padel.backend.dto.response;

import java.time.DayOfWeek;
import java.util.Set;

public class SiteDto {
    private Long id;
    private String nom;
    private String ville;
    private Set<DayOfWeek> joursFermeture;

    public SiteDto(Long id,
                   String nom,
                   String ville,
                   Set<DayOfWeek> joursFermeture) {
        this.id = id;
        this.nom = nom;
        this.ville = ville;
        this.joursFermeture = joursFermeture;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }
    public Set<DayOfWeek> getJoursFermeture() { return joursFermeture; }
}
