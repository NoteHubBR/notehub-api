package br.com.notehub.domain.notification;

import br.com.notehub.domain.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"from", "to", "related"})
@ToString(exclude = {"from", "to", "related"})
@EqualsAndHashCode(exclude = {"from", "to", "related"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JoinColumn(name = "from_user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User from;

    @JoinColumn(name = "to_user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User to;

    @JoinColumn(name = "related_user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User related;

    private Instant createdAt = Instant.now();

    private boolean read = false;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
//    @Convert(converter = NotificationFieldInfoConverter.class)
    private Map<String, Object> info;

    public Notification(User from, User to, User related, Map<String, Object> info) {
        this.from = from;
        this.to = to;
        this.related = related;
        this.info = info;
    }

    public Notification(Notification snapshot) {
        this.id = snapshot.id;
        this.from = snapshot.from;
        this.to = snapshot.to;
        this.related = snapshot.related;
        this.createdAt = snapshot.createdAt;
        this.read = snapshot.read;
        this.info = snapshot.info;
    }

}