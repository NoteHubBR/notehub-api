package br.com.notehub.domain.feed;

import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed")
@NoArgsConstructor
@Data
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Instant createdAt = Instant.now();

    @Convert(converter = FeedEventConverter.class)
    private FeedEvent event;

    @JoinColumn(name = "recipient_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User recipient;

    @JoinColumn(name = "actor_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User actor;

    @JoinColumn(name = "related_user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User related;

    @JoinColumn(name = "note_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Note note;

    @JoinColumn(name = "flame_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Flame flame;

    @JoinColumn(name = "comment_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Comment comment;

    public Feed(FeedEvent event, User recipient, User actor, User related, Note note, Flame flame, Comment comment) {
        this.event = event;
        this.recipient = recipient;
        this.actor = actor;
        this.related = related;
        this.note = note;
        this.flame = flame;
        this.comment = comment;
    }

}