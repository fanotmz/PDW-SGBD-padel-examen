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
    void findPublicMatchSummaries_filtre_par_site_quand_siteId_est_renseigne() {
        Site siteA = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site siteB = siteRepository.save(new Site("Site B", "Namur"));
        Terrain terrainA = terrainRepository.save(new Terrain("T1", siteA));
        Terrain terrainB = terrainRepository.save(new Terrain("T2", siteB));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        MatchPadel matchSiteA = matchPadelRepository.save(
                new MatchPadel(terrainA, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(terrainB, orga, LocalDateTime.of(2030, 1, 2, 11, 0), MatchVisibilite.PUBLIC)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                null,
                siteA.getId()
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(matchSiteA.getId());
        assertThat(rows.get(0).getSiteId()).isEqualTo(siteA.getId());
    }

    @Test
    void findPublicMatchSummaries_filtre_avec_from_seul() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime from = LocalDateTime.of(2030, 1, 2, 10, 0);
        matchPadelRepository.save(
                new MatchPadel(t, orga, from.minusMinutes(1), MatchVisibilite.PUBLIC)
        );
        MatchPadel matchAtFrom = matchPadelRepository.save(
                new MatchPadel(t, orga, from, MatchVisibilite.PUBLIC)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                from,
                null,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(matchAtFrom.getId());
    }

    @Test
    void findPublicMatchSummaries_filtre_avec_to_seul() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime to = LocalDateTime.of(2030, 1, 2, 10, 0);
        MatchPadel matchBeforeTo = matchPadelRepository.save(
                new MatchPadel(t, orga, to.minusMinutes(1), MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(t, orga, to.plusMinutes(1), MatchVisibilite.PUBLIC)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                to,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(matchBeforeTo.getId());
    }

    @Test
    void findPublicMatchSummaries_inclut_la_borne_exacte_to() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime to = LocalDateTime.of(2030, 1, 2, 10, 0);
        MatchPadel matchAtTo = matchPadelRepository.save(
                new MatchPadel(t, orga, to, MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(t, orga, to.plusSeconds(1), MatchVisibilite.PUBLIC)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                null,
                to,
                null
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(matchAtTo.getId());
    }

    @Test
    void findPublicMatchSummaries_filtre_par_from_to_et_siteId() {
        Site siteA = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site siteB = siteRepository.save(new Site("Site B", "Namur"));
        Terrain terrainA = terrainRepository.save(new Terrain("T1", siteA));
        Terrain terrainB = terrainRepository.save(new Terrain("T2", siteB));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime from = LocalDateTime.of(2030, 1, 2, 0, 0);
        LocalDateTime to = LocalDateTime.of(2030, 1, 2, 23, 59, 59);

        matchPadelRepository.save(
                new MatchPadel(terrainA, orga, from.minusMinutes(1), MatchVisibilite.PUBLIC)
        );
        MatchPadel inScope = matchPadelRepository.save(
                new MatchPadel(terrainA, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(terrainA, orga, to.plusSeconds(1), MatchVisibilite.PUBLIC)
        );
        matchPadelRepository.save(
                new MatchPadel(terrainB, orga, LocalDateTime.of(2030, 1, 2, 11, 0), MatchVisibilite.PUBLIC)
        );

        em.flush();
        em.clear();

        var rows = matchPadelRepository.findPublicMatchSummaries(
                MatchVisibilite.PUBLIC,
                from,
                to,
                siteA.getId()
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(inScope.getId());
        assertThat(rows.get(0).getSiteId()).isEqualTo(siteA.getId());
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

    @Test
    void findAdminSiteMatches_scopeAll_sans_filtres_retourne_tous_les_matchs_du_site() {
        Site siteA = siteRepository.save(new Site("Site A", "Bruxelles"));
        Site siteB = siteRepository.save(new Site("Site B", "Namur"));
        Terrain terrainA = terrainRepository.save(new Terrain("T1", siteA));
        Terrain terrainB = terrainRepository.save(new Terrain("T2", siteB));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 10, 9, 0);
        MatchPadel plannedFuture = matchPadelRepository.save(new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 11, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel plannedPast = matchPadelRepository.save(new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 9, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel cancelledFuture = new MatchPadel(
                terrainA, orga, LocalDateTime.of(2030, 1, 12, 10, 0), MatchVisibilite.PRIVE
        );
        cancelledFuture.setStatut(MatchStatut.ANNULE);
        cancelledFuture = matchPadelRepository.save(cancelledFuture);
        matchPadelRepository.save(new MatchPadel(
                terrainB, orga, LocalDateTime.of(2030, 1, 13, 10, 0), MatchVisibilite.PUBLIC
        ));

        em.flush();
        em.clear();

        List<MatchPadel> rows = matchPadelRepository.findAdminSiteMatches(
                siteA.getId(),
                null,
                null,
                null,
                null,
                false,
                false,
                now
        );

        assertThat(rows)
                .extracting(MatchPadel::getId)
                .containsExactlyInAnyOrder(plannedFuture.getId(), plannedPast.getId(), cancelledFuture.getId());
    }

    @Test
    void findAdminSiteMatches_futureOnly_retourne_uniquement_les_futurs_planifies() {
        Site site = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain terrain = terrainRepository.save(new Terrain("T1", site));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 10, 9, 0);
        MatchPadel plannedFuture = matchPadelRepository.save(new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 11, 10, 0), MatchVisibilite.PUBLIC
        ));
        matchPadelRepository.save(new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 9, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel cancelledFuture = new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 12, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledFuture.setStatut(MatchStatut.ANNULE);
        matchPadelRepository.save(cancelledFuture);

        em.flush();
        em.clear();

        List<MatchPadel> rows = matchPadelRepository.findAdminSiteMatches(
                site.getId(),
                MatchStatut.PLANIFIE,
                null,
                null,
                null,
                true,
                false,
                now
        );

        assertThat(rows).extracting(MatchPadel::getId).containsExactly(plannedFuture.getId());
    }

    @Test
    void findAdminSiteMatches_history_retourne_matchs_passes_et_annules_futurs() {
        Site site = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain terrain = terrainRepository.save(new Terrain("T1", site));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 10, 9, 0);
        MatchPadel plannedPast = matchPadelRepository.save(new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 9, 10, 0), MatchVisibilite.PUBLIC
        ));
        matchPadelRepository.save(new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 11, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel cancelledFuture = new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 12, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledFuture.setStatut(MatchStatut.ANNULE);
        cancelledFuture = matchPadelRepository.save(cancelledFuture);

        em.flush();
        em.clear();

        List<MatchPadel> rows = matchPadelRepository.findAdminSiteMatches(
                site.getId(),
                null,
                null,
                null,
                null,
                false,
                true,
                now
        );

        assertThat(rows)
                .extracting(MatchPadel::getId)
                .containsExactlyInAnyOrder(plannedPast.getId(), cancelledFuture.getId());
    }

    @Test
    void findAdminSiteMatches_filtre_par_statut_et_visibilite() {
        Site site = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain terrain = terrainRepository.save(new Terrain("T1", site));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 10, 9, 0);
        MatchPadel cancelledPublic = new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 11, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledPublic.setStatut(MatchStatut.ANNULE);
        cancelledPublic = matchPadelRepository.save(cancelledPublic);
        MatchPadel cancelledPrivate = new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 12, 10, 0), MatchVisibilite.PRIVE
        );
        cancelledPrivate.setStatut(MatchStatut.ANNULE);
        matchPadelRepository.save(cancelledPrivate);
        matchPadelRepository.save(new MatchPadel(
                terrain, orga, LocalDateTime.of(2030, 1, 13, 10, 0), MatchVisibilite.PUBLIC
        ));

        em.flush();
        em.clear();

        List<MatchPadel> rows = matchPadelRepository.findAdminSiteMatches(
                site.getId(),
                MatchStatut.ANNULE,
                MatchVisibilite.PUBLIC,
                null,
                null,
                false,
                false,
                now
        );

        assertThat(rows).extracting(MatchPadel::getId).containsExactly(cancelledPublic.getId());
    }

    @Test
    void countByOrganisateurMatricule_compte_les_matchs_organises() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));
        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur other = joueurRepository.save(new Joueur("ORG2", "Other", TypeJoueur.GLOBAL));

        matchPadelRepository.save(new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel cancelled = new MatchPadel(
                t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelled.setStatut(MatchStatut.ANNULE);
        matchPadelRepository.save(cancelled);
        matchPadelRepository.save(new MatchPadel(
                t, other, LocalDateTime.of(2030, 1, 3, 10, 0), MatchVisibilite.PUBLIC
        ));

        assertThat(matchPadelRepository.countByOrganisateur_Matricule("ORG1")).isEqualTo(2L);
        assertThat(matchPadelRepository.countByOrganisateur_Matricule("ORG2")).isEqualTo(1L);
    }

    @Test
    void countDistinctLinkedMatches_dedoublonne_et_separe_passe_futur_annule() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur joueur = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));
        Joueur other = joueurRepository.save(new Joueur("J002", "Bob", TypeJoueur.GLOBAL));

        LocalDateTime now = LocalDateTime.of(2030, 1, 10, 9, 30);

        MatchPadel pastAsParticipant = matchPadelRepository.save(new MatchPadel(
                t, other, LocalDateTime.of(2030, 1, 9, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel futureAsOrganizer = matchPadelRepository.save(new MatchPadel(
                t, joueur, LocalDateTime.of(2030, 1, 11, 10, 0), MatchVisibilite.PUBLIC
        ));
        MatchPadel futureOrganizerAndParticipant = matchPadelRepository.save(new MatchPadel(
                t, joueur, LocalDateTime.of(2030, 1, 12, 10, 0), MatchVisibilite.PRIVE
        ));
        MatchPadel cancelledLinked = new MatchPadel(
                t, other, LocalDateTime.of(2030, 1, 8, 10, 0), MatchVisibilite.PUBLIC
        );
        cancelledLinked.setStatut(MatchStatut.ANNULE);
        cancelledLinked = matchPadelRepository.save(cancelledLinked);

        participationRepository.save(new Participation(pastAsParticipant, joueur));
        participationRepository.save(new Participation(futureOrganizerAndParticipant, joueur));
        participationRepository.save(new Participation(cancelledLinked, joueur));

        long past = matchPadelRepository.countDistinctLinkedPastMatchesByMatricule("J001", now);
        long future = matchPadelRepository.countDistinctLinkedFutureMatchesByMatricule("J001", now);
        long cancelled = matchPadelRepository.countDistinctLinkedCancelledMatchesByMatricule("J001");

        assertThat(past).isEqualTo(1L);
        assertThat(future).isEqualTo(2L);
        assertThat(cancelled).isEqualTo(1L);
    }
}
