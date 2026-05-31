package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.Site;
import be.ephec.padel.backend.model.entities.Terrain;
import be.ephec.padel.backend.repository.SiteRepository;
import be.ephec.padel.backend.repository.TerrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TerrainService {

    private final TerrainRepository terrainRepository;
    private final SiteRepository siteRepository;

    public TerrainService(TerrainRepository terrainRepository, SiteRepository siteRepository) {
        this.terrainRepository = terrainRepository;
        this.siteRepository = siteRepository;
    }

    public List<Terrain> lister() {
        return terrainRepository.findAll();
    }

    public List<Terrain> listerParSite(Long siteId) {
        if (siteId == null) throw new BusinessException("SiteId obligatoire");
        return terrainRepository.findBySite_Id(siteId);
    }

    public Terrain getTerrain(Long id) {
        if (id == null) throw new BusinessException("Id terrain obligatoire");
        return terrainRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Terrain introuvable"));
    }

    public Terrain creerTerrain(String nom, Long siteId) {
        if (nom == null || nom.isBlank()) throw new BusinessException("Nom obligatoire");
        if (siteId == null) throw new BusinessException("SiteId obligatoire");

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new NotFoundException("Site introuvable"));

        if (terrainRepository.existsByNomAndSiteId(nom, siteId)) {
            throw new BusinessException("Terrain déjà existant pour ce site");
        }

        return terrainRepository.save(new Terrain(nom, site));
    }
}
