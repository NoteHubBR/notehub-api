package br.com.notehub.domain.token;

import br.com.notehub.domain.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@NoArgsConstructor
@Data
@JsonIgnoreProperties({"user"})
@ToString(exclude = {"user"})
@EqualsAndHashCode(exclude = {"user"})
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Instant createdAt = LocalDateTime.now().toInstant(ZoneOffset.of("-03:00"));

    private String ip;

    private UUID device;

    private String agent;

    private Instant expiresAt;

    public Token(User user, String ip, String agent, UUID device, Instant expiresAt) {
        this.user = user;
        this.ip = ip;
        this.agent = agent;
        this.device = device;
        this.expiresAt = expiresAt;
    }

}