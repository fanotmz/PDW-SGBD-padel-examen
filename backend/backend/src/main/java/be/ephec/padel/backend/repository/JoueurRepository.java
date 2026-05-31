package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface JoueurRepository extends JpaRepository<Joueur, String> {

    Optional<Joueur> findById(String matricule);
    boolean existsById(String matricule);
    Optional<Joueur> findFirstByMatriculeStartingWithOrderByMatriculeDesc(String prefix);
    List<Joueur> findBySite_Id(Long siteId);

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
    @Query("""
        select coalesce(sum(j.solde), 0)
        from Joueur j
        where j.solde > 0
          and j.site.id = :siteId
    """)
    BigDecimal sumDettesBySiteId(Long siteId);

    @Query("""
        select count(j)
        from Joueur j
        where j.solde > 0
          and j.site.id = :siteId
    """)
    long countJoueursEnDetteBySiteId(Long siteId);
}
