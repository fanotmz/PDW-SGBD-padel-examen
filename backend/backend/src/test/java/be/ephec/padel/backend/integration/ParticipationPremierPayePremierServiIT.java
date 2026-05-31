package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.ParticipationService;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    @Autowired UserRepository userRepository;

    private MatchPadel match;
    private Joueur j4;
    private Joueur j5;

    @BeforeEach
    void setup() {
        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        userRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        terrainRepository.deleteAll();
        joueurRepository.deleteAll();
        siteRepository.deleteAll();

        Site site = new Site("Site", "Ville");
        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);

        Terrain terrain = new Terrain("T1", site);
        terrain = terrainRepository.save(terrain);
        new HoraireSite(site, 2026, LocalTime.of(8, 0), LocalTime.of(22, 0));

        Joueur orga = joueurRepository.save(new Joueur("G0001", "Orga", TypeJoueur.GLOBAL));

        match = new MatchPadel(
                terrain,
                orga,
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                MatchVisibilite.PUBLIC
        );
        match = matchPadelRepository.save(match);

        Joueur j1 = joueurRepository.save(new Joueur("G0002", "J1", TypeJoueur.GLOBAL));
        Joueur j2 = joueurRepository.save(new Joueur("G0003", "J2", TypeJoueur.GLOBAL));
        Joueur j3 = joueurRepository.save(new Joueur("G0004", "J3", TypeJoueur.GLOBAL));

        participationRepository.save(new Participation(match, j1));
        participationRepository.save(new Participation(match, j2));
        participationRepository.save(new Participation(match, j3));

        j4 = joueurRepository.save(new Joueur("G0005", "J4", TypeJoueur.GLOBAL));
        j5 = joueurRepository.save(new Joueur("G0006", "J5", TypeJoueur.GLOBAL));

        createUser("j4-login", j4, SecurityRole.ROLE_JOUEUR);
        createUser("j5-login", j5, SecurityRole.ROLE_JOUEUR);
    }

    @Test
    void premier_paye_premier_servi_sur_derniere_place() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> task1 = () -> runJoinPay(ready, start, match.getId(), "j4-login");
        Callable<Boolean> task2 = () -> runJoinPay(ready, start, match.getId(), "j5-login");

        Future<Boolean> f1 = executor.submit(task1);
        Future<Boolean> f2 = executor.submit(task2);

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);

        executor.shutdownNow();

        assertNotEquals(r1, r2);
        assertEquals(4, participationRepository.countByMatch_Id(match.getId()));
    }

    private boolean runJoinPay(CountDownLatch ready,
                               CountDownLatch start,
                               Long matchId,
                               String login) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);

        try {
            runAs(login, List.of(SecurityRole.ROLE_JOUEUR), () -> participationService.rejoindreEtPayerMatchPublic(matchId));
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void createUser(String login, Joueur joueur, SecurityRole role) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash("noop");
        user.setJoueur(joueur);
        user.addRole(role);
        userRepository.save(user);
    }

    private void runAs(String login, List<SecurityRole> roles, Runnable runnable) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                login,
                "N/A",
                roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList()
        ));
        try {
            runnable.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
