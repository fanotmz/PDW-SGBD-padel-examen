package be.ephec.padel.backend.dto.response;

import be.ephec.padel.backend.model.enums.UserStatus;

public class RegisterResponse {

    private Long userId;
    private String username;
    private UserStatus status;
    private String message;

    public RegisterResponse(Long userId, String username, UserStatus status, String message) {
        this.userId = userId;
        this.username = username;
        this.status = status;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
