package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.RegisterRequest;
import be.ephec.padel.backend.dto.response.RegisterResponse;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SiteRepository siteRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    RegistrationService registrationService;

    @Test
    void register_cree_un_user_pending_sans_joueur() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setNom("Alice");
        request.setTypeAbonnementDemande(TypeJoueur.GLOBAL);

        when(userRepository.existsByLogin("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, 12L);
            return user;
        });

        RegisterResponse response = registrationService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getLogin()).isEqualTo("alice");
        assertThat(savedUser.getPasswordHash()).isEqualTo("hash");
        assertThat(savedUser.isActive()).isFalse();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getRequestedNom()).isEqualTo("Alice");
        assertThat(savedUser.getRequestedType()).isEqualTo(TypeJoueur.GLOBAL);
        assertThat(savedUser.getRequestedSite()).isNull();
        assertThat(savedUser.getJoueur()).isNull();
        assertThat(savedUser.getRoles()).containsExactly(SecurityRole.ROLE_JOUEUR);

        assertThat(response.getUserId()).isEqualTo(12L);
        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(response.getMessage()).contains("en attente");
    }

    @Test
    void register_refuse_si_username_deja_utilise() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setNom("Alice");
        request.setTypeAbonnementDemande(TypeJoueur.GLOBAL);

        when(userRepository.existsByLogin("alice")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Username deja utilise");
    }

    @Test
    void register_refuse_si_type_site_sans_siteId() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("bob");
        request.setPassword("secret123");
        request.setNom("Bob");
        request.setTypeAbonnementDemande(TypeJoueur.SITE);

        when(userRepository.existsByLogin("bob")).thenReturn(false);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("siteIdDemande");
    }

    @Test
    void register_lie_le_site_demande_si_type_site() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("carol");
        request.setPassword("secret123");
        request.setNom("Carol");
        request.setTypeAbonnementDemande(TypeJoueur.SITE);
        request.setSiteIdDemande(5L);

        Site site = new Site("Delta", "Bruxelles");
        java.lang.reflect.Field field = Site.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(site, 5L);

        when(userRepository.existsByLogin("carol")).thenReturn(false);
        when(siteRepository.findById(5L)).thenReturn(Optional.of(site));
        when(passwordEncoder.encode(eq("secret123"))).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRequestedSite()).isSameAs(site);
    }
}
