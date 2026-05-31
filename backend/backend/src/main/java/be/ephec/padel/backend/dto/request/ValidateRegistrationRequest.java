package be.ephec.padel.backend.dto.request;

import be.ephec.padel.backend.model.enums.TypeJoueur;
import jakarta.validation.constraints.NotNull;

public class ValidateRegistrationRequest {

    @NotNull
    private TypeJoueur typeAbonnementFinal;

    private Long siteIdFinal;

    public TypeJoueur getTypeAbonnementFinal() {
        return typeAbonnementFinal;
    }

    public void setTypeAbonnementFinal(TypeJoueur typeAbonnementFinal) {
        this.typeAbonnementFinal = typeAbonnementFinal;
    }

    public Long getSiteIdFinal() {
        return siteIdFinal;
    }

    public void setSiteIdFinal(Long siteIdFinal) {
        this.siteIdFinal = siteIdFinal;
    }
}
