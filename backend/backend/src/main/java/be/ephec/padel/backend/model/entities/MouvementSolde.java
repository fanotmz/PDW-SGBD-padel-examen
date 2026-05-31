package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
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

    @Column(name = "participation_id")
    private Long participationId;

    @Column(name = "match_id")
    private Long matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origine_type", nullable = false)
    private OrigineMouvementSoldeType origineType = OrigineMouvementSoldeType.LEGACY;

    @Column
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_matricule", nullable = false)
    private Joueur joueur;

    public MouvementSolde() {
    }

    public MouvementSolde(LocalDateTime dateMouvement,
                          BigDecimal montant,
                          TypeMouvement type,
                          Joueur joueur) {
        this(dateMouvement, montant, type, joueur, null, null, OrigineMouvementSoldeType.LEGACY, null);
    }

    public MouvementSolde(LocalDateTime dateMouvement,
                          BigDecimal montant,
                          TypeMouvement type,
                          Joueur joueur,
                          Long participationId,
                          Long matchId,
                          OrigineMouvementSoldeType origineType,
                          String description) {
        this.dateMouvement = dateMouvement;
        this.montant = montant;
        this.type = type;
        this.joueur = joueur;
        this.participationId = participationId;
        this.matchId = matchId;
        this.origineType = origineType == null ? OrigineMouvementSoldeType.LEGACY : origineType;
        this.description = description;
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

    public Long getParticipationId() {
        return participationId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public OrigineMouvementSoldeType getOrigineType() {
        return origineType;
    }

    public String getDescription() {
        return description;
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

    public void setParticipationId(Long participationId) {
        this.participationId = participationId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public void setOrigineType(OrigineMouvementSoldeType origineType) {
        this.origineType = origineType == null ? OrigineMouvementSoldeType.LEGACY : origineType;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
