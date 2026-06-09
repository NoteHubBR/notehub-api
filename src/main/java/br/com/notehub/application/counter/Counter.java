package br.com.notehub.application.counter;

import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.comment.CommentRepository;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.note.NoteRepository;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Counter {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final CommentRepository commentRepository;

    public void updateNotesCount(User user, boolean increment) {
        int notesCount = user.getNotesCount();
        if (increment) {
            user.setNotesCount(notesCount + 1);
        } else {
            user.setNotesCount(notesCount - 1);
        }
        userRepository.save(user);
    }

    public void updateFollowersAndFollowingCount(User follower, User following, boolean increment) {
        if (increment) {
            follower.setFollowingCount(follower.getFollowingCount() + 1);
            following.setFollowersCount(following.getFollowersCount() + 1);
        } else {
            follower.setFollowingCount(follower.getFollowingCount() - 1);
            following.setFollowersCount(following.getFollowersCount() - 1);
        }
        userRepository.save(follower);
        userRepository.save(following);
    }

    public void decrementFollowerCount(User following) {
        following.setFollowersCount(following.getFollowersCount() - 1);
        userRepository.save(following);
    }

    public void decrementFollowingCount(User follower) {
        follower.setFollowingCount(follower.getFollowingCount() - 1);
        userRepository.save(follower);
    }

    public void updateCommentsCount(Note note, boolean increment) {
        int commentsCount = note.getCommentsCount();
        if (increment) {
            note.setCommentsCount(commentsCount + 1);
        } else {
            note.setCommentsCount(commentsCount - 1);
        }
        noteRepository.save(note);
    }

    public void updateFlamesCount(Note note, boolean increment) {
        int flamesCount = note.getFlamesCount();
        if (increment) {
            note.setFlamesCount(flamesCount + 1);
        } else {
            note.setFlamesCount(flamesCount - 1);
        }
        noteRepository.save(note);
    }

    public void updateRepliesCount(Comment comment, boolean increment) {
        int repliesCount = comment.getRepliesCount();
        if (increment) {
            comment.setRepliesCount(repliesCount + 1);
        } else {
            comment.setRepliesCount(repliesCount - 1);
        }
        commentRepository.save(comment);
    }

}