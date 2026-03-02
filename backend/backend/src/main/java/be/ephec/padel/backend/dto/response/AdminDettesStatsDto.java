package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;

public class AdminDettesStatsDto {

    private final BigDecimal detteTotale;
    private final long nbJoueursEnDette;

    public AdminDettesStatsDto(BigDecimal detteTotale, long nbJoueursEnDette) {
        this.detteTotale = detteTotale;
        this.nbJoueursEnDette = nbJoueursEnDette;
    }

    public BigDecimal getDetteTotale() {
        return detteTotale;
    }

    public long getNbJoueursEnDette() {
        return nbJoueursEnDette;
    }
}