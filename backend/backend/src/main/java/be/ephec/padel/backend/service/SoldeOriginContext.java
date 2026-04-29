package be.ephec.padel.backend.service;

import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;

public final class SoldeOriginContext {

    private final OrigineMouvementSoldeType origineType;
    private final Long participationId;
    private final Long matchId;
    private final String description;

    public SoldeOriginContext(OrigineMouvementSoldeType origineType,
                              Long participationId,
                              Long matchId,
                              String description) {
        this.origineType = origineType == null ? OrigineMouvementSoldeType.LEGACY : origineType;
        this.participationId = participationId;
        this.matchId = matchId;
        this.description = description;
    }

    public static SoldeOriginContext legacy() {
        return new SoldeOriginContext(OrigineMouvementSoldeType.LEGACY, null, null, null);
    }

    public OrigineMouvementSoldeType getOrigineType() {
        return origineType;
    }

    public Long getParticipationId() {
        return participationId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public String getDescription() {
        return description;
    }
}
