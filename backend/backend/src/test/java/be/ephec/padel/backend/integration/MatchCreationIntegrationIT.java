package be.ephec.padel.backend.integration;

import be.ephec.padel.backend.model.entities.FermetureGlobale;
import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import be.ephec.padel.backend.repository.FermetureSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.MouvementSoldeRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.support.SqlServerTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MatchCreationIntegrationIT extends SqlServerTestContainerConfig {

    @Autowired MockMvc mvc;

    @Autowired SiteRepository siteRepository;
    @Autowired FermetureSiteRepository fermetureSiteRepository;
    @Autowired TerrainRepository terrainRepository;
    @Autowired JoueurRepository joueurRepository;
    @Autowired FermetureGlobaleRepository fermetureGlobaleRepository;

    @Autowired MatchPadelRepository matchPadelRepository;
    @Autowired ParticipationRepository participationRepository;
    @Autowired PaiementRepository paiementRepository;
    @Autowired MouvementSoldeRepository mouvementSoldeRepository;

    private Site site;
    private Terrain terrain;
    private Joueur orga;

    @BeforeEach
    void cleanAndSeed() {
        // Nettoyage : toujours supprimer les enfants avant les parents
        paiementRepository.deleteAll();
        participationRepository.deleteAll();
        matchPadelRepository.deleteAll();
        mouvementSoldeRepository.deleteAll();
        fermetureSiteRepository.deleteAll();
        terrainRepository.deleteAll();
        fermetureGlobaleRepository.deleteAll();
        joueurRepository.deleteAll();
        siteRepository.deleteAll();

        // Seed minimal
        site = new Site("Site IT", "Bruxelles", LocalTime.of(8, 0), LocalTime.of(22, 0));
        site.setJoursFermeture(Set.of());
        site = siteRepository.save(site);

        terrain = new Terrain("Terrain IT", site);
        terrain = terrainRepository.save(terrain);

        orga = new Joueur("G0001", "Orga Test", TypeJoueur.GLOBAL);
        orga.setSolde(BigDecimal.ZERO);
        orga = joueurRepository.save(orga);
    }

    private String jsonCreateMatch(LocalDateTime dateDebut) {
        return """
                {
                  "terrainId": %d,
                  "organisateurMatricule": "%s",
                  "dateDebut": "%s",
                  "visibilite": "PUBLIC"
                }
                """.formatted(terrain.getId(), orga.getMatricule(), dateDebut.toString());
    }

    private static LocalDateTime nextDay(DayOfWeek day, int hour, int minute) {
        LocalDate d = LocalDate.now().with(TemporalAdjusters.next(day));
        return d.atTime(hour, minute);
    }

    @Test
    void postMatch_refuse_si_fermeture_globale() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.TUESDAY, 10, 0);
        fermetureGlobaleRepository.save(new FermetureGlobale(date.toLocalDate(), "Férié"));

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("fermeture globale")));
    }

    @Test
    void postMatch_refuse_si_site_ferme_ce_jour() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.SUNDAY, 10, 0);

        site.setJoursFermeture(Set.of(DayOfWeek.SUNDAY));
        site = siteRepository.save(site);

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("site fermé")));
    }

    @Test
    void postMatch_refuse_si_hors_horaires_fin_depasse_fermeture() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.WEDNESDAY, 21, 30);

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("en dehors des horaires")));
    }

    @Test
    void postMatch_ok_si_site_ouvert_et_dans_horaires_et_pas_de_fermeture_globale() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.THURSDAY, 10, 0);

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/matchs/")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void postMatch_refuse_si_fermeture_site_exceptionnelle_date_unique() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.TUESDAY, 10, 0);

        fermetureSiteRepository.save(new FermetureSite(
                site,
                date.toLocalDate(),
                null,
                null,
                "Congé exceptionnel"
        ));

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("site fermé à cette date")));
    }

    @Test
    void postMatch_refuse_si_fermeture_site_exceptionnelle_periode() throws Exception {
        LocalDateTime date = nextDay(DayOfWeek.WEDNESDAY, 10, 0);

        fermetureSiteRepository.save(new FermetureSite(
                site,
                null,
                date.toLocalDate().minusDays(1),
                date.toLocalDate().plusDays(1),
                "Travaux"
        ));

        mvc.perform(post("/api/v1/matchs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreateMatch(date)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("site fermé à cette date")));
    }
}