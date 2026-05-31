package be.ephec.padel.backend.dto.response;

import java.time.LocalTime;
import java.util.List;

public class CreneauxMatchResponseDto {

    private List<String> creneaux;
    private String message;
    private Integer annee;
    private LocalTime heureOuverture;
    private LocalTime heureFermeture;
    private Long dureeMatchMinutes;
    private Long bufferMinutes;

    public CreneauxMatchResponseDto(List<String> creneaux, String message) {
        this(creneaux, message, null, null, null, null, null);
    }

    public CreneauxMatchResponseDto(List<String> creneaux,
                                    String message,
                                    Integer annee,
                                    LocalTime heureOuverture,
                                    LocalTime heureFermeture,
                                    Long dureeMatchMinutes,
                                    Long bufferMinutes) {
        this.creneaux = creneaux;
        this.message = message;
        this.annee = annee;
        this.heureOuverture = heureOuverture;
        this.heureFermeture = heureFermeture;
        this.dureeMatchMinutes = dureeMatchMinutes;
        this.bufferMinutes = bufferMinutes;
    }

    public List<String> getCreneaux() {
        return creneaux;
    }

    public String getMessage() {
        return message;
    }

    public Integer getAnnee() {
        return annee;
    }

    public LocalTime getHeureOuverture() {
        return heureOuverture;
    }

    public LocalTime getHeureFermeture() {
        return heureFermeture;
    }

    public Long getDureeMatchMinutes() {
        return dureeMatchMinutes;
    }

    public Long getBufferMinutes() {
        return bufferMinutes;
    }
}
