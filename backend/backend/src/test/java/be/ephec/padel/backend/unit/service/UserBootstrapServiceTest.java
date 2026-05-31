package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.UserBootstrapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBootstrapServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserBootstrapService userBootstrapService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userBootstrapService, "adminGlobalUsername", "adminGlobal");
        ReflectionTestUtils.setField(userBootstrapService, "adminGlobalPassword", "globalRaw");
        ReflectionTestUtils.setField(userBootstrapService, "adminSiteUsers", "adminSite1:1,adminSite2:2");
        ReflectionTestUtils.setField(userBootstrapService, "adminSitePassword", "siteRaw");
    }

    @Test
    void bootstrap_cree_les_admins_attendus() {
        when(userRepository.findByLogin(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(eq("globalRaw"))).thenReturn("hash-global");
        when(passwordEncoder.encode(eq("siteRaw"))).thenReturn("hash-site");

        userBootstrapService.bootstrapAdmins();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(User::getLogin)
                .containsExactly("adminGlobal", "adminSite1", "adminSite2");

        assertThat(captor.getAllValues().get(0).getRoles())
                .containsExactly(SecurityRole.ROLE_ADMIN_GLOBAL);
        assertThat(captor.getAllValues().get(1).getRoles())
                .containsExactly(SecurityRole.ROLE_ADMIN_SITE);
        assertThat(captor.getAllValues().get(2).getRoles())
                .containsExactly(SecurityRole.ROLE_ADMIN_SITE);
        assertThat(captor.getAllValues())
                .extracting(User::getStatus)
                .containsOnly(UserStatus.ACTIVE);
    }
}
