package be.ephec.padel.backend.service;

import be.ephec.padel.backend.dto.request.CreateFermetureGlobaleRequest;
import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.exception.NotFoundException;
import be.ephec.padel.backend.model.entities.FermetureGlobale;
import be.ephec.padel.backend.repository.FermetureGlobaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FermetureGlobaleService {

    private final FermetureGlobaleRepository repo;

    public FermetureGlobaleService(FermetureGlobaleRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<FermetureGlobale> lister() {
        return repo.findAll();
    }

    public FermetureGlobale creer(CreateFermetureGlobaleRequest req) {
        LocalDate date = req.getDate(); // @NotNull déjà, mais on reste safe
        if (date == null) throw new BusinessException("Date de fermeture obligatoire");

        if (repo.existsByDate(date)) {
            throw new BusinessException("Une fermeture globale existe déjà pour cette date");
        }

        return repo.save(new FermetureGlobale(date, req.getMotif()));
    }

    public void supprimer(Long id) {
        FermetureGlobale f = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Fermeture globale introuvable"));

        repo.delete(f);
    }
}