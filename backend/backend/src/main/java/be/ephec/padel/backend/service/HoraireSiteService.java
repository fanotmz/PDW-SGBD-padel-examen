package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.UpsertHoraireSiteRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.HoraireSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.HoraireSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class HoraireSiteService {

    private final HoraireSiteRepository horaireSiteRepository;
    private final SiteRepository siteRepository;

    public HoraireSiteService(HoraireSiteRepository horaireSiteRepository,
                              SiteRepository siteRepository) {
        this.horaireSiteRepository = horaireSiteRepository;
        this.siteRepository = siteRepository;
    }

    public HoraireSite create(Long siteId, UpsertHoraireSiteRequest req) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));

        validateRequest(req);

        if (horaireSiteRepository.existsBySiteIdAndAnnee(siteId, req.getAnnee())) {
            throw new BusinessException("Un horaire existe déjà pour ce site et cette année.");
        }

        HoraireSite horaire = new HoraireSite(
                site,
                req.getAnnee(),
                req.getHeureOuverture(),
                req.getHeureFermeture()
        );

        return horaireSiteRepository.save(horaire);
    }

    public HoraireSite update(Long siteId, Long horaireId, UpsertHoraireSiteRequest req) {
        HoraireSite horaire = horaireSiteRepository.findById(horaireId)
                .orElseThrow(() -> new NotFoundException("Horaire introuvable"));

        if (!horaire.getSite().getId().equals(siteId)) {
            throw new BusinessException("Horaire non associé à ce site.");
        }

        validateRequest(req);

        if (horaireSiteRepository.existsBySiteIdAndAnneeAndIdNot(siteId, req.getAnnee(), horaireId)) {
            throw new BusinessException("Un autre horaire existe déjà pour ce site et cette année.");
        }

        horaire.setAnnee(req.getAnnee());
        horaire.setHeureOuverture(req.getHeureOuverture());
        horaire.setHeureFermeture(req.getHeureFermeture());

        return horaire;
    }

    @Transactional(readOnly = true)
    public List<HoraireSite> listBySite(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new NotFoundException("Site introuvable");
        }
        return horaireSiteRepository.findBySiteIdOrderByAnneeAsc(siteId);
    }

    @Transactional(readOnly = true)
    public HoraireSite getBySiteAndAnnee(Long siteId, Integer annee) {
        return horaireSiteRepository.findBySiteIdAndAnnee(siteId, annee)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun horaire trouvé pour le site " + siteId + " en " + annee + "."
                ));
    }

    public void delete(Long siteId, Long horaireId) {
        HoraireSite horaire = horaireSiteRepository.findById(horaireId)
                .orElseThrow(() -> new NotFoundException("Horaire introuvable"));

        if (!horaire.getSite().getId().equals(siteId)) {
            throw new BusinessException("Horaire non associé à ce site.");
        }

        horaireSiteRepository.delete(horaire);
    }

    @Transactional(readOnly = true)
    public HoraireSite getApplicable(Long siteId, LocalDateTime dateDebut) {
        int annee = dateDebut.getYear();

        return horaireSiteRepository.findBySiteIdAndAnnee(siteId, annee)
                .orElseThrow(() -> new BusinessException(
                        "Aucun horaire configuré pour le site " + siteId + " en " + annee + "."
                ));
    }

    private void validateRequest(UpsertHoraireSiteRequest req) {
        if (req.getAnnee() == null) {
            throw new BusinessException("Année obligatoire");
        }
        if (req.getHeureOuverture() == null || req.getHeureFermeture() == null) {
            throw new BusinessException("Heures obligatoires");
        }
        if (!req.getHeureOuverture().isBefore(req.getHeureFermeture())) {
            throw new BusinessException("L'heure d'ouverture doit être avant l'heure de fermeture.");
        }
    }
}