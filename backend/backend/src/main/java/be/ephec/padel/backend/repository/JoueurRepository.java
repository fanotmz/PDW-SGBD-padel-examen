package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, String> {
    Optional<Joueur> findById(String matricule); // déjà fourni par JpaRepository
    boolean existsById(String matricule);
}

