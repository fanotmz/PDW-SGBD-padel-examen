package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.*;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.*;
import be.ephec.padel.backend.service.ParticipationService;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ParticipationPremierPayePremierServiIT extends SqlServerTestContainerConfig {

    @Autowired
    ParticipationService participationService;

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaiementRepository paiementRepository;
    @Autowired MouvementSoldeRepository mouvementSoldeRepository;

    private MatchPadel match;
    private Joueur j4;
    private Joueur j5;

    @BeforeEach
    void setup() {
        // Clean (ordre FK)
        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        terrainRepository.deleteAll();
        joueurRepository.deleteAll();
        siteRepository.deleteAll();

        Site site = new Site("Site", "Ville");

        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);

        Terrain terrain = new Terrain("T1", site);
        terrain = terrainRepository.save(terrain);
        HoraireSite horaire = new HoraireSite(
                site,
                2026,
                LocalTime.of(8,0),
                LocalTime.of(22,0)
        );

        Joueur orga = new Joueur("G0001", "Orga", TypeJoueur.GLOBAL);
        orga = joueurRepository.save(orga);

        // Match PUBLIC dans le futur
        match = new MatchPadel(terrain, orga, LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0), MatchVisibilite.PUBLIC);
        match = matchPadelRepository.save(match);

        // 3 joueurs déjà inscrits (on remplit jusqu’à 3)
        Joueur j1 = joueurRepository.save(new Joueur("G0002", "J1", TypeJoueur.GLOBAL));
        Joueur j2 = joueurRepository.save(new Joueur("G0003", "J2", TypeJoueur.GLOBAL));
        Joueur j3 = joueurRepository.save(new Joueur("G0004", "J3", TypeJoueur.GLOBAL));

        participationRepository.save(new Participation(match, j1));
        participationRepository.save(new Participation(match, j2));
        participationRepository.save(new Participation(match, j3));

        // 2 candidats concurrents pour la 4e place
        j4 = joueurRepository.save(new Joueur("G0005", "J4", TypeJoueur.GLOBAL));
        j5 = joueurRepository.save(new Joueur("G0006", "J5", TypeJoueur.GLOBAL));
    }

    @Test
    void premier_paye_premier_servi_sur_derniere_place() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> runJoinPay(ready, start, match.getId(), j4.getMatricule());
        Callable<Boolean> task2 = () -> runJoinPay(ready, start, match.getId(), j5.getMatricule());

        Future<Boolean> f1 = executor.submit(task1);
        Future<Boolean> f2 = executor.submit(task2);

        // attendre que les 2 threads soient prêts, puis démarrer en même temps
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);

        executor.shutdownNow();

        // exactement 1 succès
        assertNotEquals(r1, r2);

        // et au final : 4 participations max
        int nb = participationRepository.countByMatch_Id(match.getId());
        assertEquals(4, nb);
    }

    private boolean runJoinPay(CountDownLatch ready,
                               CountDownLatch start,
                               Long matchId,
                               String matricule) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);

        try {
            participationService.rejoindreEtPayerMatchPublic(matchId, matricule);
            return true;
        } catch (BusinessException ex) {
            // l’un des deux doit tomber ici ("Match déjà complet")
            return false;
        }
    }

}