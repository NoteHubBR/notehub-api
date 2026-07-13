package br.com.notehub.domain.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_identities", uniqueConstraints = @UniqueConstraint(columnNames = {"host", "provider_id"}))
@Data
@NoArgsConstructor
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = HostConverter.class)
    private Host host;

    private String providerId;

    private String providerEmail;

    private Instant linkedAt = Instant.now();

    public static UserIdentity signin(User user, Host host, String providerId, String providerEmail) {
        UserIdentity identity = new UserIdentity();
        identity.user = user;
        identity.host = host;
        identity.providerId = providerId;
        identity.providerEmail = providerEmail;
        return identity;
    }

}