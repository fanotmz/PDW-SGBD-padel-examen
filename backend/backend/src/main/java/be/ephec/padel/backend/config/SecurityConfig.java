package be.ephec.padel.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${app.security.admin.global.username:adminGlobal}")
    private String adminGlobalUsername;

    // Pas de mot de passe par défaut (doit venir d'une env var / secret)
    @Value("${app.security.admin.global.password:}")
    private String adminGlobalPassword;

    /**
     * Admins site sous forme:
     *   app.security.admin.site.users=adminSite1:1,adminSite2:2
     * Le ":siteId" est utilisé ailleurs (périmètre). Ici on ne garde que le username.
     */
    @Value("${app.security.admin.site.users:adminSite1:1,adminSite2:2}")
    private String adminSiteUsers;

    // Pas de mot de passe par défaut (doit venir d'une env var / secret)
    @Value("${app.security.admin.site.password:}")
    private String adminSitePassword;

    @PostConstruct
    void validateSecrets() {
        if (adminGlobalPassword == null || adminGlobalPassword.isBlank()) {
            throw new IllegalStateException(
                    "Missing secret: app.security.admin.global.password (env: APP_SECURITY_ADMIN_GLOBAL_PASSWORD)"
            );
        }
        if (adminSitePassword == null || adminSitePassword.isBlank()) {
            throw new IllegalStateException(
                    "Missing secret: app.security.admin.site.password (env: APP_SECURITY_ADMIN_SITE_PASSWORD)"
            );
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // Global-only
                        .requestMatchers("/api/v1/admin/stats/**").hasRole("ADMIN_GLOBAL")

                        // Global ou Site
                        .requestMatchers("/api/v1/admin/sites/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")

                        // API publique
                        .requestMatchers("/api/v1/**").permitAll()

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder) {
        List<org.springframework.security.core.userdetails.UserDetails> users = new ArrayList<>();

        // Admin global
        users.add(User.builder()
                .username(adminGlobalUsername)
                .password(encoder.encode(adminGlobalPassword))
                .roles("ADMIN_GLOBAL")
                .build());

        // Admins site (on ignore le :siteId ici)
        for (String entry : adminSiteUsers.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            String[] parts = trimmed.split(":");
            String username = parts[0].trim();

            if (username.isEmpty()) continue;

            users.add(User.builder()
                    .username(username)
                    .password(encoder.encode(adminSitePassword))
                    .roles("ADMIN_SITE")
                    .build());
        }

        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}