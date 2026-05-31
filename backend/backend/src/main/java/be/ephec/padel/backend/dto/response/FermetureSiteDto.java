package be.ephec.padel.backend.dto.response;

import java.time.LocalDate;

public class FermetureSiteDto {

    private Long id;
    private Long siteId;
    private LocalDate date;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motif;

    public FermetureSiteDto(Long id,
                            Long siteId,
                            LocalDate date,
                            LocalDate dateDebut,
                            LocalDate dateFin,
                            String motif) {
        this.id = id;
        this.siteId = siteId;
        this.date = date;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
    }

    public Long getId() {
        return id;
    }

    public Long getSiteId() {
        return siteId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public String getMotif() {
        return motif;
    }
}