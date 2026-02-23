package be.ephec.padel.backend.model.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "joueur_matricule"})
)
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchPadel match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;

    @OneToMany(mappedBy = "participation",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Paiement> paiements = new ArrayList<>();

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

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setMatch(MatchPadel match) {
        this.match = match;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }

    public void addPaiement(Paiement paiement) {
        paiements.add(paiement);
        paiement.setParticipation(this);
    }

    public void removePaiement(Paiement paiement) {
        paiements.remove(paiement);
        paiement.setParticipation(null);
    }
}
