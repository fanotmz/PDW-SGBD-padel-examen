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


    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvement type;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;

    public MouvementSolde() {
    }

    public MouvementSolde(LocalDateTime dateMouvement,
                          BigDecimal montant,
                          TypeMouvement type,
                          Joueur joueur) {
        this.dateMouvement = dateMouvement;
        this.montant = montant;
        this.type = type;
        this.joueur = joueur;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public TypeMouvement getType() {
        return type;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public void setDateMouvement(LocalDateTime dateMouvement) {
        this.dateMouvement = dateMouvement;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public void setType(TypeMouvement type) {
        this.type = type;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }
}
