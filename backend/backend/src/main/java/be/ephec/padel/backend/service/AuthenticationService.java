package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.LoginRequest;
import be.ephec.padel.backend.dto.response.LoginResponse;
import be.ephec.padel.backend.exception.ForbiddenException;
import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.UserStatus;
import be.ephec.padel.backend.repository.UserRepository;
import be.ephec.padel.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtService jwtService,
                                 UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        precheckUserStatus(request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        User authenticatedUser = userRepository.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new ForbiddenException("Utilisateur authentifié introuvable."));

        List<String> roles = authenticatedUser.getRoles().stream()
                .map(Enum::name)
                .toList();
        boolean hasPlayerProfile = authenticatedUser.getJoueur() != null;

        return new LoginResponse(token, "Bearer", roles, hasPlayerProfile);
    }

    private void precheckUserStatus(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        User user = userRepository.findByLogin(username.trim()).orElse(null);
        if (user == null) {
            return;
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new ForbiddenException("Compte en attente de validation administrateur.");
        }

        if (user.getStatus() == UserStatus.REJECTED) {
            throw new ForbiddenException("Demande d'inscription refusée.");
        }
    }
}
