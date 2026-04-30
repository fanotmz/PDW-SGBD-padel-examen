package be.ephec.padel.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RegularisationsResponseDto(
        BigDecimal totalTracable,
        List<RegularisationDto> items
) {
}
