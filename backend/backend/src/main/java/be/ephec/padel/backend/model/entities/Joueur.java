package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "joueur")
public class Joueur {

    @Id
    @Column(length = 10)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeJoueur type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;


    @OneToMany(mappedBy = "joueur")
    private List<Participation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "joueur")
    private List<MouvementSolde> mouvements = new ArrayList<>();

    public Joueur() {
    }

    public Joueur(String matricule, String nom, TypeJoueur type) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
    }

    public String getMatricule() {
        return matricule;
    }

    public String getNom() {
        return nom;
    }

    public TypeJoueur getType() {
        return type;
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public List<MouvementSolde> getMouvements() {
        return mouvements;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setType(TypeJoueur type) {
        this.type = type;
    }
}
