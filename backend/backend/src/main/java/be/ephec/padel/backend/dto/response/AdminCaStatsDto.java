package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdminCaStatsDto {

    private final BigDecimal caTotal;
    private final LocalDate from;
    private final LocalDate to;

    public AdminCaStatsDto(BigDecimal caTotal, LocalDate from, LocalDate to) {
        this.caTotal = caTotal;
        this.from = from;
        this.to = to;
    }

    public BigDecimal getCaTotal() {
        return caTotal;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }
}