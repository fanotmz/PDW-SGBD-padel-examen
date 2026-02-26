package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

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

    // ----------------------------
    // Issue #30 : horaires + fermetures site
    // ----------------------------

    // TEMP (dev) : nullable=true pour éviter l’échec Hibernate sur SQL Server quand la table `site` contient déjà des lignes.
// SQL Server n’autorise pas l’ajout d’une colonne NOT NULL sans DEFAULT sur une table non vide.
// À remplacer par une vraie migration (Flyway/Liquibase) :
// 1) ajouter colonne nullable, 2) backfill des valeurs, 3) passer NOT NULL (+ éventuellement DEFAULT).
    @Column(name = "heure_ouverture", nullable = true)
    private LocalTime heureOuverture;

    @Column(name = "heure_fermeture", nullable = true)
    private LocalTime heureFermeture;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "site_jour_fermeture",
            joinColumns = @JoinColumn(name = "site_id")
    )
    @Column(name = "jour", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> joursFermeture = new HashSet<>();

    // ----------------------------
    // Relation terrains
    // ----------------------------

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
    public Site(String nom, String ville, LocalTime heureOuverture, LocalTime heureFermeture) {
        this.nom = nom;
        this.ville = ville;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
    }

    // ---- Getters ----

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

    public LocalTime getHeureOuverture() {
        return heureOuverture;
    }

    public LocalTime getHeureFermeture() {
        return heureFermeture;
    }

    public Set<DayOfWeek> getJoursFermeture() {
        return joursFermeture;
    }

    // ---- Setters ----

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public void setHeureOuverture(LocalTime heureOuverture) {
        this.heureOuverture = heureOuverture;
    }

    public void setHeureFermeture(LocalTime heureFermeture) {
        this.heureFermeture = heureFermeture;
    }

    public void setJoursFermeture(Set<DayOfWeek> joursFermeture) {
        this.joursFermeture.clear();
        if (joursFermeture != null) {
            this.joursFermeture.addAll(joursFermeture);
        }
    }

    // ---- Helpers ----

    public void addTerrain(Terrain terrain) {
        terrains.add(terrain);
        terrain.setSite(this);
    }

    public void removeTerrain(Terrain terrain) {
        terrains.remove(terrain);
        terrain.setSite(null);
    }
}