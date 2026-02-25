package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Joueur;
import be.ephec.padel.backend.model.entities.MouvementSolde;
import be.ephec.padel.backend.model.enums.TypeJoueur;
import be.ephec.padel.backend.model.enums.TypeMouvement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MouvementSoldeRepositoryTest extends SqlServerTestContainerConfig {

    @Autowired JoueurRepository joueurRepository;
    @Autowired MouvementSoldeRepository mouvementSoldeRepository;

    @Test
    void findByJoueur_MatriculeOrderByDateMouvementDesc_trie_descendant() {
        Joueur j = joueurRepository.save(new Joueur("J001", "Alice", TypeJoueur.GLOBAL));

        MouvementSolde m1 = mouvementSoldeRepository.save(new MouvementSolde(
                LocalDateTime.of(2026, 2, 1, 10, 0),
                new BigDecimal("15.00"),
                TypeMouvement.DEBIT,
                j
        ));

        MouvementSolde m2 = mouvementSoldeRepository.save(new MouvementSolde(
                LocalDateTime.of(2026, 2, 2, 10, 0),
                new BigDecimal("7.50"),
                TypeMouvement.CREDIT,
                j
        ));

        MouvementSolde m3 = mouvementSoldeRepository.save(new MouvementSolde(
                LocalDateTime.of(2026, 1, 31, 10, 0),
                new BigDecimal("5.00"),
                TypeMouvement.DEBIT,
                j
        ));

        List<MouvementSolde> res =
                mouvementSoldeRepository.findByJoueur_MatriculeOrderByDateMouvementDesc("J001");

        assertThat(res).hasSize(3);
        assertThat(res.get(0).getId()).isEqualTo(m2.getId()); // 2026-02-02
        assertThat(res.get(1).getId()).isEqualTo(m1.getId()); // 2026-02-01
        assertThat(res.get(2).getId()).isEqualTo(m3.getId()); // 2026-01-31
    }
}