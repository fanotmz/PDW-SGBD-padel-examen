package be.ephec.padel.backend.service.model;

import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class OpenDebtLine {

    private final Long sourceMouvementId;
    private final Long participationId;
    private final Long matchId;
    private final OrigineMouvementSoldeType origineType;
    private final LocalDateTime dateMouvement;
    private final BigDecimal montantInitial;
    private BigDecimal montantImpute;
    private BigDecimal montantRestant;
    private final boolean legacy;
    private final String description;

    public OpenDebtLine(Long sourceMouvementId,
                        Long participationId,
                        Long matchId,
                        OrigineMouvementSoldeType origineType,
                        LocalDateTime dateMouvement,
                        BigDecimal montantInitial,
                        boolean legacy,
                        String description) {
        this.sourceMouvementId = sourceMouvementId;
        this.participationId = participationId;
        this.matchId = matchId;
        this.origineType = origineType;
        this.dateMouvement = dateMouvement;
        this.montantInitial = scale(montantInitial);
        this.montantImpute = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.montantRestant = scale(montantInitial);
        this.legacy = legacy;
        this.description = description;
    }

    public Long getSourceMouvementId() {
        return sourceMouvementId;
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

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public BigDecimal getMontantInitial() {
        return montantInitial;
    }

    public BigDecimal getMontantImpute() {
        return montantImpute;
    }

    public BigDecimal getMontantRestant() {
        return montantRestant;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOpen() {
        return montantRestant.signum() > 0;
    }

    public boolean cibleParticipation(Long targetParticipationId) {
        return participationId != null && participationId.equals(targetParticipationId);
    }

    public void imputer(BigDecimal montant) {
        BigDecimal montantScale = scale(montant);
        if (montantScale.signum() <= 0) {
            return;
        }

        BigDecimal imputation = montantScale.min(montantRestant);
        this.montantImpute = this.montantImpute.add(imputation).setScale(2, RoundingMode.HALF_UP);
        this.montantRestant = this.montantRestant.subtract(imputation).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }
}
