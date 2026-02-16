package be.ephec.padel.backend.repository;
import java.util.Optional;

import be.ephec.padel.backend.model.entities.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByMatch_Id(Long matchId);
    List<Participation> findByJoueurMatricule(String matricule);

    boolean existsByMatch_IdAndJoueur_Matricule(Long matchId, String joueurMatricule);

    Optional<Participation> findByMatch_IdAndJoueur_Matricule(Long matchId, String matricule);

    int countByMatch_Id(Long matchId);
}
