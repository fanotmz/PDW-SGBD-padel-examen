package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.security.ServiceAutorisationAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final JoueurRepository joueurRepository;
    private final ServiceAutorisationAdmin serviceAutorisationAdmin;

    public AdminSiteService(
            SiteRepository siteRepository,
            JoueurRepository joueurRepository,
            ServiceAutorisationAdmin serviceAutorisationAdmin
    ) {
        this.siteRepository = siteRepository;
        this.joueurRepository = joueurRepository;
        this.serviceAutorisationAdmin = serviceAutorisationAdmin;
    }

    public List<JoueurAdminDto> getJoueursBySite(Long siteId) {
        serviceAutorisationAdmin.verifierAccesAuSite(siteId);

        if (!siteRepository.existsById(siteId)) {
            throw new NotFoundException("Site introuvable: " + siteId);
        }

        List<Joueur> joueurs = joueurRepository.findBySite_Id(siteId);

        return joueurs.stream()
                .map(this::toJoueurAdminDto)
                .toList();
    }

    private JoueurAdminDto toJoueurAdminDto(Joueur joueur) {
        return new JoueurAdminDto(
                joueur.getMatricule(),
                joueur.getNom(),
                joueur.getType(),
                joueur.getSolde()
        );
    }
}