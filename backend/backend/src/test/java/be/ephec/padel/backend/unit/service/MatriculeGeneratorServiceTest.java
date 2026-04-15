package be.ephec.padel.backend.unit.service;

import be.ephec.padel.backend.exception.BusinessException;
import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.repository.JoueurRepository;
import be.ephec.padel.backend.service.MatriculeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatriculeGeneratorServiceTest {

    @Mock
    JoueurRepository joueurRepository;

    @InjectMocks
    MatriculeGeneratorService matriculeGeneratorService;

    @Test
    void generateFor_commence_a_0001_si_aucun_joueur_du_type() {
        when(joueurRepository.findFirstByMatriculeStartingWithOrderByMatriculeDesc("G"))
                .thenReturn(Optional.empty());

        String matricule = matriculeGeneratorService.generateFor(TypeJoueur.GLOBAL);

        assertThat(matricule).isEqualTo("G0001");
    }

    @Test
    void generateFor_prend_le_suivant_du_prefixe() {
        when(joueurRepository.findFirstByMatriculeStartingWithOrderByMatriculeDesc("S"))
                .thenReturn(Optional.of(new Joueur("S0042", "Nom", TypeJoueur.SITE)));

        String matricule = matriculeGeneratorService.generateFor(TypeJoueur.SITE);

        assertThat(matricule).isEqualTo("S0043");
    }

    @Test
    void generateFor_refuse_si_compteur_depasse_9999() {
        when(joueurRepository.findFirstByMatriculeStartingWithOrderByMatriculeDesc("L"))
                .thenReturn(Optional.of(new Joueur("L9999", "Nom", TypeJoueur.LIBRE)));

        assertThatThrownBy(() -> matriculeGeneratorService.generateFor(TypeJoueur.LIBRE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plus aucun matricule disponible");
    }
}
