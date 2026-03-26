package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.security.PersistentUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistentUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    PersistentUserDetailsService persistentUserDetailsService;

    @Test
    void loadUserByUsername_mappe_login_hash_roles_et_active() {
        User user = new User();
        user.setLogin("player1");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setRoles(Set.of(SecurityRole.ROLE_JOUEUR, SecurityRole.ROLE_ADMIN_SITE));

        when(userRepository.findByLogin("player1")).thenReturn(Optional.of(user));

        UserDetails details = persistentUserDetailsService.loadUserByUsername("player1");

        assertThat(details.getUsername()).isEqualTo("player1");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_JOUEUR", "ROLE_ADMIN_SITE");
    }

    @Test
    void loadUserByUsername_404_logique_si_login_inconnu() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> persistentUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
