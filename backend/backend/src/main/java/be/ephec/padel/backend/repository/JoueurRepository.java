package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, String> {

    Optional<Joueur> findById(String matricule); // déjà fourni par JpaRepository
    boolean existsById(String matricule);

    @Query("""
        select coalesce(sum(j.solde), 0)
        from Joueur j
        where j.solde > 0
    """)
    BigDecimal sumDettes();

    @Query("""
        select count(j)
        from Joueur j
        where j.solde > 0
    """)
    long countJoueursEnDette();
}