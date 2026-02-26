package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.UpdateSiteHorairesRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public List<Site> lister() {
        return siteRepository.findAll();
    }

    public Site getSite(Long id) {
        if (id == null) throw new BusinessException("Id site obligatoire");
        return siteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));
    }

    public Site creerSite(String nom, String ville) {
        if (nom == null || nom.isBlank()) throw new BusinessException("Nom obligatoire");
        if (ville == null || ville.isBlank()) throw new BusinessException("Ville obligatoire");

        if (siteRepository.existsByNom(nom)) {
            throw new BusinessException("Nom de site déjà utilisé");
        }

        return siteRepository.save(new Site(nom, ville));
    }
    @Transactional
    public Site updateHoraires(Long siteId, UpdateSiteHorairesRequest req) {

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));

        if (!req.getHeureOuverture().isBefore(req.getHeureFermeture())) {
            throw new BusinessException("L'heure d'ouverture doit être avant l'heure de fermeture.");
        }

        site.setHeureOuverture(req.getHeureOuverture());
        site.setHeureFermeture(req.getHeureFermeture());

        site.getJoursFermeture().clear();
        if (req.getJoursFermeture() != null) {
            site.getJoursFermeture().addAll(req.getJoursFermeture());
        }

        return site;
    }
}
