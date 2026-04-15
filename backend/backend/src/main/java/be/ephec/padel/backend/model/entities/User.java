package be.ephec.padel.backend.model.entities;

import be.ephec.padel.backend.model.enums.SecurityRole;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.UserStatus;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "requested_nom", length = 255)
    private String requestedNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_type", length = 20)
    private TypeJoueur requestedType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_site_id")
    private Site requestedSite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joueur_id", referencedColumnName = "id")
    private Joueur joueur;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Set<SecurityRole> roles = new LinkedHashSet<>();

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getRequestedNom() {
        return requestedNom;
    }

    public void setRequestedNom(String requestedNom) {
        this.requestedNom = requestedNom;
    }

    public TypeJoueur getRequestedType() {
        return requestedType;
    }

    public void setRequestedType(TypeJoueur requestedType) {
        this.requestedType = requestedType;
    }

    public Site getRequestedSite() {
        return requestedSite;
    }

    public void setRequestedSite(Site requestedSite) {
        this.requestedSite = requestedSite;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public void setJoueur(Joueur joueur) {
        this.joueur = joueur;
    }

    public Set<SecurityRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<SecurityRole> roles) {
        this.roles = (roles == null) ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
    }

    public void addRole(SecurityRole role) {
        if (role != null) {
            roles.add(role);
        }
    }
}
