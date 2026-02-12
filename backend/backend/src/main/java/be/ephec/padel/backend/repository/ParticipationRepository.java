package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByMatchId(Long matchId);
    List<Participation> findByJoueurMatricule(String matricule);
    boolean existsByMatchIdAndJoueurMatricule(Long matchId, String matricule);
}
