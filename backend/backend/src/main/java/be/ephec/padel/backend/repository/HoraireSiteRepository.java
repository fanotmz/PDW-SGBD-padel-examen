package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.HoraireSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoraireSiteRepository extends JpaRepository<HoraireSite, Long> {

    Optional<HoraireSite> findBySiteIdAndAnnee(Long siteId, Integer annee);

    boolean existsBySiteIdAndAnnee(Long siteId, Integer annee);

    boolean existsBySiteIdAndAnneeAndIdNot(Long siteId, Integer annee, Long id);

    List<HoraireSite> findBySiteIdOrderByAnneeAsc(Long siteId);
}