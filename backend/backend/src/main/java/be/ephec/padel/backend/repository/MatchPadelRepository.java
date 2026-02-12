package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.MatchPadel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchPadelRepository extends JpaRepository<MatchPadel, Long> {
    List<MatchPadel> findByTerrainId(Long terrainId);
    List<MatchPadel> findByDateDebutBetween(LocalDateTime start, LocalDateTime end);
}
