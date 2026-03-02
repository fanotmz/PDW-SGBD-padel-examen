package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.response.JoueurAdminDto;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final JoueurRepository joueurRepository;

    public AdminSiteService(SiteRepository siteRepository, JoueurRepository joueurRepository) {
        this.siteRepository = siteRepository;
        this.joueurRepository = joueurRepository;
    }

    public List<JoueurAdminDto> getJoueursBySite(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new NotFoundException("Site introuvable: " + siteId);
        }

        List<Joueur> joueurs = joueurRepository.findBySite_Id(siteId);

        return joueurs.stream()
                .map(j -> new JoueurAdminDto(
                        j.getMatricule(),
                        j.getNom(),
                        j.getType(),
                        j.getSolde()
                ))
                .toList();
    }
}