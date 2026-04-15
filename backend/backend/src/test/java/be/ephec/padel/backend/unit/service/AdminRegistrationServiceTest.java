package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.dto.request.ValidateRegistrationRequest;
import be.ephec.padel.backend.dto.response.PendingRegistrationDto;
import be.ephec.padel.backend.dto.response.RegistrationDecisionResponse;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.service.AdminRegistrationService;
import be.ephec.padel.backend.service.JoueurService;
import be.ephec.padel.backend.service.MatriculeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRegistrationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    SiteRepository siteRepository;

    @Mock
    JoueurService joueurService;

    @Mock
    MatriculeGeneratorService matriculeGeneratorService;

    @InjectMocks
    AdminRegistrationService adminRegistrationService;

    @Test
    void listPendingRegistrations_expose_les_infos_utiles_au_front() throws Exception {
        Site site = site(3L, "Site Delta");
        User user = pendingUser(10L, "alice", "Alice", TypeJoueur.SITE);
        user.setRequestedSite(site);

        when(userRepository.findByStatusOrderByIdAsc(UserStatus.PENDING)).thenReturn(List.of(user));

        List<PendingRegistrationDto> result = adminRegistrationService.listPendingRegistrations();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getUserId()).isEqualTo(10L);
        assertThat(result.getFirst().getUsername()).isEqualTo("alice");
        assertThat(result.getFirst().getNomDemande()).isEqualTo("Alice");
        assertThat(result.getFirst().getTypeAbonnementDemande()).isEqualTo(TypeJoueur.SITE);
        assertThat(result.getFirst().getSiteIdDemande()).isEqualTo(3L);
        assertThat(result.getFirst().getSiteNomDemande()).isEqualTo("Site Delta");
    }

    @Test
    void validate_cree_le_joueur_lie_le_user_et_active_le_compte() throws Exception {
        User user = pendingUser(10L, "alice", "Alice", TypeJoueur.GLOBAL);
        Site site = site(7L, "Site Final");
        Joueur joueur = new Joueur("S0007", "Alice", TypeJoueur.SITE, site);

        ValidateRegistrationRequest request = new ValidateRegistrationRequest();
        request.setTypeAbonnementFinal(TypeJoueur.SITE);
        request.setSiteIdFinal(7L);

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(user));
        when(siteRepository.findById(7L)).thenReturn(Optional.of(site));
        when(matriculeGeneratorService.generateFor(TypeJoueur.SITE)).thenReturn("S0007");
        when(joueurService.creerJoueur("S0007", "Alice", TypeJoueur.SITE, 7L)).thenReturn(joueur);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationDecisionResponse response = adminRegistrationService.validate(10L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getJoueur()).isSameAs(joueur);

        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getJoueurMatricule()).isEqualTo("S0007");
        assertThat(response.getJoueurType()).isEqualTo(TypeJoueur.SITE);
        assertThat(response.getJoueurSiteId()).isEqualTo(7L);
    }

    @Test
    void reject_conserve_le_user_et_le_passe_en_rejected() throws Exception {
        User user = pendingUser(11L, "bob", "Bob", TypeJoueur.LIBRE);
        when(userRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationDecisionResponse response = adminRegistrationService.reject(11L);

        assertThat(user.isActive()).isFalse();
        assertThat(user.getStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(response.getStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(response.getMessage()).contains("refusée");
    }

    @Test
    void validate_refuse_si_la_demande_n_est_plus_pending() throws Exception {
        User user = pendingUser(12L, "charlie", "Charlie", TypeJoueur.GLOBAL);
        user.setStatus(UserStatus.REJECTED);

        ValidateRegistrationRequest request = new ValidateRegistrationRequest();
        request.setTypeAbonnementFinal(TypeJoueur.GLOBAL);

        when(userRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminRegistrationService.validate(12L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cette demande n'est plus en attente.");
    }

    @Test
    void validate_traduit_une_collision_de_matricule_en_erreur_metier_claire() throws Exception {
        User user = pendingUser(13L, "david", "David", TypeJoueur.GLOBAL);
        ValidateRegistrationRequest request = new ValidateRegistrationRequest();
        request.setTypeAbonnementFinal(TypeJoueur.GLOBAL);

        when(userRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(user));
        when(matriculeGeneratorService.generateFor(TypeJoueur.GLOBAL)).thenReturn("G0007");
        when(joueurService.creerJoueur("G0007", "David", TypeJoueur.GLOBAL, null))
                .thenThrow(new DataIntegrityViolationException(
                        "Violation of PRIMARY KEY constraint on dbo.joueur"
                ));

        assertThatThrownBy(() -> adminRegistrationService.validate(13L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Collision de matricule détectée. Veuillez relancer la validation.");
    }

    @Test
    void validate_traduit_un_conflit_de_liaison_user_joueur_en_erreur_metier_claire() throws Exception {
        User user = pendingUser(14L, "eve", "Eve", TypeJoueur.GLOBAL);
        Joueur joueur = new Joueur("G0008", "Eve", TypeJoueur.GLOBAL);
        ValidateRegistrationRequest request = new ValidateRegistrationRequest();
        request.setTypeAbonnementFinal(TypeJoueur.GLOBAL);

        when(userRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(user));
        when(matriculeGeneratorService.generateFor(TypeJoueur.GLOBAL)).thenReturn("G0008");
        when(joueurService.creerJoueur("G0008", "Eve", TypeJoueur.GLOBAL, null)).thenReturn(joueur);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Violation of UNIQUE INDEX on app_user.joueur_id"));

        assertThatThrownBy(() -> adminRegistrationService.validate(14L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Un joueur est déjà lié à un autre utilisateur.");
    }

    private User pendingUser(Long id, String login, String nom, TypeJoueur type) throws Exception {
        User user = new User();
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        user.setLogin(login);
        user.setRequestedNom(nom);
        user.setRequestedType(type);
        user.setStatus(UserStatus.PENDING);
        user.setActive(false);
        return user;
    }

    private Site site(Long id, String nom) throws Exception {
        Site site = new Site(nom, "Ville");
        Field field = Site.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(site, id);
        return site;
    }
}
