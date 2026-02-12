package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByParticipation_Id(Long participationId);

    List<Paiement> findByParticipation_Joueur_Matricule(String matricule);

    List<Paiement> findByParticipation_Match_Id(Long matchId);
}
