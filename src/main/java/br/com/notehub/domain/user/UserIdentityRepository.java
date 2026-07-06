package br.com.notehub.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    boolean existsByUser(User user);

    Optional<UserIdentity> findByHostAndProviderId(Host host, String providerId);

}