package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.enums.TypePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        select coalesce(sum(p.montant), 0)
        from Paiement p
        where p.participation.id = :participationId
          and p.type = :typePaiement
    """)
    BigDecimal sumMontantByParticipationIdAndType(@Param("participationId") Long participationId,
                                                  @Param("typePaiement") TypePaiement typePaiement);

    @Query("""
        select coalesce(sum(p.montant), 0)
        from Paiement p
        join p.participation pa
        join pa.match m
        where m.id = :matchId
    """)
    BigDecimal sumMontantByMatchId(@Param("matchId") Long matchId);

    @Query("""
        select coalesce(sum(p.montant), 0)
        from Paiement p
        join p.participation pa
        join pa.match m
        where m.id = :matchId
          and p.type = :typePaiement
    """)
    BigDecimal sumMontantByMatchIdAndType(@Param("matchId") Long matchId,
                                          @Param("typePaiement") TypePaiement typePaiement);

    @Query("""
        select coalesce(sum(p.montant), 0)
        from Paiement p
        where p.datePaiement >= :from
          and p.datePaiement < :to
    """)
    BigDecimal sumMontantByDatePaiementBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select coalesce(sum(p.montant), 0)
        from Paiement p
        where p.datePaiement >= :from
          and p.datePaiement < :to
          and p.participation.match.terrain.site.id = :siteId
    """)
    BigDecimal sumMontantByDatePaiementBetweenAndSiteId(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("siteId") Long siteId
    );
}
