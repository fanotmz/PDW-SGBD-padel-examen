package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerrainRepository extends JpaRepository<Terrain, Long> {

    boolean existsByNomAndSiteId(String nom, Long siteId);
    Optional<Terrain> findByNomAndSiteId(String nom, Long siteId);

    List<Terrain> findBySite_Id(Long siteId);
}

