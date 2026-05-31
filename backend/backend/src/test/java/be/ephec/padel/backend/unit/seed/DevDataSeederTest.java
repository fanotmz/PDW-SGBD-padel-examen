package be.ephec.padel.backend.unit.seed;

import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.MatchPadelRepository;
import be.ephec.padel.backend.repository.PaiementRepository;
import be.ephec.padel.backend.repository.ParticipationRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.seed.DevDataSeeder;
import be.ephec.padel.backend.service.SoldeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    @Mock
    SiteRepository siteRepository;
    @Mock
    TerrainRepository terrainRepository;
    @Mock
    HoraireSiteRepository horaireSiteRepository;
    @Mock
    JoueurRepository joueurRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    MatchPadelRepository matchPadelRepository;
    @Mock
    ParticipationRepository participationRepository;
    @Mock
    PaiementRepository paiementRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    SoldeService soldeService;

    DevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneId.of("Europe/Brussels"));
        seeder = new DevDataSeeder(
                siteRepository,
                terrainRepository,
                horaireSiteRepository,
                joueurRepository,
                userRepository,
                matchPadelRepository,
                participationRepository,
                paiementRepository,
                passwordEncoder,
                soldeService,
                clock,
                "admin.global.dev",
                "admin.site.nord.dev:1"
        );
    }

    @Test
    void seed_skip_si_marqueur_global_deja_present() {
        when(userRepository.findByLogin("admin.global.dev")).thenReturn(Optional.of(new be.ephec.padel.backend.model.entities.User()));
        when(userRepository.findByLogin("admin.site.nord.dev")).thenReturn(Optional.of(new be.ephec.padel.backend.model.entities.User()));
        when(userRepository.existsByLogin("joueur.global.dev")).thenReturn(true);

        seeder.seed();

        verify(userRepository).existsByLogin("joueur.global.dev");
        verify(siteRepository, never()).count();
        verifyNoInteractions(terrainRepository, horaireSiteRepository, joueurRepository, matchPadelRepository, participationRepository, paiementRepository);
    }

    @Test
    void seed_fail_fast_si_mapping_admin_site_invalide() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneId.of("Europe/Brussels"));
        DevDataSeeder invalidSeeder = new DevDataSeeder(
                siteRepository,
                terrainRepository,
                horaireSiteRepository,
                joueurRepository,
                userRepository,
                matchPadelRepository,
                participationRepository,
                paiementRepository,
                passwordEncoder,
                soldeService,
                clock,
                "admin.global.dev",
                "admin.site.nord.dev:99"
        );

        assertThatThrownBy(invalidSeeder::seed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.admin.site.users");
    }

    @Test
    void seed_fail_fast_si_db_non_propre_avant_premier_seed() {
        when(userRepository.findByLogin("admin.global.dev")).thenReturn(Optional.of(new be.ephec.padel.backend.model.entities.User()));
        when(userRepository.findByLogin("admin.site.nord.dev")).thenReturn(Optional.of(new be.ephec.padel.backend.model.entities.User()));
        when(userRepository.existsByLogin("joueur.global.dev")).thenReturn(false);
        when(siteRepository.count()).thenReturn(1L);

        assertThatThrownBy(seeder::seed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB locale propre / vide");
    }
}
