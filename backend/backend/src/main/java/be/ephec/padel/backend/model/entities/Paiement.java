package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.TypePaiement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participation_id", nullable = false)
    private Participation participation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypePaiement type = TypePaiement.ENCAISSEMENT;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    public Paiement() {
    }

    public Paiement(Participation participation, BigDecimal montant, LocalDateTime datePaiement) {
        this(participation, montant, TypePaiement.ENCAISSEMENT, datePaiement);
    }

    public Paiement(Participation participation,
                    BigDecimal montant,
                    TypePaiement type,
                    LocalDateTime datePaiement) {
        this.participation = participation;
        this.montant = montant;
        this.type = type;
        this.datePaiement = datePaiement;
    }

    public Long getId() {
        return id;
    }

    public Participation getParticipation() {
        return participation;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public LocalDateTime getDatePaiement() {
        return datePaiement;
    }

    public TypePaiement getType() {
        return type;
    }

    public void setParticipation(Participation participation) {
        this.participation = participation;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public void setType(TypePaiement type) {
        this.type = type;
    }

    public void setDatePaiement(LocalDateTime datePaiement) {
        this.datePaiement = datePaiement;
    }
}

