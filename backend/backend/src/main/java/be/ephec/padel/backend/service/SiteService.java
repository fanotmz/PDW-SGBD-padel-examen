package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class SiteService {

    private final SiteRepository siteRepository;
    private final HoraireSiteRepository horaireSiteRepository;
    public SiteService(SiteRepository siteRepository, HoraireSiteRepository horaireSiteRepository) {
        this.siteRepository = siteRepository;
        this.horaireSiteRepository = horaireSiteRepository;
    }

    public List<Site> lister() {
        return siteRepository.findAll();
    }

    public Site getSite(Long id) {
        if (id == null) throw new BusinessException("Id site obligatoire");
        return siteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));
    }

    @Transactional
    public Site creerSite(String nom, String ville,
                          Integer annee,
                          LocalTime heureOuverture,
                          LocalTime heureFermeture) {
        if (nom == null || nom.isBlank()) {
            throw new BusinessException("Nom obligatoire");
        }
        if (ville == null || ville.isBlank()) {
            throw new BusinessException("Ville obligatoire");
        }
        if (annee == null) {
            throw new BusinessException("Année obligatoire");
        }
        if (heureOuverture == null || heureFermeture == null) {
            throw new BusinessException("Horaires obligatoires");
        }
        if (!heureOuverture.isBefore(heureFermeture)) {
            throw new BusinessException("L'heure d'ouverture doit être avant l'heure de fermeture.");
        }
        if (siteRepository.existsByNom(nom)) {
            throw new BusinessException("Nom de site déjà utilisé");
        }

        Site site = siteRepository.save(new Site(nom, ville));

        horaireSiteRepository.save(new HoraireSite(site, annee, heureOuverture, heureFermeture));

        return site;
    }
}
