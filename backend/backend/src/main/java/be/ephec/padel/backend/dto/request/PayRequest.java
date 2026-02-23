package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PayRequest {

    @NotNull(message = "Montant obligatoire")
    @Positive(message = "Le montant doit être > 0")
    private BigDecimal montant;

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}