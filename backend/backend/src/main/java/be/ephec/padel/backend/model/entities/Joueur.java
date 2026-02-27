package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
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

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;

    @Column(name = "penalite_jusqua")
    private LocalDateTime penaliteJusqua;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

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
        this.solde = BigDecimal.ZERO;
    }

    public Joueur(String matricule, String nom, TypeJoueur type, Site site) {
        this.matricule = matricule;
        this.nom = nom;
        this.type = type;
        this.site = site;
        this.solde = BigDecimal.ZERO;
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

    public BigDecimal getSolde() {
        return solde;
    }

    public Site getSite() {
        return site;
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

    public void setSolde(BigDecimal solde) {
        this.solde = (solde == null) ? BigDecimal.ZERO : solde;
    }

    public void setSite(Site site) {
        this.site = site;
    }
    public LocalDateTime getPenaliteJusqua() {
        return penaliteJusqua;
    }

    public void setPenaliteJusqua(LocalDateTime penaliteJusqua) {
        this.penaliteJusqua = penaliteJusqua;
    }
}
