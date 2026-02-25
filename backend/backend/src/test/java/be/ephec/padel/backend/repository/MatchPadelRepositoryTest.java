package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MatchPadelRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;

    @Autowired TestEntityManager em;

    @Test
    void findByTerrainId_retourne_les_matchs_du_terrain() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t1 = terrainRepository.save(new Terrain("T1", s));
        Terrain t2 = terrainRepository.save(new Terrain("T2", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel m1 = matchPadelRepository.save(new MatchPadel(t1, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC));
        MatchPadel m2 = matchPadelRepository.save(new MatchPadel(t1, orga, LocalDateTime.now().plusDays(2), MatchVisibilite.PRIVE));
        matchPadelRepository.save(new MatchPadel(t2, orga, LocalDateTime.now().plusDays(3), MatchVisibilite.PUBLIC));

        List<MatchPadel> matchsT1 = matchPadelRepository.findByTerrainId(t1.getId());

        assertThat(matchsT1).hasSize(2);
        assertThat(matchsT1).extracting(MatchPadel::getId).containsExactlyInAnyOrder(m1.getId(), m2.getId());
    }

    @Test
    void findByDateDebutBetween_retourne_les_matchs_dans_intervalle() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime d1 = LocalDateTime.of(2026, 3, 1, 10, 0);
        LocalDateTime d2 = LocalDateTime.of(2026, 3, 5, 10, 0);
        LocalDateTime d3 = LocalDateTime.of(2026, 3, 10, 10, 0);

        MatchPadel m1 = matchPadelRepository.save(new MatchPadel(t, orga, d1, MatchVisibilite.PUBLIC));
        MatchPadel m2 = matchPadelRepository.save(new MatchPadel(t, orga, d2, MatchVisibilite.PUBLIC));
        matchPadelRepository.save(new MatchPadel(t, orga, d3, MatchVisibilite.PUBLIC));

        List<MatchPadel> res = matchPadelRepository.findByDateDebutBetween(
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 3, 6, 0, 0)
        );

        assertThat(res).hasSize(2);
        assertThat(res).extracting(MatchPadel::getId).containsExactlyInAnyOrder(m1.getId(), m2.getId());
    }

    @Test
    void findByIdWithDetails_charge_terrain_site_organisateur_et_participations() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));

        MatchPadel m = matchPadelRepository.save(new MatchPadel(
                t, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC
        ));

        // Maintenir les 2 côtés (recommandé)
        Participation part = new Participation(m, j1);
        m.addParticipation(part);
        participationRepository.save(part);

        // Important : forcer écriture DB + vider le contexte pour éviter de relire l'objet "cached"
        em.flush();
        em.clear();

        Optional<MatchPadel> opt = matchPadelRepository.findByIdWithDetails(m.getId());
        assertThat(opt).isPresent();

        MatchPadel loaded = opt.get();

        assertThat(loaded.getTerrain()).isNotNull();
        assertThat(loaded.getTerrain().getNom()).isEqualTo("T1");

        assertThat(loaded.getTerrain().getSite()).isNotNull();
        assertThat(loaded.getTerrain().getSite().getNom()).isEqualTo("Site A");

        assertThat(loaded.getOrganisateur()).isNotNull();
        assertThat(loaded.getOrganisateur().getMatricule()).isEqualTo("ORG1");

        assertThat(loaded.getParticipations()).hasSize(1);
        assertThat(loaded.getParticipations().get(0).getJoueur().getMatricule()).isEqualTo("J001");
    }
}