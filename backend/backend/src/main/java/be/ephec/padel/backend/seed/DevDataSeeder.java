package be.ephec.padel.backend.seed;

import be.ephec.padel.backend.common.Tarifs;
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
import be.ephec.padel.backend.model.enums.OrigineMouvementSoldeType;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypePaiement;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.SoldeOriginContext;
import be.ephec.padel.backend.service.SoldeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Service
public class DevDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final String SEED_MARKER_LOGIN = "joueur.global.dev";
    private static final String EXPECTED_ADMIN_SITE_MAPPING = "admin.site.nord.dev:1";
    private static final String EXPECTED_ADMIN_SITE_LOGIN = "admin.site.nord.dev";
    private static final String PLAYER_PASSWORD = "joueur123";
    private static final BigDecimal PART = Tarifs.PART_PAR_JOUEUR;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final SiteRepository siteRepository;
    private final TerrainRepository terrainRepository;
    private final HoraireSiteRepository horaireSiteRepository;
    private final JoueurRepository joueurRepository;
    private final UserRepository userRepository;
    private final MatchPadelRepository matchPadelRepository;
    private final ParticipationRepository participationRepository;
    private final PaiementRepository paiementRepository;
    private final PasswordEncoder passwordEncoder;
    private final SoldeService soldeService;
    private final Clock clock;
    private final String adminGlobalUsername;
    private final String adminSiteUsers;

    public DevDataSeeder(SiteRepository siteRepository,
                         TerrainRepository terrainRepository,
                         HoraireSiteRepository horaireSiteRepository,
                         JoueurRepository joueurRepository,
                         UserRepository userRepository,
                         MatchPadelRepository matchPadelRepository,
                         ParticipationRepository participationRepository,
                         PaiementRepository paiementRepository,
                         PasswordEncoder passwordEncoder,
                         SoldeService soldeService,
                         Clock clock,
                         @Value("${app.security.admin.global.username:}") String adminGlobalUsername,
                         @Value("${app.security.admin.site.users:}") String adminSiteUsers) {
        this.siteRepository = siteRepository;
        this.terrainRepository = terrainRepository;
        this.horaireSiteRepository = horaireSiteRepository;
        this.joueurRepository = joueurRepository;
        this.userRepository = userRepository;
        this.matchPadelRepository = matchPadelRepository;
        this.participationRepository = participationRepository;
        this.paiementRepository = paiementRepository;
        this.passwordEncoder = passwordEncoder;
        this.soldeService = soldeService;
        this.clock = clock;
        this.adminGlobalUsername = adminGlobalUsername;
        this.adminSiteUsers = adminSiteUsers;
    }

    @Transactional
    public void seed() {
        log.info("Seed dev actif: support garanti uniquement sur DB locale propre / vide.");

        validateAdminBootstrapOrderAndConfiguration();

        if (userRepository.existsByLogin(SEED_MARKER_LOGIN)) {
            log.info("Seed dev deja present (marqueur detecte: {}). Aucun reseed des matchs et donnees metier.", SEED_MARKER_LOGIN);
            return;
        }

        validateCleanDatabaseBeforeFirstSeed();

        int currentYear = LocalDate.now(clock).getYear();

        Site siteNord = ensureSite("Site Nord", "Bruxelles");
        if (!Long.valueOf(1L).equals(siteNord.getId())) {
            throw new IllegalStateException(
                    "Seed dev incompatible avec cette base: Site Nord devrait obtenir l'id 1 mais a l'id "
                            + siteNord.getId()
                            + ". Support garanti uniquement sur une DB locale propre / vide."
            );
        }
        Site siteSud = ensureSite("Site Sud", "Namur");

        ensureHoraire(siteNord, currentYear);
        ensureHoraire(siteNord, currentYear + 1);
        ensureHoraire(siteSud, currentYear);
        ensureHoraire(siteSud, currentYear + 1);

        Terrain nordT1 = ensureTerrain(siteNord, "Nord T1");
        Terrain nordT2 = ensureTerrain(siteNord, "Nord T2");
        Terrain sudT1 = ensureTerrain(siteSud, "Sud T1");
        Terrain sudT2 = ensureTerrain(siteSud, "Sud T2");

        Joueur joueurGlobal = ensureJoueur("G9001", "Joueur Global Demo", TypeJoueur.GLOBAL, null, ZERO);
        Joueur joueurSiteNord = ensureJoueur("S9001", "Joueur Site Nord Demo", TypeJoueur.SITE, siteNord, ZERO);
        Joueur joueurLibre = ensureJoueur("L9001", "Joueur Libre Demo", TypeJoueur.LIBRE, null, ZERO);
        Joueur joueurSiteSud = ensureJoueur("S9002", "Joueur Site Sud Demo", TypeJoueur.SITE, siteSud, ZERO);

        ensurePlayerUser("joueur.global.dev", joueurGlobal);
        ensurePlayerUser("joueur.site.dev", joueurSiteNord);
        ensurePlayerUser("joueur.libre.dev", joueurLibre);
        ensurePlayerUser("joueur.site.sud.dev", joueurSiteSud);

        LocalDate today = LocalDate.now(clock);

        MatchPadel publicFuturOuvert = ensureMatch(
                nordT1,
                joueurGlobal,
                today.plusDays(1).atTime(18, 0),
                MatchVisibilite.PUBLIC,
                MatchStatut.PLANIFIE
        );
        ensureParticipation(publicFuturOuvert, joueurGlobal);
        ensureParticipation(publicFuturOuvert, joueurSiteNord);
        ensureEncaissement(publicFuturOuvert, joueurGlobal, PART);
        ensureEncaissement(publicFuturOuvert, joueurSiteNord, PART);

        MatchPadel publicFuturComplet = ensureMatch(
                nordT2,
                joueurSiteNord,
                today.plusDays(2).atTime(19, 30),
                MatchVisibilite.PUBLIC,
                MatchStatut.PLANIFIE
        );
        ensureParticipation(publicFuturComplet, joueurSiteNord);
        ensureParticipation(publicFuturComplet, joueurGlobal);
        Participation participationLibrePublic = ensureParticipation(publicFuturComplet, joueurLibre);
        ensureParticipation(publicFuturComplet, joueurSiteSud);
        ensureEncaissement(publicFuturComplet, joueurSiteNord, PART);
        ensureEncaissement(publicFuturComplet, joueurGlobal, PART);
        ensureEncaissement(publicFuturComplet, joueurSiteSud, PART);
        ensureSeedDebt(participationLibrePublic, OrigineMouvementSoldeType.REJOINDRE_MATCH_PUBLIC_PART);

        MatchPadel priveFutur = ensureMatch(
                sudT1,
                joueurSiteSud,
                today.plusDays(3).atTime(20, 0),
                MatchVisibilite.PRIVE,
                MatchStatut.PLANIFIE
        );
        ensureParticipation(priveFutur, joueurSiteSud);
        Participation participationLibrePrive = ensureParticipation(priveFutur, joueurLibre);
        ensureEncaissement(priveFutur, joueurSiteSud, PART);
        ensureSeedDebt(participationLibrePrive, OrigineMouvementSoldeType.AJOUT_MATCH_PRIVE);

        MatchPadel publicPasse = ensureMatch(
                nordT1,
                joueurGlobal,
                today.minusDays(5).atTime(18, 30),
                MatchVisibilite.PUBLIC,
                MatchStatut.PLANIFIE
        );
        ensureParticipation(publicPasse, joueurGlobal);
        ensureParticipation(publicPasse, joueurSiteNord);
        ensureParticipation(publicPasse, joueurLibre);
        ensureParticipation(publicPasse, joueurSiteSud);
        ensureEncaissement(publicPasse, joueurGlobal, PART);
        ensureEncaissement(publicPasse, joueurSiteNord, PART);
        ensureEncaissement(publicPasse, joueurLibre, PART);
        ensureEncaissement(publicPasse, joueurSiteSud, PART);

        MatchPadel annule = ensureMatch(
                sudT2,
                joueurGlobal,
                today.plusDays(4).atTime(17, 0),
                MatchVisibilite.PUBLIC,
                MatchStatut.ANNULE
        );
        ensureParticipation(annule, joueurGlobal);
        ensureParticipation(annule, joueurSiteNord);
        ensureEncaissement(annule, joueurGlobal, PART);
        ensureEncaissement(annule, joueurSiteNord, PART);
        ensureRemboursement(annule, joueurGlobal, PART.negate());
        ensureRemboursement(annule, joueurSiteNord, PART.negate());

        log.info("""
                Seed dev initialise avec succes.
                Comptes de demo :
                - admin.global.dev / admin123
                - admin.site.nord.dev / admin123
                - joueur.global.dev / {}
                - joueur.site.dev / {}
                - joueur.libre.dev / {}
                - joueur.site.sud.dev / {}
                Support ADMIN_SITE garanti uniquement sur DB locale propre / vide.
                """, PLAYER_PASSWORD, PLAYER_PASSWORD, PLAYER_PASSWORD, PLAYER_PASSWORD);
    }

    private void validateAdminBootstrapOrderAndConfiguration() {
        if (adminSiteUsers == null || !adminSiteUsers.contains(EXPECTED_ADMIN_SITE_MAPPING)) {
            throw new IllegalStateException(
                    "Seed dev incompatible avec la configuration courante: la propriete app.security.admin.site.users doit contenir "
                            + EXPECTED_ADMIN_SITE_MAPPING
                            + "."
            );
        }

        if (adminGlobalUsername == null || adminGlobalUsername.isBlank()) {
            throw new IllegalStateException("Seed dev incompatible: app.security.admin.global.username est vide.");
        }

        if (userRepository.findByLogin(adminGlobalUsername).isEmpty()) {
            throw new IllegalStateException(
                    "Le bootstrap admin global n'a pas ete execute avant le seed (login attendu: "
                            + adminGlobalUsername
                            + ")."
            );
        }

        if (userRepository.findByLogin(EXPECTED_ADMIN_SITE_LOGIN).isEmpty()) {
            throw new IllegalStateException(
                    "Le bootstrap ADMIN_SITE n'a pas ete execute avant le seed (login attendu: "
                            + EXPECTED_ADMIN_SITE_LOGIN
                            + ")."
            );
        }
    }

    private void validateCleanDatabaseBeforeFirstSeed() {
        boolean dataExists = siteRepository.count() > 0
                || terrainRepository.count() > 0
                || horaireSiteRepository.count() > 0
                || joueurRepository.count() > 0
                || matchPadelRepository.count() > 0
                || participationRepository.count() > 0
                || paiementRepository.count() > 0;

        if (dataExists) {
            throw new IllegalStateException(
                    "app.seed.enabled=true requiert une DB locale propre / vide avant le premier seed. "
                            + "Des donnees metier existent deja, le seed dev one-shot est donc refuse."
            );
        }
    }

    private Site ensureSite(String nom, String ville) {
        Site site = siteRepository.findByNom(nom).orElseGet(() -> siteRepository.save(new Site(nom, ville)));
        site.setVille(ville);
        site.setJoursFermeture(Set.of());
        return siteRepository.save(site);
    }

    private void ensureHoraire(Site site, int year) {
        if (horaireSiteRepository.existsBySiteIdAndAnnee(site.getId(), year)) {
            return;
        }

        horaireSiteRepository.save(new HoraireSite(
                site,
                year,
                LocalTime.of(8, 0),
                LocalTime.of(23, 0)
        ));
    }

    private Terrain ensureTerrain(Site site, String nom) {
        return terrainRepository.findByNomAndSiteId(nom, site.getId())
                .orElseGet(() -> terrainRepository.save(new Terrain(nom, site)));
    }

    private Joueur ensureJoueur(String matricule,
                                String nom,
                                TypeJoueur type,
                                Site site,
                                BigDecimal solde) {
        Joueur joueur = joueurRepository.findById(matricule)
                .orElseGet(() -> site == null
                        ? new Joueur(matricule, nom, type)
                        : new Joueur(matricule, nom, type, site));

        joueur.setNom(nom);
        joueur.setType(type);
        joueur.setSite(site);
        joueur.setSolde(solde == null ? ZERO : solde.setScale(2, RoundingMode.HALF_UP));

        return joueurRepository.save(joueur);
    }

    private void ensurePlayerUser(String login, Joueur joueur) {
        User user = userRepository.findByLogin(login).orElseGet(User::new);
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(PLAYER_PASSWORD));
        user.setActive(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setJoueur(joueur);
        user.addRole(SecurityRole.ROLE_JOUEUR);
        userRepository.save(user);
    }

    private MatchPadel ensureMatch(Terrain terrain,
                                   Joueur organisateur,
                                   LocalDateTime dateDebut,
                                   MatchVisibilite visibilite,
                                   MatchStatut statut) {
        MatchPadel match = matchPadelRepository.findByTerrain_IdAndDateDebut(terrain.getId(), dateDebut)
                .orElseGet(() -> matchPadelRepository.save(new MatchPadel(terrain, organisateur, dateDebut, visibilite)));

        match.setTerrain(terrain);
        match.setOrganisateur(organisateur);
        match.setDateDebut(dateDebut);
        match.setVisibilite(visibilite);
        match.setStatut(statut);

        return matchPadelRepository.save(match);
    }

    private Participation ensureParticipation(MatchPadel match, Joueur joueur) {
        return participationRepository.findByMatch_IdAndJoueur_Matricule(match.getId(), joueur.getMatricule())
                .orElseGet(() -> participationRepository.save(new Participation(match, joueur)));
    }

    private void ensureEncaissement(MatchPadel match, Joueur joueur, BigDecimal montant) {
        Participation participation = ensureParticipation(match, joueur);
        if (paiementRepository.findByParticipation_IdAndType(participation.getId(), TypePaiement.ENCAISSEMENT).isPresent()) {
            return;
        }

        paiementRepository.save(new Paiement(
                participation,
                montant.setScale(2, RoundingMode.HALF_UP),
                TypePaiement.ENCAISSEMENT,
                match.getDateDebut().minusDays(2)
        ));
    }

    private void ensureRemboursement(MatchPadel match, Joueur joueur, BigDecimal montant) {
        Participation participation = ensureParticipation(match, joueur);
        if (paiementRepository.findByParticipation_IdAndType(participation.getId(), TypePaiement.REMBOURSEMENT).isPresent()) {
            return;
        }

        paiementRepository.save(new Paiement(
                participation,
                montant.setScale(2, RoundingMode.HALF_UP),
                TypePaiement.REMBOURSEMENT,
                match.getDateDebut().minusDays(1)
        ));
    }

    private void ensureSeedDebt(Participation participation, OrigineMouvementSoldeType origineType) {
        soldeService.debiter(
                participation.getJoueur().getMatricule(),
                PART,
                new SoldeOriginContext(
                        origineType,
                        participation.getId(),
                        participation.getMatch().getId(),
                        null
                )
        );
    }
}
