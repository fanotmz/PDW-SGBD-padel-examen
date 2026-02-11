package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

@Entity
@Table(
        name = "participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "joueur_matricule"})
)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchPadel match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;

    public Participation() {
    }

    public Participation(MatchPadel match, Joueur joueur) {
        this.match = match;
        this.joueur = joueur;
    }

    public Long getId() {
        return id;
    }

    public MatchPadel getMatch() {
        return match;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public void setMatch(MatchPadel match) {
        this.match = match;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }
}
