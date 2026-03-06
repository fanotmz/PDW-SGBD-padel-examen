package be.ephec.padel.backend.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class SqlServerTestContainerConfig {

    @Container
    static final MSSQLServerContainer<?> SQLSERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withStartupTimeout(Duration.ofMinutes(3));

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {

        // Démarre explicitement le container avant que Spring ne tente la première connexion
        if (!SQLSERVER.isRunning()) {
            SQLSERVER.start();
        }

        registry.add("spring.datasource.url", SQLSERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQLSERVER::getUsername);
        registry.add("spring.datasource.password", SQLSERVER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // IMPORTANT : éviter "create" sur SQL Server (drop de contraintes sur tables inexistantes => erreurs)
        // "update" crée le schéma si absent, sans phase drop agressive.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // Inutile (et parfois contradictoire) de setter aussi hbm2ddl.auto à part.
        // registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "update");

        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
    }
}