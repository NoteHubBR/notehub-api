package br.com.notehub.domain.feed;

import br.com.notehub.application.dto.response.feed.FeedEventRES;
import br.com.notehub.application.dto.response.page.PageRES;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface FeedService {

    void onProfilePrivacyChanged(UUID actorId, boolean isPrivateProfile);

    void onUserFollowed(UUID followerId, UUID followingId);

    void onUserUnfollowed(UUID unfollowingId, UUID followingId);

    void onNoteCreated(UUID noteId);

    void onNoteHidden(UUID noteId);

    void onNoteFlamed(UUID flameId);

    void onNoteCommented(UUID commentId);

    PageRES<FeedEventRES> getFeed(Pageable pageable, UUID recipientId, List<FeedEvent> events);

}