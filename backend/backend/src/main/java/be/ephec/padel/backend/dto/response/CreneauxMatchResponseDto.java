package be.ephec.padel.backend.dto.response;

import java.util.List;

public class CreneauxMatchResponseDto {

    private List<String> creneaux;
    private String message;

    public CreneauxMatchResponseDto(List<String> creneaux, String message) {
        this.creneaux = creneaux;
        this.message = message;
    }

    public List<String> getCreneaux() {
        return creneaux;
    }

    public String getMessage() {
        return message;
    }
}
