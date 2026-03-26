package be.ephec.padel.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

import be.ephec.padel.backend.security.PersistentUserDetailsService;
import be.ephec.padel.backend.repository.UserRepository;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Swagger / OpenAPI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // Login
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // ===== ADMIN =====

                        // Stats globales : ADMIN_GLOBAL uniquement
                        .requestMatchers("/api/v1/admin/stats/**").hasRole("ADMIN_GLOBAL")

                        // Endpoints admin par site : ADMIN_GLOBAL ou ADMIN_SITE
                        .requestMatchers("/api/v1/admin/sites/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")

                        // Autres endpoints admin : ADMIN_GLOBAL ou ADMIN_SITE
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")

                        // ===== API publique mais avec exceptions sensibles (Issue 62) =====

                        // Joueurs : listing + création réservés à l'admin global
                        .requestMatchers(HttpMethod.GET, "/api/v1/joueurs").hasRole("ADMIN_GLOBAL")
                        .requestMatchers(HttpMethod.POST, "/api/v1/joueurs").hasRole("ADMIN_GLOBAL")

                        // Sites : création + update horaires réservés à l'admin global
                        .requestMatchers(HttpMethod.POST, "/api/v1/sites").hasRole("ADMIN_GLOBAL")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/sites/*/horaires").hasRole("ADMIN_GLOBAL")

                        // Terrains : création réservée à l'admin global
                        .requestMatchers(HttpMethod.POST, "/api/v1/terrains").hasRole("ADMIN_GLOBAL")

                        // Fermetures globales : création + suppression réservées à l'admin global
                        .requestMatchers(HttpMethod.POST, "/api/v1/fermetures-globales").hasRole("ADMIN_GLOBAL")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/fermetures-globales/*").hasRole("ADMIN_GLOBAL")

                        // Tout le reste sous /api/v1 reste public (pour l’instant)
                        .requestMatchers("/api/v1/**").permitAll()

                        // Fallback
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService(ObjectProvider<UserRepository> provider) {
        UserRepository repository = provider.getIfAvailable();
        if (repository != null) {
            return new PersistentUserDetailsService(repository);
        }
        return username -> {
            throw new UsernameNotFoundException("UserDetailsService unavailable in this context");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
