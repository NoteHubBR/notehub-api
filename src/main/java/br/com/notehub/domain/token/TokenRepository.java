package br.com.notehub.domain.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByDevice(UUID id);

    @Query("SELECT t FROM Token t WHERE t.expiresAt < :now")
    List<Token> findExpiredTokens(@Param("now") Instant now);

}