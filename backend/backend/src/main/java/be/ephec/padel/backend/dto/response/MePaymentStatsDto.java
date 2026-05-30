package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;

public record MePaymentStatsDto(
        long participationsPayees,
        long participationsAPayer,
        BigDecimal montantNetPaye,
        BigDecimal montantRembourse
) {
}
