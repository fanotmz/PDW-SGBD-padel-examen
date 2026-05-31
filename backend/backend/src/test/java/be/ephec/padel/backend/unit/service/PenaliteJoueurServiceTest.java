package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.service.PenaliteJoueurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PenaliteJoueurServiceTest {

    private PenaliteJoueurService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2030-01-01T09:00:00Z"),
                ZoneId.of("UTC")
        );
        service = new PenaliteJoueurService(clock);
    }

    @Test
    void appliquePenaliteSiAbsente() {
        Joueur joueur = new Joueur("G0001", "Joueur", TypeJoueur.GLOBAL);

        service.appliquerPenaliteReservation(joueur);

        assertThat(joueur.getPenaliteJusqua())
                .isEqualTo(LocalDateTime.of(2030, 1, 8, 23, 59, 59, 999999999));
    }

    @Test
    void prolongeSiPenaliteExistantePlusCourte() {
        Joueur joueur = new Joueur("G0001", "Joueur", TypeJoueur.GLOBAL);
        joueur.setPenaliteJusqua(LocalDateTime.of(2030, 1, 3, 12, 0));

        service.appliquerPenaliteReservation(joueur);

        assertThat(joueur.getPenaliteJusqua())
                .isEqualTo(LocalDateTime.of(2030, 1, 8, 23, 59, 59, 999999999));
    }

    @Test
    void neRaccourcitPasPenaliteExistantePlusLongue() {
        Joueur joueur = new Joueur("G0001", "Joueur", TypeJoueur.GLOBAL);
        LocalDateTime existante = LocalDateTime.of(2030, 1, 20, 0, 0);
        joueur.setPenaliteJusqua(existante);

        service.appliquerPenaliteReservation(joueur);

        assertThat(joueur.getPenaliteJusqua()).isEqualTo(existante);
    }
}
