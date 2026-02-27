package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.MatchVisibilite;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "match_padel")
public class MatchPadel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terrain_id", nullable = false)
    private Terrain terrain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisateur_matricule", nullable = false)
    private Joueur organisateur;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "j1_traite_le")
    private LocalDateTime j1TraiteLe;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchVisibilite visibilite;

    @Column(name = "solde_traite_le")
    private LocalDateTime soldeTraiteLe;

    @OneToMany(mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();


    public MatchPadel() {
    }

    public MatchPadel(Terrain terrain,
                      Joueur organisateur,
                      LocalDateTime dateDebut,
                      MatchVisibilite visibilite) {
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

    public List<Participation> getParticipations() {
        return participations;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public LocalDateTime getJ1TraiteLe() {
        return j1TraiteLe;
    }

    public void setJ1TraiteLe(LocalDateTime j1TraiteLe) {
        this.j1TraiteLe = j1TraiteLe;
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

    public LocalDateTime getSoldeTraiteLe() {
        return soldeTraiteLe;
    }

    public void setSoldeTraiteLe(LocalDateTime soldeTraiteLe) {
        this.soldeTraiteLe = soldeTraiteLe;
    }
    public void addParticipation(Participation participation) {
        participations.add(participation);
        participation.setMatch(this);
    }

    public void removeParticipation(Participation participation) {
        participations.remove(participation);
        participation.setMatch(null);
    }
}

