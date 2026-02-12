package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.MouvementSolde;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MouvementSoldeRepository extends JpaRepository<MouvementSolde, Long> {
    List<MouvementSolde> findByJoueurMatriculeOrderByDateMouvementDesc(String matricule);
}
