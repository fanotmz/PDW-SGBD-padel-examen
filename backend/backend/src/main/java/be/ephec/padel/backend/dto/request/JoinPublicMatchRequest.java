package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class JoinPublicMatchRequest {

    @NotBlank(message = "Matricule du joueur obligatoire")
    private String joueurMatricule;

    @NotNull(message = "Montant obligatoire")
    @Positive(message = "Le montant doit être > 0")
    private BigDecimal montant;

    public String getJoueurMatricule() { return joueurMatricule; }
    public void setJoueurMatricule(String joueurMatricule) { this.joueurMatricule = joueurMatricule; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}