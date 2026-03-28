package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MatchPadel;
import be.ephec.padel.backend.model.entities.Paiement;
import be.ephec.padel.backend.model.entities.Participation;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.MatchStatut;
import be.ephec.padel.backend.model.enums.MatchVisibilite;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.repository.FermetureSiteRepository;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.MatchPadelService;
import be.ephec.padel.backend.service.PaiementService;
import be.ephec.padel.backend.service.ParticipationService;
import be.ephec.padel.backend.service.TraitementDebutMatchService;
import be.ephec.padel.backend.service.TraitementJ1Service;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {
        be.ephec.padel.backend.BackendApplication.class,
        FermetureSiteAnnulationIntegrationIT.FixedClockConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "app.security.admin.global.password=test-global-password",
        "app.security.admin.site.password=test-site-password",
        "app.security.admin.site.users=adminSite1:1,adminSite2:2"
})
class FermetureSiteAnnulationIntegrationIT extends SqlServerTestContainerConfig {

    @Autowired MockMvc mockMvc;

    @Autowired SiteRepository siteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired HoraireSiteRepository horaireSiteRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaiementRepository paiementRepository;
    @Autowired MouvementSoldeRepository mouvementSoldeRepository;
    @Autowired FermetureSiteRepository fermetureSiteRepository;
    @Autowired UserRepository userRepository;

    @Autowired MatchPadelService matchPadelService;
    @Autowired ParticipationService participationService;
    @Autowired PaiementService paiementService;
    @Autowired TraitementJ1Service traitementJ1Service;
    @Autowired TraitementDebutMatchService traitementDebutMatchService;
    @Autowired MutableClock clock;

    private Site site;
    private Terrain terrain;
    private Joueur orga;
    private Joueur joueur2;
    private Joueur joueur3;
    private Joueur joueur4;
    private Joueur joueur5;

    @BeforeEach
    void cleanAndSeed() {
        clock.setInstant(LocalDateTime.of(2030, 1, 1, 9, 0)
                .atZone(clock.getZone())
                .toInstant());

        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        userRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        fermetureSiteRepository.deleteAll();
        horaireSiteRepository.deleteAll();
        terrainRepository.deleteAll();
        joueurRepository.deleteAll();
        siteRepository.deleteAll();

        site = new Site("Site Annulation IT", "Bruxelles");
        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);

        terrain = new Terrain("Terrain Annulation IT", site);
        terrain = terrainRepository.save(terrain);

        horaireSiteRepository.save(new HoraireSite(
                site,
                LocalDate.now(clock).getYear(),
                LocalTime.of(8, 0),
                LocalTime.of(22, 0)
        ));

        orga = joueurRepository.save(joueur("G0001", "Orga"));
        joueur2 = joueurRepository.save(joueur("G0002", "Joueur 2"));
        joueur3 = joueurRepository.save(joueur("G0003", "Joueur 3"));
        joueur4 = joueurRepository.save(joueur("G0004", "Joueur 4"));
        joueur5 = joueurRepository.save(joueur("G0005", "Joueur 5"));

