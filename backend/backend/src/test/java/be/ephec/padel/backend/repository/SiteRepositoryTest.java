package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SiteRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;

    @Test
    void existsByNom_retourneTrue_si_nom_existe() {
        siteRepository.save(new Site("Site A", "Bruxelles"));

        assertThat(siteRepository.existsByNom("Site A")).isTrue();
        assertThat(siteRepository.existsByNom("Site X")).isFalse();
    }
}