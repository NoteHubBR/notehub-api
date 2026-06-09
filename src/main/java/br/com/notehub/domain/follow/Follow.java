package br.com.notehub.domain.follow;

import br.com.notehub.domain.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "follows")
@Data
@NoArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("followerId")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @MapsId("followingId")
    private User following;

    private Instant createdAt = Instant.now();

    public Follow(User follower, User following) {
        this.id = new FollowId(follower.getId(), following.getId());
        this.follower = follower;
        this.following = following;
    }

}