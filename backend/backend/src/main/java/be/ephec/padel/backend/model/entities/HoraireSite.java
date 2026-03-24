package be.ephec.padel.backend.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalTime;

@Entity
@Table(
        name = "horaire_site",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_horaire_site_site_annee",
                        columnNames = {"site_id", "annee"}
                )
        }
)
public class HoraireSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private Integer annee;

    @Column(name = "heure_ouverture", nullable = false)
    private LocalTime heureOuverture;

    @Column(name = "heure_fermeture", nullable = false)
    private LocalTime heureFermeture;

    public HoraireSite() {
    }

    public HoraireSite(Site site, Integer annee, LocalTime heureOuverture, LocalTime heureFermeture) {
        this.site = site;
        this.annee = annee;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
    }

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setSite(Site site) {
        this.site = site;
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