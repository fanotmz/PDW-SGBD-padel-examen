package be.ephec.padel.backend.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MatriculeGeneratorService {

    private final JoueurRepository joueurRepository;

    public MatriculeGeneratorService(JoueurRepository joueurRepository) {
        this.joueurRepository = joueurRepository;
    }

    public String generateFor(TypeJoueur type) {
        if (type == null) {
            throw new BusinessException("Type joueur obligatoire");
        }

        String prefix = switch (type) {
            case GLOBAL -> "G";
            case SITE -> "S";
            case LIBRE -> "L";
        };

        int nextNumber = joueurRepository.findFirstByMatriculeStartingWithOrderByMatriculeDesc(prefix)
                .map(Joueur::getMatricule)
                .map(this::extractSequence)
                .map(last -> last + 1)
                .orElse(1);

        if (nextNumber > 9999) {
            throw new BusinessException("Plus aucun matricule disponible pour le type " + type);
        }

        return prefix + String.format("%04d", nextNumber);
    }

    private int extractSequence(String matricule) {
        if (matricule == null || matricule.length() != 5) {
            throw new BusinessException("Matricule existant invalide: " + matricule);
        }

        try {
            return Integer.parseInt(matricule.substring(1));
        } catch (NumberFormatException ex) {
            throw new BusinessException("Matricule existant invalide: " + matricule);
        }
    }
}
