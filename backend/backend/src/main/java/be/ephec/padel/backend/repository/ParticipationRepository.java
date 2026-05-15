package be.ephec.padel.backend.repository;
import java.util.Optional;

import be.ephec.padel.backend.model.entities.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    List<Participation> findByMatch_Id(Long matchId);
    List<Participation> findByJoueur_Matricule(String matricule);
    List<Participation> findByJoueur_MatriculeOrderByMatch_DateDebutAsc(String matricule);
    long countByJoueur_Matricule(String matricule);
    boolean existsByMatch_IdAndJoueur_Matricule(Long matchId, String joueurMatricule);

    Optional<Participation> findByMatch_IdAndJoueur_Matricule(Long matchId, String matricule);

    @Query("""
            select distinct p
            from Participation p
            join fetch p.joueur
            join fetch p.match m
            join fetch m.terrain t
            join fetch t.site s
            where p.id in :ids
            """)
    List<Participation> findByIdInWithDetails(@Param("ids") List<Long> ids);

    @Query("""
            select p
            from Participation p
            join fetch p.joueur
            join fetch p.match m
            join fetch m.organisateur
            join fetch m.terrain t
            join fetch t.site
            where p.id = :id
            """)
    Optional<Participation> findByIdWithDetails(@Param("id") Long id);

    int countByMatch_Id(Long matchId);
}
