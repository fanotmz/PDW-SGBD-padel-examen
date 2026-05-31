package be.ephec.padel.backend.dto.response;

import java.time.LocalDate;

public class FermetureGlobaleDto {

    private Long id;
    private LocalDate date;
    private String motif;

    public FermetureGlobaleDto(Long id, LocalDate date, String motif) {
        this.id = id;
        this.date = date;
        this.motif = motif;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getMotif() { return motif; }
}