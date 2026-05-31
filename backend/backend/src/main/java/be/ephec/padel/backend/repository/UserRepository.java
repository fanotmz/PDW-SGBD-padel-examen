package be.ephec.padel.backend.repository;

import be.ephec.padel.backend.model.entities.User;
import be.ephec.padel.backend.model.enums.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    List<User> findByStatusOrderByIdAsc(UserStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select u
        from User u
        left join fetch u.requestedSite
        left join fetch u.joueur
        where u.id = :id
    """)
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
