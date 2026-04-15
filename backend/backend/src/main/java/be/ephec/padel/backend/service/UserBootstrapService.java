package be.ephec.padel.backend.service;

import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.global.username:adminGlobal}")
    private String adminGlobalUsername;

    @Value("${app.security.admin.global.password:}")
    private String adminGlobalPassword;

    @Value("${app.security.admin.site.users:}")
    private String adminSiteUsers;

    @Value("${app.security.admin.site.password:}")
    private String adminSitePassword;

    public UserBootstrapService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void bootstrapAdmins() {
        validateBootstrapConfiguration();

        upsertAdmin(adminGlobalUsername, adminGlobalPassword, SecurityRole.ROLE_ADMIN_GLOBAL);

        if (adminSiteUsers == null || adminSiteUsers.isBlank()) {
            return;
        }

        for (String entry : adminSiteUsers.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split(":");
            String username = parts[0].trim();
            if (username.isEmpty()) {
                continue;
            }

            upsertAdmin(username, adminSitePassword, SecurityRole.ROLE_ADMIN_SITE);
        }
    }

    private void validateBootstrapConfiguration() {
        if (adminGlobalUsername == null || adminGlobalUsername.isBlank()) {
            throw new IllegalStateException(
                    "Missing value: app.security.admin.global.username"
            );
        }

        if (adminGlobalPassword == null || adminGlobalPassword.isBlank()) {
            throw new IllegalStateException(
                    "Missing secret: app.security.admin.global.password (env: APP_SECURITY_ADMIN_GLOBAL_PASSWORD)"
            );
        }

        if (adminSiteUsers != null && !adminSiteUsers.isBlank()
                && (adminSitePassword == null || adminSitePassword.isBlank())) {
            throw new IllegalStateException(
                    "Missing secret: app.security.admin.site.password (env: APP_SECURITY_ADMIN_SITE_PASSWORD)"
            );
        }
    }

    private void upsertAdmin(String login, String rawPassword, SecurityRole role) {
        User user = userRepository.findByLogin(login).orElseGet(User::new);
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setActive(true);
        user.setStatus(UserStatus.ACTIVE);
        user.addRole(role);
        userRepository.save(user);
    }
}
