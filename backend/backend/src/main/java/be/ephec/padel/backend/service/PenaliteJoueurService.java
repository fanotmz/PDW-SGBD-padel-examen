package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class PenaliteJoueurService {

    private final Clock clock;

    public PenaliteJoueurService(Clock clock) {
        this.clock = clock;
    }

    public void appliquerPenaliteReservation(Joueur joueur) {
        if (joueur == null) {
            throw new BusinessException("Joueur obligatoire pour appliquer une penalite");
        }

        LocalDateTime finPenalite = LocalDateTime.now(clock)
                .toLocalDate()
                .plusDays(7)
                .atTime(LocalTime.MAX);

        LocalDateTime penaliteExistante = joueur.getPenaliteJusqua();
        if (penaliteExistante == null || penaliteExistante.isBefore(finPenalite)) {
            joueur.setPenaliteJusqua(finPenalite);
        }
    }
}
