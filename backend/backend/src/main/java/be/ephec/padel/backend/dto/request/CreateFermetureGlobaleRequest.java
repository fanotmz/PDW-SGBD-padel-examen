package be.ephec.padel.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateFermetureGlobaleRequest {

    @NotNull
    private LocalDate date;

    private String motif;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}