package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaiementRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaiementRepository paiementRepository;

    @Test
    void sumMontantByParticipationId_sommeCorrectement() {
        Site s = siteRepository.save(new Site("Site A", "Bruxelles"));
        Terrain t = terrainRepository.save(new Terrain("T1", s));

        Joueur orga = joueurRepository.save(new Joueur("ORG1", "Orga", TypeJoueur.GLOBAL));
        Joueur j1 = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));

        MatchPadel match = matchPadelRepository.save(
                new MatchPadel(t, orga, LocalDateTime.now().plusDays(1), MatchVisibilite.PUBLIC)
        );

        Participation part = participationRepository.save(new Participation(match, j1));

        paiementRepository.save(new Paiement(part, new BigDecimal("5.00"), LocalDateTime.now()));
        paiementRepository.save(new Paiement(part, new BigDecimal("7.50"), LocalDateTime.now()));

        BigDecimal sum = paiementRepository.sumMontantByParticipationId(part.getId());

        assertThat(sum).isNotNull();
        assertThat(sum).isEqualByComparingTo(new BigDecimal("12.50"));
    }
}