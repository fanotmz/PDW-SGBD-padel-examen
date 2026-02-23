package be.ephec.padel.backend.mapper;

import be.ephec.padel.backend.dto.request.JoueurCreateRequest;
import be.ephec.padel.backend.dto.response.JoueurDto;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.Site;

import java.math.BigDecimal;

public final class JoueurMapper {

    private JoueurMapper() {}

    public static JoueurDto toDto(Joueur joueur) {
        if (joueur == null) return null;

        Long siteId = null;
        if (joueur.getSite() != null) {
            siteId = joueur.getSite().getId();
        }

        BigDecimal solde = joueur.getSolde() != null ? joueur.getSolde() : BigDecimal.ZERO;

        return new JoueurDto(
                joueur.getMatricule(),
                joueur.getNom(),
                joueur.getType(),
                siteId,
                solde
        );
    }

    /**
     * Mapping request -> entity.
     * IMPORTANT : l'entity Joueur n'a pas de setMatricule(), donc on utilise le constructeur.
     * Le Site (si nécessaire) doit être résolu par le service (lookup + règles métier).
     */
    public static Joueur toEntity(JoueurCreateRequest req) {
        if (req == null) return null;

        return new Joueur(
                req.getMatricule(),
                req.getNom(),
                req.getType()
        );
    }

    /**
     * Variante utile si ton service a déjà résolu le Site à partir de siteId.
     */
    public static Joueur toEntity(JoueurCreateRequest req, Site site) {
        if (req == null) return null;

        return new Joueur(
                req.getMatricule(),
                req.getNom(),
                req.getType(),
                site
        );
    }
}