        createUser("orga-login", orga, SecurityRole.ROLE_JOUEUR);
        createUser("joueur3-login", joueur3, SecurityRole.ROLE_JOUEUR);
        createUser("joueur5-login", joueur5, SecurityRole.ROLE_JOUEUR);
    }

    @Test
    void fermeture_date_annule_match_prive_compense_et_bloque_les_ecritures() throws Exception {
        LocalDateTime dateMatch = LocalDateTime.of(2030, 1, 2, 10, 0);

        MatchPadel match = runAs("orga-login", List.of(SecurityRole.ROLE_JOUEUR), () ->
                matchPadelService.creerMatch(terrain.getId(), dateMatch, MatchVisibilite.PRIVE)
        );
        runAs("orga-login", List.of(SecurityRole.ROLE_JOUEUR), () -> {
            participationService.ajouterJoueurParOrganisateur(match.getId(), joueur2.getMatricule());
            participationService.ajouterJoueurParOrganisateur(match.getId(), joueur3.getMatricule());
            return null;
        });

        Participation participationJ3 = participationRepository
                .findByMatch_IdAndJoueur_Matricule(match.getId(), joueur3.getMatricule())
                .orElseThrow();
        runAs("joueur3-login", List.of(SecurityRole.ROLE_JOUEUR), () -> {
            paiementService.payerParticipation(participationJ3.getId(), new BigDecimal("10.00"));
            return null;
        });

        mockMvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2030-01-02",
                                  "motif": "Incident technique"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/admin/sites/" + site.getId() + "/fermetures/")));

        MatchPadel cancelled = matchPadelRepository.findByIdWithDetails(match.getId()).orElseThrow();
        assertThat(cancelled.getStatut()).isEqualTo(MatchStatut.ANNULE);
        assertThat(cancelled.getParticipations()).hasSize(3);

        Joueur orgaReloaded = joueurRepository.findById(orga.getMatricule()).orElseThrow();
        Joueur joueur2Reloaded = joueurRepository.findById(joueur2.getMatricule()).orElseThrow();
        Joueur joueur3Reloaded = joueurRepository.findById(joueur3.getMatricule()).orElseThrow();
        assertThat(orgaReloaded.getSolde()).isEqualByComparingTo("0.00");
        assertThat(joueur2Reloaded.getSolde()).isEqualByComparingTo("0.00");
        assertThat(joueur3Reloaded.getSolde()).isEqualByComparingTo("0.00");

        List<Paiement> paiements = paiementRepository.findByParticipation_Match_Id(match.getId());
        assertThat(paiements).hasSize(4);
        assertThat(paiements.stream()
                .filter(p -> p.getType() == TypePaiement.REMBOURSEMENT)
                .map(Paiement::getMontant))
                .containsExactlyInAnyOrder(new BigDecimal("-15.00"), new BigDecimal("-10.00"));

        mockMvc.perform(get("/api/v1/matchs/public")
                        .with(user("orga-login").roles("JOUEUR"))
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-03")
                        .param("siteId", site.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/v1/matchs/" + match.getId())
                        .with(user("orga-login").roles("JOUEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULE"))
                .andExpect(jsonPath("$.resteAPayer").value(0.00))
                .andExpect(jsonPath("$.montantPaye").value(25.00))
                .andExpect(jsonPath("$.montantRembourse").value(25.00));

        mockMvc.perform(get("/api/v1/me/matchs")
                        .with(user("joueur3-login").roles("JOUEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("ANNULE"));

        mockMvc.perform(get("/api/v1/me/matchs/organises")
                        .with(user("orga-login").roles("JOUEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("ANNULE"))
                .andExpect(jsonPath("$[0].risquePenaliteJ1").value(false));

        mockMvc.perform(post("/api/v1/matchs/" + match.getId() + "/participants/prive")
                        .with(user("orga-login").roles("JOUEUR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "joueurMatriculeAAjouter": "%s"
                                }
                                """.formatted(joueur4.getMatricule())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Match annul")));

        mockMvc.perform(post("/api/v1/participations/" + participationJ3.getId() + "/paiements")
                        .with(user("joueur3-login").roles("JOUEUR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "montant": 5.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Match annul")));

        FermetureSite fermeture = fermetureSiteRepository.findAll().get(0);
        mockMvc.perform(delete("/api/v1/admin/sites/" + site.getId() + "/fermetures/" + fermeture.getId())
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        MatchPadel stillCancelled = matchPadelRepository.findByIdWithDetails(match.getId()).orElseThrow();
        assertThat(stillCancelled.getStatut()).isEqualTo(MatchStatut.ANNULE);
    }

    @Test
    void fermeture_date_annule_match_public_et_bloque_join_et_montant_attendu() throws Exception {
        LocalDateTime dateMatch = LocalDateTime.of(2030, 1, 2, 11, 0);

        MatchPadel match = runAs("orga-login", List.of(SecurityRole.ROLE_JOUEUR), () ->
                matchPadelService.creerMatch(terrain.getId(), dateMatch, MatchVisibilite.PUBLIC)
        );

        mockMvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2030-01-02",
                                  "motif": "Incident technique"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/matchs/" + match.getId() + "/participants/public/montant-attendu")
                        .with(user("joueur5-login").roles("JOUEUR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Match annul")));

        mockMvc.perform(post("/api/v1/matchs/" + match.getId() + "/participants/public")
                        .with(user("joueur5-login").roles("JOUEUR"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Match annul")));
    }

    @Test
    void match_annule_est_ignore_par_schedulers_et_exclu_des_stats() throws Exception {
        LocalDateTime dateMatch = LocalDateTime.of(2030, 1, 2, 9, 1);

        MatchPadel match = runAs("orga-login", List.of(SecurityRole.ROLE_JOUEUR), () ->
                matchPadelService.creerMatch(terrain.getId(), dateMatch, MatchVisibilite.PRIVE)
        );

        mockMvc.perform(post("/api/v1/admin/sites/" + site.getId() + "/fermetures/date")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2030-01-02",
                                  "motif": "Incident technique"
                                }
                                """))
                .andExpect(status().isCreated());

        int traitesJ1 = traitementJ1Service.traiterJ1FenetreMinutes(5);
        assertThat(traitesJ1).isZero();

        MatchPadel afterJ1 = matchPadelRepository.findByIdWithDetails(match.getId()).orElseThrow();
        assertThat(afterJ1.getJ1TraiteLe()).isNull();
        assertThat(joueurRepository.findById(orga.getMatricule()).orElseThrow().getPenaliteJusqua()).isNull();

        clock.setInstant(dateMatch.plusMinutes(1).atZone(clock.getZone()).toInstant());

        int traitesDebut = traitementDebutMatchService.traiterDebutMatchFenetreMinutes(5);
        assertThat(traitesDebut).isZero();

        MatchPadel afterDebut = matchPadelRepository.findByIdWithDetails(match.getId()).orElseThrow();
        assertThat(afterDebut.getSoldeTraiteLe()).isNull();
        assertThat(joueurRepository.findById(orga.getMatricule()).orElseThrow().getSolde()).isEqualByComparingTo("0.00");

        mockMvc.perform(get("/api/v1/admin/stats/matchs")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .param("from", "2030-01-02")
                        .param("to", "2030-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbMatchs").value(0));

        mockMvc.perform(get("/api/v1/admin/stats/ca")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caTotal").value(0.00));

        mockMvc.perform(get("/api/v1/admin/sites/" + site.getId() + "/stats/matchs")
                        .with(user("adminGlobal").roles("ADMIN_GLOBAL"))
                        .param("from", "2030-01-02")
                        .param("to", "2030-01-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbMatchs").value(0));
    }

    private Joueur joueur(String matricule, String nom) {
        Joueur joueur = new Joueur(matricule, nom, TypeJoueur.GLOBAL);
        joueur.setSolde(BigDecimal.ZERO);
        return joueur;
    }

    private void createUser(String login, Joueur joueur, SecurityRole role) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash("noop");
        user.setJoueur(joueur);
        user.addRole(role);
        userRepository.save(user);
    }

    private <T> T runAs(String login, List<SecurityRole> roles, Supplier<T> supplier) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                login,
                "N/A",
                roles.stream().map(role -> new SimpleGrantedAuthority(role.name())).toList()
        ));
        try {
            return supplier.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        MutableClock clock() {
            return new MutableClock(
                    Instant.parse("2030-01-01T08:00:00Z"),
                    ZoneId.of("Europe/Brussels")
            );
        }
    }

    static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
