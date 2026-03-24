package be.ephec.padel.backend.dto.response;

import java.time.LocalTime;

public class HoraireSiteDto {

    private Long id;
    private Long siteId;
    private Integer annee;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;

    public HoraireSiteDto(Long id, Long siteId, Integer annee,
                          LocalTime heureOuverture, LocalTime heureFermeture) {
        this.id = id;
        this.siteId = siteId;
        this.annee = annee;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
    }

    public Long getId() {
        return id;
    }

    public Long getSiteId() {
        return siteId;
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
}