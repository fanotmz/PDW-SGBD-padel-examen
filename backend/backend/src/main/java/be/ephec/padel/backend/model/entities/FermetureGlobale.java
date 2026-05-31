package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "fermeture_globale",
        uniqueConstraints = @UniqueConstraint(columnNames = {"date_fermeture"})
)
public class FermetureGlobale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_fermeture", nullable = false)
    private LocalDate date;

    @Column(name = "motif")
    private String motif;

    public FermetureGlobale() {}

    public FermetureGlobale(LocalDate date, String motif) {
        this.date = date;
        this.motif = motif;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getMotif() { return motif; }

    public void setDate(LocalDate date) { this.date = date; }
    public void setMotif(String motif) { this.motif = motif; }
}