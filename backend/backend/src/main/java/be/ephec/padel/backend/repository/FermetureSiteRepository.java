package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.FermetureSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FermetureSiteRepository extends JpaRepository<FermetureSite, Long> {

    List<FermetureSite> findBySiteIdOrderByDateAscDateDebutAsc(Long siteId);

    Optional<FermetureSite> findByIdAndSiteId(Long id, Long siteId);

    boolean existsBySiteIdAndDate(Long siteId, LocalDate date);

    boolean existsBySiteIdAndDateDebutAndDateFin(Long siteId, LocalDate dateDebut, LocalDate dateFin);

    boolean existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
            Long siteId,
            LocalDate date1,
            LocalDate date2
    );

    boolean existsBySiteIdAndDateBetween(Long siteId, LocalDate start, LocalDate end);

    @Query("""
           select count(f) > 0
           from FermetureSite f
           where f.site.id = :siteId
             and f.dateDebut is not null
             and f.dateFin is not null
             and f.dateDebut <= :newEnd
             and f.dateFin >= :newStart
           """)
    boolean existsPeriodeChevauchante(@Param("siteId") Long siteId,
                                      @Param("newStart") LocalDate newStart,
                                      @Param("newEnd") LocalDate newEnd);
}