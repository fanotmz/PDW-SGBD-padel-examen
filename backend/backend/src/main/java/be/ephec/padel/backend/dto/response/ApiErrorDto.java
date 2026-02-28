package be.ephec.padel.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Format standard d'erreur renvoyé par l'API")
public class ApiErrorDto {

    @Schema(example = "2026-02-27T22:22:23.984853714")
    private LocalDateTime timestamp;

    @Schema(example = "400")
    private int status;

    @Schema(example = "Bad Request")
    private String error;

    @Schema(example = "Validation failed")
    private String message;

    @Schema(example = "/api/v1/matchs")
    private String path;

    @Schema(
            description = "Détails de validation (champ/param -> message). Null si pas applicable.",
            example = "{\"dateDebut\":\"La date de début doit être dans le futur\",\"terrainId\":\"doit être supérieur à 0\"}"
    )
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