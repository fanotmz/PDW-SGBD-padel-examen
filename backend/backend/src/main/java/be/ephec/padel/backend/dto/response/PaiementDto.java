package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaiementDto {

    private Long id;
    private Long participationId;
    private BigDecimal montant;
    private LocalDateTime datePaiement;

    public PaiementDto(Long id, Long participationId,
                       BigDecimal montant,
                       LocalDateTime datePaiement) {
        this.id = id;
        this.participationId = participationId;
        this.montant = montant;
        this.datePaiement = datePaiement;
    }

    public Long getId() { return id; }
    public Long getParticipationId() { return participationId; }
    public BigDecimal getMontant() { return montant; }
    public LocalDateTime getDatePaiement() { return datePaiement; }
}