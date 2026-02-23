package be.ephec.padel.backend.dto.response;

public class TerrainDto {
    private Long id;
    private String nom;
    private Long siteId;

    public TerrainDto(Long id, String nom, Long siteId) {
        this.id = id;
        this.nom = nom;
        this.siteId = siteId;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public Long getSiteId() { return siteId; }
}
