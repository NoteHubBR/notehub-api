package br.com.notehub.domain.feed;

import br.com.notehub.application.dto.response.feed.FeedEventRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface FeedService {

    void onUserFollowed(User follower, User following);

    void onNoteCreated(Note note);

    void onNoteCommented(Comment comment);

    void onNoteFlamed(Flame flame);

    PageRES<FeedEventRES> getFeed(Pageable pageable, UUID recipientId);

}