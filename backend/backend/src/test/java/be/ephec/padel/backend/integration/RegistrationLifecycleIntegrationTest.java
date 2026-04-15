package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.dto.request.ValidateRegistrationRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
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
import be.ephec.padel.backend.service.AdminRegistrationService;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.security.admin.global.username=adminGlobal",
        "app.security.admin.global.password=test123",
        "app.security.admin.site.users=",
        "app.security.admin.site.password=test123"
})
class RegistrationLifecycleIntegrationTest extends SqlServerTestContainerConfig {

    @Autowired
    MockMvc mvc;

    @Autowired
    SiteRepository siteRepository;

    @Autowired
    TerrainRepository terrainRepository;

    @Autowired
    HoraireSiteRepository horaireSiteRepository;

    @Autowired
    FermetureSiteRepository fermetureSiteRepository;

    @Autowired
    FermetureGlobaleRepository fermetureGlobaleRepository;

    @Autowired
    PaiementRepository paiementRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    MatchPadelRepository matchPadelRepository;

    @Autowired
    MouvementSoldeRepository mouvementSoldeRepository;

    @Autowired
    JoueurRepository joueurRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AdminRegistrationService adminRegistrationService;

    @Autowired
    PasswordEncoder passwordEncoder;

    private Site site;

    @BeforeEach
    void cleanAndSeed() {
        cleanupData();

        site = new Site("Site Delta", "Bruxelles");
        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);
    }

    @AfterEach
    void cleanupAfterTest() {
        cleanupData();
    }

    private void cleanupData() {
        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        fermetureSiteRepository.deleteAll();
        horaireSiteRepository.deleteAll();
        terrainRepository.deleteAll();
        fermetureGlobaleRepository.deleteAll();

        List<User> nonAdminUsers = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().noneMatch(this::isAdminRole))
                .toList();
        userRepository.deleteAll(nonAdminUsers);

        joueurRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Test
    void register_validate_login_et_me_fonctionnent_de_bout_en_bout() throws Exception {
        MvcResult registerResult = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "secret123",
                                  "nom": "Alice",
                                  "typeAbonnementDemande": "SITE",
                                  "siteIdDemande": %d
                                }
                                """.formatted(site.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value(containsString("en attente")))
                .andReturn();

        String registerBody = registerResult.getResponse().getContentAsString();
        String userId = extractNumericField(registerBody, "userId");

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Compte en attente de validation administrateur."));

        String adminToken = extractStringField(
                mvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "adminGlobal",
                                          "password": "test123"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "token"
        );

        mvc.perform(get("/api/v1/admin/inscriptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].typeAbonnementDemande").value("SITE"))
                .andExpect(jsonPath("$[0].siteIdDemande").value(site.getId()))
                .andExpect(jsonPath("$[0].siteNomDemande").value("Site Delta"));

        mvc.perform(post("/api/v1/admin/inscriptions/" + userId + "/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeAbonnementFinal": "SITE",
                                  "siteIdFinal": %d
                                }
                                """.formatted(site.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.joueurMatricule").value(startsWith("S")))
                .andExpect(jsonPath("$.joueurNom").value("Alice"))
                .andExpect(jsonPath("$.joueurType").value("SITE"))
                .andExpect(jsonPath("$.joueurSiteId").value(site.getId()));

        String playerToken = extractStringField(
                mvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "alice",
                                          "password": "secret123"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "token"
        );

        mvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + playerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Alice"))
                .andExpect(jsonPath("$.type").value("SITE"))
                .andExpect(jsonPath("$.siteId").value(site.getId()))
                .andExpect(jsonPath("$.matricule").value(startsWith("S")));
    }

    @Test
    void validate_en_concurrence_sur_le_meme_user_ne_valide_qu_une_seule_fois() throws Exception {
        User user = pendingUser("race-user", "Race User", TypeJoueur.GLOBAL, null);
        ValidateRegistrationRequest request = new ValidateRegistrationRequest();
        request.setTypeAbonnementFinal(TypeJoueur.GLOBAL);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> runConcurrentValidation(user.getId(), request, startGate));
            Future<String> second = executor.submit(() -> runConcurrentValidation(user.getId(), request, startGate));

            startGate.countDown();

            List<String> outcomes = new ArrayList<>();
            outcomes.add(first.get());
            outcomes.add(second.get());

            assertThat(outcomes).containsExactlyInAnyOrder("SUCCESS", "PENDING_GONE");

            User reloaded = userRepository.findById(user.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(reloaded.isActive()).isTrue();
            assertThat(reloaded.getJoueur()).isNotNull();
            assertThat(joueurRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void un_meme_joueur_ne_peut_pas_etre_lie_a_deux_users() throws Exception {
        Joueur joueur = joueurRepository.save(new Joueur("G0001", "Linked Player", TypeJoueur.GLOBAL));

        User user1 = new User();
        user1.setLogin("linked-user-1");
        user1.setPasswordHash(passwordEncoder.encode("secret123"));
        user1.setActive(true);
        user1.setStatus(UserStatus.ACTIVE);
        user1.addRole(SecurityRole.ROLE_JOUEUR);
        user1.setJoueur(joueur);
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setLogin("linked-user-2");
        user2.setPasswordHash(passwordEncoder.encode("secret123"));
        user2.setActive(true);
        user2.setStatus(UserStatus.ACTIVE);
        user2.addRole(SecurityRole.ROLE_JOUEUR);
        user2.setJoueur(joueur);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private boolean isAdminRole(SecurityRole role) {
        return role == SecurityRole.ROLE_ADMIN_GLOBAL || role == SecurityRole.ROLE_ADMIN_SITE;
    }

    private User pendingUser(String login, String nom, TypeJoueur requestedType, Site requestedSite) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode("secret123"));
        user.setActive(false);
        user.setStatus(UserStatus.PENDING);
        user.setRequestedNom(nom);
        user.setRequestedType(requestedType);
        user.setRequestedSite(requestedSite);
        user.addRole(SecurityRole.ROLE_JOUEUR);
        return userRepository.saveAndFlush(user);
    }

    private String runConcurrentValidation(Long userId,
                                           ValidateRegistrationRequest request,
                                           CountDownLatch startGate) throws Exception {
        startGate.await();
        try {
            adminRegistrationService.validate(userId, request);
            return "SUCCESS";
        } catch (BusinessException ex) {
            if ("Cette demande n'est plus en attente.".equals(ex.getMessage())) {
                return "PENDING_GONE";
            }
            throw ex;
        }
    }

    private String extractNumericField(String json, String fieldName) {
        return extractStringField(json, fieldName);
    }

    private String extractStringField(String json, String fieldName) {
        String token = "\"" + fieldName + "\":";
        int start = json.indexOf(token);
        if (start < 0) {
            throw new IllegalStateException("Field not found: " + fieldName);
        }

        int valueStart = start + token.length();
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            return json.substring(valueStart + 1, valueEnd);
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd);
    }
}
