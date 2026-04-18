package be.ephec.padel.backend.seed;

import be.ephec.padel.backend.service.UserBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev"})
@DependsOn(UserBootstrapService.BEAN_NAME)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevDataSeedRunner implements ApplicationRunner {

    private final DevDataSeeder devDataSeeder;

    public DevDataSeedRunner(DevDataSeeder devDataSeeder) {
        this.devDataSeeder = devDataSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        devDataSeeder.seed();
    }
}
