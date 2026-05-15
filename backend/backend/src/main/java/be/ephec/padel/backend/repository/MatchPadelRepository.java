package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.repository.projection.PublicMatchSummaryProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchPadelRepository extends JpaRepository<MatchPadel, Long> {

    Optional<MatchPadel> findByTerrain_IdAndDateDebut(Long terrainId, LocalDateTime dateDebut);

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
        select distinct m from MatchPadel m
        join fetch m.terrain t
        join fetch t.site
        join fetch m.organisateur
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
      and m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE
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
      and m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE
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
      and m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE
""")
    long countByDateDebutBetween(LocalDateTime from, LocalDateTime to);

    long countByOrganisateur_Matricule(String matricule);

    @Query("""
        select count(m)
        from MatchPadel m
        where m.dateDebut >= :from
          and m.dateDebut < :to
          and m.terrain.site.id = :siteId
          and m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE
    """)
    long countByDateDebutBetweenAndSiteId(LocalDateTime from, LocalDateTime to, Long siteId);

    @Query("""
    select m
    from MatchPadel m
    where m.terrain.site.id = :siteId
      and m.statut = :statut
      and m.dateDebut > :now
      and m.dateDebut >= :from
      and m.dateDebut < :to
""")
    List<MatchPadel> findPlannedFutureMatchesBySiteIdAndDateDebutBetween(
            @Param("siteId") Long siteId,
            @Param("statut") MatchStatut statut,
            @Param("now") LocalDateTime now,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    select m
    from MatchPadel m
    where m.statut = :statut
      and m.dateDebut > :now
      and m.dateDebut >= :from
      and m.dateDebut < :to
""")
    List<MatchPadel> findPlannedFutureMatchesByDateDebutBetween(
            @Param("statut") MatchStatut statut,
            @Param("now") LocalDateTime now,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        select distinct m
        from MatchPadel m
        join fetch m.terrain t
        join fetch t.site s
        join fetch m.organisateur o
        left join fetch m.participations p
        where s.id = :siteId
          and (:statut is null or m.statut = :statut)
          and (:visibilite is null or m.visibilite = :visibilite)
          and (:from is null or m.dateDebut >= :from)
          and (:to is null or m.dateDebut < :to)
          and (:futureOnly = false or (m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE and m.dateDebut > :now))
          and (:historyOnly = false or (m.dateDebut < :now or m.statut = be.ephec.padel.backend.model.enums.MatchStatut.ANNULE))
        order by m.dateDebut asc
    """)
    List<MatchPadel> findAdminSiteMatches(
            @Param("siteId") Long siteId,
            @Param("statut") MatchStatut statut,
            @Param("visibilite") MatchVisibilite visibilite,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("futureOnly") boolean futureOnly,
            @Param("historyOnly") boolean historyOnly,
            @Param("now") LocalDateTime now
    );

    @Query("""
        select count(distinct m.id)
        from MatchPadel m
        where m.statut <> be.ephec.padel.backend.model.enums.MatchStatut.ANNULE
          and m.dateDebut < :now
          and (
              m.organisateur.matricule = :matricule
              or exists (
                  select 1
                  from Participation p
                  where p.match = m
                    and p.joueur.matricule = :matricule
              )
          )
    """)
    long countDistinctLinkedPastMatchesByMatricule(
            @Param("matricule") String matricule,
            @Param("now") LocalDateTime now
    );

    @Query("""
        select count(distinct m.id)
        from MatchPadel m
        where m.statut <> be.ephec.padel.backend.model.enums.MatchStatut.ANNULE
          and m.dateDebut >= :now
          and (
              m.organisateur.matricule = :matricule
              or exists (
                  select 1
                  from Participation p
                  where p.match = m
                    and p.joueur.matricule = :matricule
              )
          )
    """)
    long countDistinctLinkedFutureMatchesByMatricule(
            @Param("matricule") String matricule,
            @Param("now") LocalDateTime now
    );

    @Query("""
        select count(distinct m.id)
        from MatchPadel m
        where m.statut = be.ephec.padel.backend.model.enums.MatchStatut.ANNULE
          and (
              m.organisateur.matricule = :matricule
              or exists (
                  select 1
                  from Participation p
                  where p.match = m
                    and p.joueur.matricule = :matricule
              )
          )
    """)
    long countDistinctLinkedCancelledMatchesByMatricule(@Param("matricule") String matricule);

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
      and m.statut = be.ephec.padel.backend.model.enums.MatchStatut.PLANIFIE
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
    @Query("""
    select distinct m
    from MatchPadel m
    join fetch m.terrain t
    join fetch t.site
    join fetch m.organisateur o
    left join fetch m.participations p
    where o.matricule = :matricule
    order by m.dateDebut asc
""")
    List<MatchPadel> findOrganizedMatchesWithDetailsByMatricule(@Param("matricule") String matricule);
}

