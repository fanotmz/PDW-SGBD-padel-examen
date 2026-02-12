package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private String ville;

    @OneToMany(mappedBy = "site",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Terrain> terrains = new ArrayList<>();

    public Site() {
    }

    public Site(String nom, String ville) {
        this.nom = nom;
        this.ville = ville;
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

    public List<Terrain> getTerrains() {
        return terrains;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void addTerrain(Terrain terrain) {
        terrains.add(terrain);
        terrain.setSite(this);
    }

    public void removeTerrain(Terrain terrain) {
        terrains.remove(terrain);
        terrain.setSite(null);
    }
}
