package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.TypeMouvement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvement_solde")
public class MouvementSolde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvement type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    public MouvementSolde() {}

    public MouvementSolde(Joueur joueur, TypeMouvement type, BigDecimal montant, LocalDateTime dateMouvement) {
        this.joueur = joueur;
        this.type = type;
        this.montant = montant;
        this.dateMouvement = dateMouvement;
    }

    public Long getId() { return id; }
    public Joueur getJoueur() { return joueur; }
    public TypeMouvement getType() { return type; }
    public BigDecimal getMontant() { return montant; }
    public LocalDateTime getDateMouvement() { return dateMouvement; }

    public void setJoueur(Joueur joueur) { this.joueur = joueur; }
    public void setType(TypeMouvement type) { this.type = type; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }
}
