package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.FermetureGlobale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface FermetureGlobaleRepository extends JpaRepository<FermetureGlobale, Long> {
    boolean existsByDate(LocalDate date);
}