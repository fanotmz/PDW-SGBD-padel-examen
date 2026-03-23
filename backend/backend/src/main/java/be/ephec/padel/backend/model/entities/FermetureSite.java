package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "fermeture_site")
public class FermetureSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "date_fermeture")
    private LocalDate date;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "motif")
    private String motif;

    public FermetureSite() {
    }

    public FermetureSite(Site site, LocalDate date, LocalDate dateDebut, LocalDate dateFin, String motif) {
        this.site = site;
        this.date = date;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
    }

    public Long getId() { return id; }
    public Site getSite() { return site; }
    public LocalDate getDate() { return date; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public String getMotif() { return motif; }

    public void setSite(Site site) { this.site = site; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public void setMotif(String motif) { this.motif = motif; }
}