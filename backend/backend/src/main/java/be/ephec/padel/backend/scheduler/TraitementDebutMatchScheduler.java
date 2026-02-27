package be.ephec.padel.backend.scheduler;

import be.ephec.padel.backend.service.TraitementDebutMatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TraitementDebutMatchScheduler {

    private final TraitementDebutMatchService service;

    public TraitementDebutMatchScheduler(TraitementDebutMatchService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000) // toutes les 1 minute
    public void run() {
        // fenêtre de 5 minutes "dans le passé" pour ne rater aucun match
        service.traiterDebutMatchFenetreMinutes(5);
    }
}