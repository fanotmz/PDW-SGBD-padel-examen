package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TerrainRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;

    @Test
    void existsByNomAndSiteId_retourneTrue_uniquement_pour_le_bon_site() {
        Site s1 = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site s2 = siteRepository.save(new Site("Site B", "Liege"));

        terrainRepository.save(new Terrain("T1", s1));

        assertThat(terrainRepository.existsByNomAndSiteId("T1", s1.getId())).isTrue();
        assertThat(terrainRepository.existsByNomAndSiteId("T1", s2.getId())).isFalse();
        assertThat(terrainRepository.existsByNomAndSiteId("T2", s1.getId())).isFalse();
    }

    @Test
    void findBySite_Id_retourne_les_terrains_du_site() {
        Site s1 = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site s2 = siteRepository.save(new Site("Site B", "Liege"));

        terrainRepository.save(new Terrain("T1", s1));
        terrainRepository.save(new Terrain("T2", s1));
        terrainRepository.save(new Terrain("T3", s2));

        List<Terrain> terrainsS1 = terrainRepository.findBySite_Id(s1.getId());
        List<Terrain> terrainsS2 = terrainRepository.findBySite_Id(s2.getId());

        assertThat(terrainsS1).hasSize(2);
        assertThat(terrainsS1).extracting(Terrain::getNom).containsExactlyInAnyOrder("T1", "T2");

        assertThat(terrainsS2).hasSize(1);
        assertThat(terrainsS2.get(0).getNom()).isEqualTo("T3");
    }
}