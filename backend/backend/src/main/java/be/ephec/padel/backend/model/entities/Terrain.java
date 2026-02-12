package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "terrain")
public class Terrain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;


    public Terrain() {
    }

    public Terrain(String nom, Site site) {
        this.nom = nom;
        this.site = site;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Site getSite() {
        return site;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}

