package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FermetureSiteRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;
    @Autowired FermetureSiteRepository fermetureSiteRepository;

    @Test
    void existsBySiteIdAndDate_retourne_true_si_date_unique_existe() {
        Site site = siteRepository.save(new Site("Site A", "Bruxelles"));
        fermetureSiteRepository.save(new FermetureSite(
                site,
                LocalDate.of(2030, 1, 15),
                null,
                null,
                "Jour férié local"
        ));

        assertThat(
                fermetureSiteRepository.existsBySiteIdAndDate(site.getId(), LocalDate.of(2030, 1, 15))
        ).isTrue();

        assertThat(
                fermetureSiteRepository.existsBySiteIdAndDate(site.getId(), LocalDate.of(2030, 1, 16))
        ).isFalse();
    }

    @Test
    void existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual_retourne_true_si_date_dans_periode() {
        Site site = siteRepository.save(new Site("Site A", "Bruxelles"));
        fermetureSiteRepository.save(new FermetureSite(
                site,
                null,
                LocalDate.of(2030, 2, 10),
                LocalDate.of(2030, 2, 15),
                "Travaux"
        ));

        assertThat(
                fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        site.getId(),
                        LocalDate.of(2030, 2, 12),
                        LocalDate.of(2030, 2, 12)
                )
        ).isTrue();

        assertThat(
                fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                        site.getId(),
                        LocalDate.of(2030, 2, 20),
                        LocalDate.of(2030, 2, 20)
                )
        ).isFalse();
    }

    @Test
    void findBySiteIdOrderByDateAscDateDebutAsc_retourne_les_fermetures_du_site() {
        Site siteA = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site siteB = siteRepository.save(new Site("Site B", "Liège"));

        fermetureSiteRepository.save(new FermetureSite(
                siteA, LocalDate.of(2030, 1, 10), null, null, "F1"
        ));
        fermetureSiteRepository.save(new FermetureSite(
                siteA, null, LocalDate.of(2030, 2, 1), LocalDate.of(2030, 2, 3), "F2"
        ));
        fermetureSiteRepository.save(new FermetureSite(
                siteB, LocalDate.of(2030, 3, 1), null, null, "F3"
        ));

        List<FermetureSite> result = fermetureSiteRepository.findBySiteIdOrderByDateAscDateDebutAsc(siteA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(f -> f.getSite().getId().equals(siteA.getId()));
    }
}