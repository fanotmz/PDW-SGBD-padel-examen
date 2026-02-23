package be.ephec.padel.backend.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiErrorDto {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /**
     * Optionnel : détails de validation (champ/param -> message).
     * Null si pas applicable.
     */
    private Map<String, String> details;

    public ApiErrorDto() {
        // Constructeur vide requis (Jackson)
    }

    public ApiErrorDto(LocalDateTime timestamp,
                       int status,
                       String error,
                       String message,
                       String path,
                       Map<String, String> details) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
}