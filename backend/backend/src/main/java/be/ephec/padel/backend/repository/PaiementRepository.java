package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByParticipation_Id(Long participationId);
    List<Paiement> findByParticipation_Joueur_Matricule(String matricule);
    List<Paiement> findByParticipation_Match_Id(Long matchId);

    @Query("""
        select coalesce(sum(p.montant), 0)
        from Paiement p
        where p.participation.id = :participationId
    """)
    BigDecimal sumMontantByParticipationId(@Param("participationId") Long participationId);

    @Query("""
        select coalesce(sum(pa.montant), 0)
        from Paiement pa
        join pa.participation part
        where part.match.id = :matchId
    """)
    BigDecimal sumMontantByMatchId(@Param("matchId") Long matchId);
}