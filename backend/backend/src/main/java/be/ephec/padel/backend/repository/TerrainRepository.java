package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerrainRepository extends JpaRepository<Terrain, Long> {

    boolean existsByNomAndSiteId(String nom, Long siteId);

    List<Terrain> findBySite_Id(Long siteId);
}

