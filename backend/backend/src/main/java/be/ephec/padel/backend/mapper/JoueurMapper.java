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
                solde,
                joueur.getPenaliteJusqua()
        );
    }

    public static Joueur toEntity(JoueurCreateRequest req) {
        if (req == null) return null;

        return new Joueur(
                req.getMatricule(),
                req.getNom(),
                req.getType()
        );
    }

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
