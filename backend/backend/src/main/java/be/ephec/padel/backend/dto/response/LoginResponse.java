package be.ephec.padel.backend.dto.response;

import java.util.List;

public class LoginResponse {

    private String token;
    private String type;
    private List<String> roles = List.of();
    private boolean hasPlayerProfile;

    public LoginResponse() {
    }

    public LoginResponse(String token, String type) {
        this(token, type, List.of(), false);
    }

    public LoginResponse(String token, String type, List<String> roles, boolean hasPlayerProfile) {
        this.token = token;
        this.type = type;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.hasPlayerProfile = hasPlayerProfile;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean getHasPlayerProfile() {
        return hasPlayerProfile;
    }

    public void setHasPlayerProfile(boolean hasPlayerProfile) {
        this.hasPlayerProfile = hasPlayerProfile;
    }
}
