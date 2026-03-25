package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
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

        MatchPadel m1 = matchPadelRepository.save(
                new MatchPadel(t1, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC)
        );
        MatchPadel m2 = matchPadelRepository.save(
                new MatchPadel(t1, orga, LocalDateTime.now().plusDays(2), MatchVisibilite.PRIVE)
        );
        matchPadelRepository.save(
                new MatchPadel(t2, orga, LocalDateTime.now().plusDays(3), MatchVisibilite.PUBLIC)
        );

        List<MatchPadel> matchsT1 = matchPadelRepository.findByTerrainId(t1.getId());

        assertThat(matchsT1).hasSize(2);
        assertThat(matchsT1)
                .extracting(MatchPadel::getId)
                .containsExactlyInAnyOrder(m1.getId(), m2.getId());
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
        assertThat(res)
                .extracting(MatchPadel::getId)
                .containsExactlyInAnyOrder(m1.getId(), m2.getId());
    }

    @Test
    void findByIdWithDetails_charge_terrain_site_organisateur_et_participations() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));

        MatchPadel m = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC)
        );

        Participation part = new Participation(m, j1);
        m.addParticipation(part);
        participationRepository.save(part);

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

    @Test
    void findPublicMatchSummaries_retourne_seulement_les_matchs_publics() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel publicMatch = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PRIVE)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                null,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(publicMatch.getId());
        assertThat(rows.get(0).getNbParticipants()).isEqualTo(0L);
    }

    @Test
    void findPublicMatchSummaries_compte_correctement_les_participations() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));
        Joueur j2 = joueurRepository.save(new Joueur("J002", "Bob", TypeJoueur.GLOBAL));

        MatchPadel publicMatch = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC)
        );

        Participation p1 = new Participation(publicMatch, j1);
        Participation p2 = new Participation(publicMatch, j2);

        publicMatch.addParticipation(p1);
        publicMatch.addParticipation(p2);

        participationRepository.save(p1);
        participationRepository.save(p2);

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                null,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(publicMatch.getId());
        assertThat(rows.get(0).getNbParticipants()).isEqualTo(2L);
    }

    @Test
    void findPublicMatchSummaries_exclut_les_matchs_annules() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel plannedMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC
        );
        MatchPadel cancelledMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledMatch.setStatut(MatchStatut.ANNULE);

        matchPadelRepository.save(plannedMatch);
        matchPadelRepository.save(cancelledMatch);

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                null,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(plannedMatch.getId());
    }

    @Test
    void countByDateDebutBetween_exclut_les_matchs_annules() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        matchPadelRepository.save(new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC
        ));

        MatchPadel cancelledMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledMatch.setStatut(MatchStatut.ANNULE);
        matchPadelRepository.save(cancelledMatch);

        long count = matchPadelRepository.countByDateDebutBetween(
                LocalDateTime.of(2030, 1, 1, 0, 0),
                LocalDateTime.of(2030, 1, 3, 0, 0)
        );

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void findAtraiterJ1AvecDetails_exclut_les_matchs_annules() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel plannedMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        MatchPadel cancelledMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 11, 0), MatchVisibilite.PUBLIC
        );
        cancelledMatch.setStatut(MatchStatut.ANNULE);

        matchPadelRepository.save(plannedMatch);
        matchPadelRepository.save(cancelledMatch);

        List<MatchPadel> rows = matchPadelRepository.findAtraiterJ1AvecDetails(
                LocalDateTime.of(2030, 1, 2, 0, 0),
                LocalDateTime.of(2030, 1, 3, 0, 0)
        );

        assertThat(rows).extracting(MatchPadel::getId).containsExactly(plannedMatch.getId());
    }

    @Test
    void findAtraiterDebutMatchAvecDetails_exclut_les_matchs_annules() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel plannedMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        MatchPadel cancelledMatch = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 11, 0), MatchVisibilite.PUBLIC
        );
        cancelledMatch.setStatut(MatchStatut.ANNULE);

        matchPadelRepository.save(plannedMatch);
        matchPadelRepository.save(cancelledMatch);

        List<MatchPadel> rows = matchPadelRepository.findAtraiterDebutMatchAvecDetails(
                LocalDateTime.of(2030, 1, 2, 0, 0),
                LocalDateTime.of(2030, 1, 3, 0, 0)
        );

        assertThat(rows).extracting(MatchPadel::getId).containsExactly(plannedMatch.getId());
    }

    @Test
    void findPlannedFutureMatchesBySiteIdAndDateDebutBetween_retourne_seulement_les_matchs_planifies_futurs_du_site() {
        Site siteA = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site siteB = siteRepository.save(new Site("Site B", "Namur"));
        Terrain terrainA = terrainRepository.save(new Terrain("T1", siteA));
        Terrain terrainB = terrainRepository.save(new Terrain("T2", siteB));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 1, 9, 0);
        LocalDateTime from = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 1, 3, 0, 0);

        MatchPadel inScope = new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC
        );
        MatchPadel startedOrPast = new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 1, 8, 0), MatchVisibilite.PUBLIC
        );
        MatchPadel cancelled = new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelled.setStatut(MatchStatut.ANNULE);
        MatchPadel otherSite = new MatchPadel(
                terrainB, orga, LocalDateTime.of(2030, 1, 1, 11, 0), MatchVisibilite.PUBLIC
        );

        matchPadelRepository.save(inScope);
        matchPadelRepository.save(startedOrPast);
        matchPadelRepository.save(cancelled);
        matchPadelRepository.save(otherSite);

        List<MatchPadel> rows = matchPadelRepository.findPlannedFutureMatchesBySiteIdAndDateDebutBetween(
                siteA.getId(),
                MatchStatut.PLANIFIE,
                now,
                from,
                to
        );

        assertThat(rows).extracting(MatchPadel::getId).containsExactly(inScope.getId());
    }
}
