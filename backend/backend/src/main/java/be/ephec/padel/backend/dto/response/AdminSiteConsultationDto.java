package be.ephec.padel.backend.dto.response;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public class AdminSiteConsultationDto {

    private Long id;
    private String nom;
    private String ville;
    private Set<DayOfWeek> joursFermeture;
    private List<TerrainDto> terrains;
    private List<HoraireSiteDto> horaires;

    public AdminSiteConsultationDto(Long id,
                                    String nom,
                                    String ville,
                                    Set<DayOfWeek> joursFermeture,
                                    List<TerrainDto> terrains,
                                    List<HoraireSiteDto> horaires) {
        this.id = id;
        this.nom = nom;
        this.ville = ville;
        this.joursFermeture = joursFermeture;
        this.terrains = terrains;
        this.horaires = horaires;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getVille() {
        return ville;
    }

    public Set<DayOfWeek> getJoursFermeture() {
        return joursFermeture;
    }

    public List<TerrainDto> getTerrains() {
        return terrains;
    }

    public List<HoraireSiteDto> getHoraires() {
        return horaires;
    }
}
