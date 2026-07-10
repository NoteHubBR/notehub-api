package br.com.notehub.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    boolean existsByUser(User user);

    Optional<UserIdentity> findByHostAndProviderId(Host host, String providerId);

    List<UserIdentity> findAllByUserIdOrderByLinkedAtDesc(UUID id);

}