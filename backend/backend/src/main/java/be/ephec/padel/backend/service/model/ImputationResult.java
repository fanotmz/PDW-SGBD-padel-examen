package be.ephec.padel.backend.service.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

public class ImputationResult {

    private final List<OpenDebtLine> openDebtLines;
    private final BigDecimal totalTrackedOpenAmount;

    public ImputationResult(List<OpenDebtLine> openDebtLines, BigDecimal totalTrackedOpenAmount) {
        this.openDebtLines = List.copyOf(openDebtLines);
        this.totalTrackedOpenAmount = (totalTrackedOpenAmount == null ? BigDecimal.ZERO : totalTrackedOpenAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public List<OpenDebtLine> getOpenDebtLines() {
        return Collections.unmodifiableList(openDebtLines);
    }

    public BigDecimal getTotalTrackedOpenAmount() {
        return totalTrackedOpenAmount;
    }
}
