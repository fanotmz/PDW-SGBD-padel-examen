package be.ephec.padel.backend.repository.projection;

import java.time.LocalDateTime;

public interface PublicMatchSummaryProjection {

    Long getId();
    LocalDateTime getDateDebut();

    Long getSiteId();
    String getSiteNom();

    Long getTerrainId();
    String getTerrainNom();

    String getOrganisateurMatricule();

    long getNbParticipants();
}