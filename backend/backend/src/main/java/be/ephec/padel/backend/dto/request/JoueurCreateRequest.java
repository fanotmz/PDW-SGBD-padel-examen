package be.ephec.padel.backend.dto.request;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class JoueurCreateRequest {

    @NotBlank
    private String matricule;

    @NotBlank
    private String nom;

    @NotNull
    private TypeJoueur type;

    // Peut être null, ton service valide selon le type
    private Long siteId;

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public TypeJoueur getType() { return type; }
    public void setType(TypeJoueur type) { this.type = type; }

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
}
