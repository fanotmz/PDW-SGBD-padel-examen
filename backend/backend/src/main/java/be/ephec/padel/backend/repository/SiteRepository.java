package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Site;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Long> {
    boolean existsByNom(String nom);
}
