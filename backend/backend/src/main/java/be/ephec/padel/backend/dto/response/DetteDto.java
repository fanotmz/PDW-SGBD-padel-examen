package be.ephec.padel.backend.dto.response;

public class DetteDto {
    private boolean dette;

    public DetteDto(boolean dette) { this.dette = dette; }
    public boolean isDette() { return dette; }
}
