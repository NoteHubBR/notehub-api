package br.com.notehub.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findAllByActiveTrue();

    @Query("""
            SELECT u
            FROM User u
            WHERE u.active = true
            AND (
                :q IS NUll
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
            )
            """)
    Page<User> findAllActiveUsersByUsernameOrDisplayName(Pageable pageable, @Param("q") String q);

    @Query("SELECT u FROM User u WHERE u.createdAt < :nowMinus7Days AND u.active = false")
    List<User> findUsersWithExpiredActivationTime(@Param("nowMinus7Days") Instant nowMinus7Days);

    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.id IN :ids
              AND (
                  :q IS NULL
                  OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                  OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
              )
            """)
    Page<User> findAllByIdIn(Pageable pageable, @Param("q") String q, @Param("ids") List<UUID> ids);

}