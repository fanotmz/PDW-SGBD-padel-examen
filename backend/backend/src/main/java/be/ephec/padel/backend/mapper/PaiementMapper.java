package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.response.PaiementDto;
import be.ephec.padel.backend.model.entities.Paiement;

public final class PaiementMapper {

    private PaiementMapper() {}

    public static PaiementDto toDto(Paiement paiement) {
        if (paiement == null) return null;

        Long participationId = paiement.getParticipation() != null
                ? paiement.getParticipation().getId()
                : null;

        return new PaiementDto(
                paiement.getId(),
                participationId,
                paiement.getMontant(),
                paiement.getDatePaiement()
        );
    }
}