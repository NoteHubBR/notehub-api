package br.com.notehub.application.implementation.feed;

import br.com.notehub.application.dto.response.feed.FeedEventRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.comment.CommentRepository;
import br.com.notehub.domain.feed.*;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.flame.FlameRepository;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.note.NoteRepository;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedRepository repository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final FlameRepository flameRepository;
    private final CommentRepository commentRepository;

    private boolean canSeeProfile(User requesting, User requested) {
        if (!requested.isProfilePrivate()) return true;
        boolean requestedContainsRequesting = requested.getFollowing().contains(requesting);
        boolean requestingContainsRequested = requesting.getFollowing().contains(requested);
        return requestingContainsRequested && requestedContainsRequesting;
    }

    private boolean canSeeNote(Note note, Flame flame, Comment comment) {
        if (note != null) return !note.isHidden();
        if (flame != null) return !flame.getNote().isHidden();
        if (comment != null) return !comment.getNote().isHidden();
        return true;
    }

    private void fanOut(FeedEvent event, User actor, User related, Note note, Flame flame, Comment comment) {
        Set<User> followers = actor.getFollowers();
        List<Feed> events = followers.stream()
                .filter(f -> canSeeProfile(actor, f))
                .filter(f -> canSeeNote(note, flame, comment))
                .map(f -> new Feed(event, f, actor, related, note, flame, comment))
                .toList();
        repository.saveAll(events);
    }

    @Async
    @Transactional
    @Override
    public void onProfilePrivacyChanged(UUID actorId, boolean isPrivateProfile) {
        User user = userRepository.findById(actorId).orElseThrow(EntityNotFoundException::new);
        if (!isPrivateProfile) return;
        repository.deleteActorEventsForNonMutualFollowers(user.getId());
    }

    @Async
    @Transactional
    @Override
    public void onUserFollowed(UUID followerId, UUID followingId) {
        User follower = userRepository.findById(followerId).orElseThrow(EntityNotFoundException::new);
        User following = userRepository.findById(followingId).orElseThrow(EntityNotFoundException::new);
        fanOut(FeedEvent.USER_FOLLOWED, follower, following, null, null, null);
    }

    @Async
    @Transactional
    @Override
    public void onUserUnfollowed(UUID unfollowingId, UUID followingId) {
        User exfollower = userRepository.findById(unfollowingId).orElseThrow(EntityNotFoundException::new);
        User exfollowing = userRepository.findById(followingId).orElseThrow(EntityNotFoundException::new);
        repository.deleteActorFollowEvent(unfollowingId, followingId);
        repository.deleteAllExRecipientEventsOnUnfollowEventByActor(followingId, unfollowingId);
        if (!canSeeProfile(exfollowing, exfollower)) {
            repository.deleteAllExRecipientEventsOnUnfollowEventByActor(unfollowingId, followingId);
        }
    }

    @Async
    @Transactional
    @Override
    public void onNoteCreated(UUID noteId) {
        Note note = noteRepository.findById(noteId).orElseThrow(EntityNotFoundException::new);
        if (note.isHidden()) return;
        fanOut(FeedEvent.NOTE_CREATED, note.getUser(), null, note, null, null);
    }

    @Async
    @Transactional
    @Override
    public void onNoteHidden(UUID noteId) {
        Note note = noteRepository.findById(noteId).orElseThrow(EntityNotFoundException::new);
        if (note.isHidden()) repository.deleteAllByNoteId(noteId);
    }

    @Async
    @Transactional
    @Override
    public void onNoteFlamed(UUID flameId) {
        Flame flame = flameRepository.findById(flameId).orElseThrow(EntityNotFoundException::new);
        fanOut(FeedEvent.NOTE_FLAMED, flame.getUser(), null, null, flame, null);
    }

    @Async
    @Transactional
    @Override
    public void onNoteCommented(UUID commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(EntityNotFoundException::new);
        fanOut(FeedEvent.NOTE_COMMENTED, comment.getUser(), null, null, null, comment);
    }

    @Override
    public PageRES<FeedEventRES> getFeed(Pageable pageable, UUID recipientId, List<FeedEvent> events) {
        Specification<Feed> spec = Specification
                .where(FeedSpec.forRecipient(recipientId))
                .and(FeedSpec.withEvents(events));
        Page<FeedEventRES> page = repository.findAll(spec, pageable).map(FeedEventRES::new);
        return new PageRES<>(page);
    }

}