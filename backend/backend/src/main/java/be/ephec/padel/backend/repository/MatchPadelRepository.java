package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.MatchPadel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPadelRepository extends JpaRepository<MatchPadel, Long> {
    List<MatchPadel> findByTerrainId(Long terrainId);
    List<MatchPadel> findByDateDebutBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
select distinct m
from MatchPadel m
join fetch m.terrain t
join fetch t.site
join fetch m.organisateur o
left join fetch m.participations p
where m.id = :id
""")
    Optional<MatchPadel> findByIdWithDetails(@Param("id") Long id);
}

