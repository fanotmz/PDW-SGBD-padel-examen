package be.ephec.padel.backend.config;

import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.security.JwtAuthenticationFilter;
import be.ephec.padel.backend.security.JwtService;
import be.ephec.padel.backend.security.PersistentUserDetailsService;
import be.ephec.padel.backend.security.RestAccessDeniedHandler;
import be.ephec.padel.backend.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final boolean swaggerPermitAll;

    public SecurityConfig(@Value("${app.security.swagger.permit-all:true}") boolean swaggerPermitAll) {
        this.swaggerPermitAll = swaggerPermitAll;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           AuthenticationEntryPoint authenticationEntryPoint,
                                           AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> {
                    if (swaggerPermitAll) {
                        auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                    }

                    auth
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                            .requestMatchers("/api/v1/admin/inscriptions/**").hasRole("ADMIN_GLOBAL")
                            .requestMatchers("/api/v1/admin/stats/**").hasRole("ADMIN_GLOBAL")
                            .requestMatchers("/api/v1/admin/sites/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")
                            .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN_GLOBAL", "ADMIN_SITE")
                            .requestMatchers(HttpMethod.GET, "/api/v1/joueurs").hasRole("ADMIN_GLOBAL")
                            .requestMatchers(HttpMethod.POST, "/api/v1/joueurs").hasRole("ADMIN_GLOBAL")
                            .requestMatchers(HttpMethod.POST, "/api/v1/sites").hasRole("ADMIN_GLOBAL")
                            .requestMatchers(HttpMethod.POST, "/api/v1/terrains").hasRole("ADMIN_GLOBAL")
                            .requestMatchers(HttpMethod.POST, "/api/v1/fermetures-globales").hasRole("ADMIN_GLOBAL")
                            .requestMatchers(HttpMethod.DELETE, "/api/v1/fermetures-globales/*").hasRole("ADMIN_GLOBAL")
                            .requestMatchers("/api/v1/**").authenticated()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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

    @Bean
    public JwtService jwtService(
            @Value("${app.security.jwt.secret:cle-secrete-jwt-local-dev-ephec-padel-2026-123456789}") String secret,
            @Value("${app.security.jwt.expiration-ms:3600000}") long expirationMs) {
        return new JwtService(secret, expirationMs);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                           UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new RestAccessDeniedHandler(objectMapper);
    }
}
