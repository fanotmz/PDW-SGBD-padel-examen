package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.MatchVisibilite;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_padel")
public class MatchPadel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "terrain_id", nullable = false)
    private Terrain terrain;

    @ManyToOne(optional = false)
    @JoinColumn(name = "organisateur_matricule", nullable = false)
    private Joueur organisateur;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchVisibilite visibilite;

    public MatchPadel() {
    }

    public MatchPadel(Terrain terrain, Joueur organisateur, LocalDateTime dateDebut, MatchVisibilite visibilite) {
        this.terrain = terrain;
        this.organisateur = organisateur;
        this.dateDebut = dateDebut;
        this.visibilite = visibilite;
    }

    public Long getId() {
        return id;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public Joueur getOrganisateur() {
        return organisateur;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public MatchVisibilite getVisibilite() {
        return visibilite;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public void setOrganisateur(Joueur organisateur) {
        this.organisateur = organisateur;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setVisibilite(MatchVisibilite visibilite) {
        this.visibilite = visibilite;
    }
}
