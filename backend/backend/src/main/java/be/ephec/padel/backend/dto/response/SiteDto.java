package be.ephec.padel.backend.dto.response;

public class SiteDto {
    private Long id;
    private String nom;
    private String ville;

    public SiteDto(Long id, String nom, String ville) {
        this.id = id;
        this.nom = nom;
        this.ville = ville;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }
}
