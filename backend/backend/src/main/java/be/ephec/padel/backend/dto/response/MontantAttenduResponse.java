package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;

public class MontantAttenduResponse {

    private BigDecimal montantAttendu;

    public MontantAttenduResponse() {
    }

    public MontantAttenduResponse(BigDecimal montantAttendu) {
        this.montantAttendu = montantAttendu;
    }

    public BigDecimal getMontantAttendu() {
        return montantAttendu;
    }

    public void setMontantAttendu(BigDecimal montantAttendu) {
        this.montantAttendu = montantAttendu;
    }
}