package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.CreateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.CreateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSiteDateRequest;
import be.ephec.padel.backend.dto.request.UpdateFermetureSitePeriodeRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.FermetureSite;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.repository.FermetureSiteRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FermetureSiteService {

    private final FermetureSiteRepository fermetureSiteRepository;
    private final SiteRepository siteRepository;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    public FermetureSiteService(FermetureSiteRepository fermetureSiteRepository,
                                SiteRepository siteRepository,
                                ServiceAutorisationAdmin serviceAutorisationAdmin) {
        this.fermetureSiteRepository = fermetureSiteRepository;
        this.siteRepository = siteRepository;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
    }

    @Transactional(readOnly = true)
    public List<FermetureSite> listerParSite(Long siteId) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);
        getSiteOrThrow(siteId);
        return fermetureSiteRepository.findBySiteIdOrderByDateAscDateDebutAsc(siteId);
    }

    @Transactional(readOnly = true)
    public FermetureSite getById(Long siteId, Long fermetureId) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        if (fermetureId == null) {
            throw new BusinessException("Id fermeture obligatoire");
        }

        getSiteOrThrow(siteId);

        return fermetureSiteRepository.findByIdAndSiteId(fermetureId, siteId)
                .orElseThrow(() -> new NotFoundException("Fermeture site introuvable"));
    }

    public FermetureSite creerDate(Long siteId, CreateFermetureSiteDateRequest req) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        Site site = getSiteOrThrow(siteId);
        validerRequestDate(req);

        if (fermetureSiteRepository.existsBySiteIdAndDate(siteId, req.getDate())) {
            throw new BusinessException("Une fermeture existe déjà pour cette date");
        }

        if (fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                siteId, req.getDate(), req.getDate())) {
            throw new BusinessException("Cette date est déjà couverte par une période de fermeture");
        }

        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(req.getDate());
        fermeture.setDateDebut(null);
        fermeture.setDateFin(null);
        fermeture.setMotif(req.getMotif());

        return fermetureSiteRepository.save(fermeture);
    }

    public FermetureSite creerPeriode(Long siteId, CreateFermetureSitePeriodeRequest req) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        Site site = getSiteOrThrow(siteId);
        validerRequestPeriode(req);

        if (fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                siteId, req.getDateDebut(), req.getDateFin())) {
            throw new BusinessException("Une fermeture existe déjà pour cette période");
        }

        if (fermetureSiteRepository.existsPeriodeChevauchante(
                siteId, req.getDateDebut(), req.getDateFin())) {
            throw new BusinessException("Cette période chevauche une autre fermeture");
        }

        if (fermetureSiteRepository.existsBySiteIdAndDateBetween(
                siteId, req.getDateDebut(), req.getDateFin())) {
            throw new BusinessException("Cette période contient déjà une date de fermeture");
        }

        FermetureSite fermeture = new FermetureSite();
        fermeture.setSite(site);
        fermeture.setDate(null);
        fermeture.setDateDebut(req.getDateDebut());
        fermeture.setDateFin(req.getDateFin());
        fermeture.setMotif(req.getMotif());

        return fermetureSiteRepository.save(fermeture);
    }

    public FermetureSite updateDate(Long siteId, Long fermetureId, UpdateFermetureSiteDateRequest req) {
        FermetureSite fermeture = getById(siteId, fermetureId);
        validerRequestDate(req);

        boolean duplicateDate = fermetureSiteRepository.existsBySiteIdAndDate(siteId, req.getDate());
        boolean sameAsCurrentDate = req.getDate().equals(fermeture.getDate());

        if (duplicateDate && !sameAsCurrentDate) {
            throw new BusinessException("Une fermeture existe déjà pour cette date");
        }

        boolean coveredByPeriod = fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                siteId, req.getDate(), req.getDate());

        if (coveredByPeriod) {
            throw new BusinessException("Cette date est déjà couverte par une période de fermeture");
        }

        fermeture.setDate(req.getDate());
        fermeture.setDateDebut(null);
        fermeture.setDateFin(null);
        fermeture.setMotif(req.getMotif());

        return fermetureSiteRepository.save(fermeture);
    }

    public FermetureSite updatePeriode(Long siteId, Long fermetureId, UpdateFermetureSitePeriodeRequest req) {
        FermetureSite fermeture = getById(siteId, fermetureId);
        validerRequestPeriode(req);

        boolean sameAsCurrent =
                req.getDateDebut().equals(fermeture.getDateDebut())
                        && req.getDateFin().equals(fermeture.getDateFin());

        boolean duplicate = fermetureSiteRepository.existsBySiteIdAndDateDebutAndDateFin(
                siteId, req.getDateDebut(), req.getDateFin());

        if (duplicate && !sameAsCurrent) {
            throw new BusinessException("Une fermeture existe déjà pour cette période");
        }

        verifierChevauchementPeriodePourUpdate(siteId, fermetureId, req.getDateDebut(), req.getDateFin());

        fermeture.setDate(null);
        fermeture.setDateDebut(req.getDateDebut());
        fermeture.setDateFin(req.getDateFin());
        fermeture.setMotif(req.getMotif());

        return fermetureSiteRepository.save(fermeture);
    }

    public void delete(Long siteId, Long fermetureId) {
        FermetureSite fermeture = getById(siteId, fermetureId);
        fermetureSiteRepository.delete(fermeture);
    }

    @Transactional(readOnly = true)
    public boolean isDateFermeePourSite(Long siteId, LocalDate date) {
        if (siteId == null) {
            throw new BusinessException("Id site obligatoire");
        }
        if (date == null) {
            throw new BusinessException("Date obligatoire");
        }

        return fermetureSiteRepository.existsBySiteIdAndDate(siteId, date)
                || fermetureSiteRepository.existsBySiteIdAndDateDebutLessThanEqualAndDateFinGreaterThanEqual(
                siteId, date, date
        );
    }

    private Site getSiteOrThrow(Long siteId) {
        if (siteId == null) {
            throw new BusinessException("Id site obligatoire");
        }

        return siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));
    }

    private void validerRequestDate(CreateFermetureSiteDateRequest req) {
        if (req == null) {
            throw new BusinessException("Requête obligatoire");
        }
        if (req.getDate() == null) {
            throw new BusinessException("Date obligatoire");
        }
    }

    private void validerRequestDate(UpdateFermetureSiteDateRequest req) {
        if (req == null) {
            throw new BusinessException("Requête obligatoire");
        }
        if (req.getDate() == null) {
            throw new BusinessException("Date obligatoire");
        }
    }

    private void validerRequestPeriode(CreateFermetureSitePeriodeRequest req) {
        if (req == null) {
            throw new BusinessException("Requête obligatoire");
        }
        if (req.getDateDebut() == null || req.getDateFin() == null) {
            throw new BusinessException("La période doit contenir une date de début et une date de fin");
        }
        if (req.getDateFin().isBefore(req.getDateDebut())) {
            throw new BusinessException("La date de fin doit être après ou égale à la date de début");
        }
    }

    private void validerRequestPeriode(UpdateFermetureSitePeriodeRequest req) {
        if (req == null) {
            throw new BusinessException("Requête obligatoire");
        }
        if (req.getDateDebut() == null || req.getDateFin() == null) {
            throw new BusinessException("La période doit contenir une date de début et une date de fin");
        }
        if (req.getDateFin().isBefore(req.getDateDebut())) {
            throw new BusinessException("La date de fin doit être après ou égale à la date de début");
        }
    }
    private void verifierChevauchementPeriodePourUpdate(Long siteId,
                                                        Long fermetureId,
                                                        LocalDate newStart,
                                                        LocalDate newEnd) {
        List<FermetureSite> fermetures = fermetureSiteRepository.findBySiteIdOrderByDateAscDateDebutAsc(siteId);

        for (FermetureSite f : fermetures) {
            if (f.getId().equals(fermetureId)) {
                continue;
            }

            if (f.getDate() != null) {
                if (!f.getDate().isBefore(newStart) && !f.getDate().isAfter(newEnd)) {
                    throw new BusinessException("Cette période contient déjà une date de fermeture");
                }
            }

            if (f.getDateDebut() != null && f.getDateFin() != null) {
                boolean overlap = !f.getDateDebut().isAfter(newEnd) && !f.getDateFin().isBefore(newStart);
                if (overlap) {
                    throw new BusinessException("Cette période chevauche une autre fermeture");
                }
            }
        }
    }
}