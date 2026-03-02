package be.ephec.padel.backend.dto.response;

import java.time.LocalDate;

public class AdminMatchsStatsDto {

    private final long nbMatchs;
    private final LocalDate from;
    private final LocalDate to;

    public AdminMatchsStatsDto(long nbMatchs, LocalDate from, LocalDate to) {
        this.nbMatchs = nbMatchs;
        this.from = from;
        this.to = to;
    }

    public long getNbMatchs() {
        return nbMatchs;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }
}