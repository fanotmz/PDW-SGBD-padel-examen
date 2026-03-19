package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.MatchPadel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPadelRepository extends JpaRepository<MatchPadel, Long> {

    @Query("select m from MatchPadel m where m.terrain.id = :terrainId")
    List<MatchPadel> findByTerrainId(@Param("terrainId") Long terrainId);
    List<MatchPadel> findByDateDebutBetween(LocalDateTime start, LocalDateTime end);
    @Query("""
        select m
        from MatchPadel m
        where m.terrain.id = :terrainId
          and m.dateDebut between :start and :end
        """)
    List<MatchPadel> findByTerrainIdAndDateDebutBetween(@Param("terrainId") Long terrainId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
    @Query("""
        select (count(m) > 0)
        from MatchPadel m
        where m.terrain.id = :terrainId
          and m.dateDebut between :start and :end
        """)
    boolean existsByTerrainIdAndDateDebutBetween(@Param("terrainId") Long terrainId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
    @Query("""
        select distinct m
        from MatchPadel m
        join fetch m.terrain t
        join fetch t.site
        join fetch m.organisateur o
        left join fetch m.participations p
        where m.id = :id
        """)
    Optional<MatchPadel> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m from MatchPadel m
        left join fetch m.participations p
        left join fetch p.joueur
        where m.id = :id
    """)
    Optional<MatchPadel> findByIdForUpdateWithParticipations(@Param("id") Long id);

    @Query("""
    select distinct m
    from MatchPadel m
    join fetch m.organisateur o
    left join fetch m.participations p
    left join fetch p.joueur pj
    where m.j1TraiteLe is null
      and m.dateDebut >= :from
      and m.dateDebut < :to
""")
    List<MatchPadel> findAtraiterJ1AvecDetails(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Query("""
    select distinct m
    from MatchPadel m
    join fetch m.organisateur o
    left join fetch m.participations p
    left join fetch p.joueur pj
    where m.soldeTraiteLe is null
      and m.dateDebut >= :from
      and m.dateDebut < :to
""")
    List<MatchPadel> findAtraiterDebutMatchAvecDetails(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to);
    @Query("""
    select count(m)
    from MatchPadel m
    where m.dateDebut >= :from
      and m.dateDebut < :to
""")

    long countByDateDebutBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
        select count(m)
        from MatchPadel m
        where m.dateDebut >= :from
          and m.dateDebut < :to
          and m.terrain.site.id = :siteId
    """)
    long countByDateDebutBetweenAndSiteId(LocalDateTime from, LocalDateTime to, Long siteId);
    @Query("""
    select
        m.id as id,
        m.dateDebut as dateDebut,
        s.id as siteId,
        s.nom as siteNom,
        t.id as terrainId,
        t.nom as terrainNom,
        o.matricule as organisateurMatricule,
        count(p.id) as nbParticipants
    from MatchPadel m
    join m.terrain t
    join t.site s
    join m.organisateur o
    left join m.participations p
    where m.visibilite = :visibilite
      and (:from is null or m.dateDebut >= :from)
      and (:to is null or m.dateDebut <= :to)
      and (:siteId is null or s.id = :siteId)
    group by
        m.id, m.dateDebut,
        s.id, s.nom,
        t.id, t.nom,
        o.matricule
    order by m.dateDebut asc
""")
    List<PublicMatchSummaryProjection> findPublicMatchSummaries(
            @Param("visibilite") MatchVisibilite visibilite,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("siteId") Long siteId
    );

}

