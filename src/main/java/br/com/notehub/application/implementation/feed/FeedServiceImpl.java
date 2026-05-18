package br.com.notehub.application.implementation.feed;

import br.com.notehub.application.dto.response.feed.FeedEventRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.feed.Feed;
import br.com.notehub.domain.feed.FeedEvent;
import br.com.notehub.domain.feed.FeedRepository;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedRepository repository;

    private boolean canSeeProfile(User requesting, User requested) {
        if (!requested.isProfilePrivate()) return true;
        boolean requestedContainsRequesting = requested.getFollowing().contains(requesting);
        boolean requestingContainsRequested = requesting.getFollowing().contains(requested);
        return requestingContainsRequested && requestedContainsRequesting;
    }

    private void fanOut(FeedEvent event, User actor, User related, Note note, Flame flame, Comment comment) {
        Set<User> followers = actor.getFollowers();
        List<Feed> events = followers.stream()
                .filter(f -> canSeeProfile(actor, f))
                .map(f -> new Feed(event, f, actor, related, note, flame, comment))
                .toList();
        repository.saveAll(events);
    }

    @Async
    @Override
    public void onUserFollowed(User follower, User following) {
        fanOut(FeedEvent.USER_FOLLOWED, follower, following, null, null, null);
    }

    @Async
    @Override
    public void onNoteCreated(Note note) {
        if (note.isHidden()) return;
        fanOut(FeedEvent.NOTE_CREATED, note.getUser(), null, note, null, null);
    }

    @Async
    @Override
    public void onNoteFlamed(Flame flame) {
        fanOut(FeedEvent.NOTE_FLAMED, flame.getUser(), null, null, flame, null);
    }

    @Async
    @Override
    public void onNoteCommented(Comment comment) {
        fanOut(FeedEvent.NOTE_COMMENTED, comment.getUser(), null, null, null, comment);
    }

    @Override
    public PageRES<FeedEventRES> getFeed(Pageable pageable, UUID recipientId) {
        Page<FeedEventRES> page = repository
                .findFeedForUser(pageable, recipientId)
                .map(FeedEventRES::new);
        return new PageRES<>(page);
    }

}