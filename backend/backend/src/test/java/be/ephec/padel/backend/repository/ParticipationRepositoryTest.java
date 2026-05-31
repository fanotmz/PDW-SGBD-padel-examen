package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ParticipationRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;

    @Test
    void existsByMatchIdAndJoueurMatricule_true_apresInsertion() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));

        MatchPadel match = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC)
        );

        participationRepository.save(new Participation(match, j1));

        assertThat(participationRepository.existsByMatch_IdAndJoueur_Matricule(match.getId(), "J001"))
                .isTrue();
    }

    @Test
    void countByMatchId_compteBien() {
        Site s = siteRepository.save(new Site("Site B", "Namur"));
        Terrain t = terrainRepository.save(new Terrain("T2", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG2", "Orga2", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J010", "Bob", TypeJoueur.GLOBAL));
        Joueur j2 = joueurRepository.save(new Joueur("J011", "Eve", TypeJoueur.GLOBAL));

        MatchPadel match = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.now().plusDays(2), MatchVisibilite.PRIVE)
        );

        participationRepository.save(new Participation(match, j1));
        participationRepository.save(new Participation(match, j2));

        assertThat(participationRepository.countByMatch_Id(match.getId()))
                .isEqualTo(2);
    }

    @Test
    void countByJoueurMatricule_compte_les_participations_du_joueur() {
        Site s = siteRepository.save(new Site("Site C", "Liege"));
        Terrain t = terrainRepository.save(new Terrain("T3", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG3", "Orga3", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J020", "Alice", TypeJoueur.GLOBAL));
        Joueur j2 = joueurRepository.save(new Joueur("J021", "Bob", TypeJoueur.GLOBAL));

        MatchPadel match1 = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.of(2030, 1, 1, 10, 0), MatchVisibilite.PUBLIC)
        );
        MatchPadel match2 = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.of(2030, 1, 2, 10, 0), MatchVisibilite.PRIVE)
        );

        participationRepository.save(new Participation(match1, j1));
        participationRepository.save(new Participation(match2, j1));
        participationRepository.save(new Participation(match2, j2));

        assertThat(participationRepository.countByJoueur_Matricule("J020")).isEqualTo(2L);
        assertThat(participationRepository.countByJoueur_Matricule("J021")).isEqualTo(1L);
    }
}
