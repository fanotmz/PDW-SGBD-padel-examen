package be.ephec.padel.backend.dto.response;

public record MeStatsDto(
        MeNextMatchDto prochainMatch,
        MeMatchRoleStatsDto matchsCommeOrganisateur,
        MeMatchRoleStatsDto matchsCommeParticipant,
        MePaymentStatsDto paiements
) {
}
