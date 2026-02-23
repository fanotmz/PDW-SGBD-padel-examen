package be.ephec.padel.backend.dto.response;

public class ParticipationDto {

    private Long id;
    private Long matchId;
    private String joueurMatricule;

    public ParticipationDto(Long id, Long matchId, String joueurMatricule) {
        this.id = id;
        this.matchId = matchId;
        this.joueurMatricule = joueurMatricule;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public String getJoueurMatricule() { return joueurMatricule; }
}
