package be.ephec.padel.backend.scheduler;

import be.ephec.padel.backend.service.TraitementJ1Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TraitementJ1Scheduler {

    private final TraitementJ1Service traitementJ1Service;

    public TraitementJ1Scheduler(TraitementJ1Service traitementJ1Service) {
        this.traitementJ1Service = traitementJ1Service;
    }

    @Scheduled(fixedDelay = 300_000)
    public void run() {
        traitementJ1Service.traiterJ1FenetreMinutes(5);
    }
}
