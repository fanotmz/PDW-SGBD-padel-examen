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

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        service.traiterDebutMatchFenetreMinutes(5);
    }
}
