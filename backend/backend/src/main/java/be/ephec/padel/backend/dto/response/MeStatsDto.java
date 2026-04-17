package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;

public class MeStatsDto {

    private final long nbMatchsParticipes;
    private final long nbMatchsOrganises;
    private final long nbMatchsPasses;
    private final long nbMatchsFuturs;
    private final long nbMatchsAnnules;
    private final BigDecimal montantTotalPaye;
    private final BigDecimal detteActuelle;

    public MeStatsDto(long nbMatchsParticipes,
                      long nbMatchsOrganises,
                      long nbMatchsPasses,
                      long nbMatchsFuturs,
                      long nbMatchsAnnules,
                      BigDecimal montantTotalPaye,
                      BigDecimal detteActuelle) {
        this.nbMatchsParticipes = nbMatchsParticipes;
        this.nbMatchsOrganises = nbMatchsOrganises;
        this.nbMatchsPasses = nbMatchsPasses;
        this.nbMatchsFuturs = nbMatchsFuturs;
        this.nbMatchsAnnules = nbMatchsAnnules;
        this.montantTotalPaye = montantTotalPaye;
        this.detteActuelle = detteActuelle;
    }

    public long getNbMatchsParticipes() {
        return nbMatchsParticipes;
    }

    public long getNbMatchsOrganises() {
        return nbMatchsOrganises;
    }

    public long getNbMatchsPasses() {
        return nbMatchsPasses;
    }

    public long getNbMatchsFuturs() {
        return nbMatchsFuturs;
    }

    public long getNbMatchsAnnules() {
        return nbMatchsAnnules;
    }

    public BigDecimal getMontantTotalPaye() {
        return montantTotalPaye;
    }

    public BigDecimal getDetteActuelle() {
        return detteActuelle;
    }
}
