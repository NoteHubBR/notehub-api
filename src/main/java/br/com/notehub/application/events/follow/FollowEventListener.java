package br.com.notehub.application.events.follow;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.domain.follow.events.UserDeletedEvent;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowEventListener {

    private final UserRepository userRepository;
    private final Counter counter;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDelete(UserDeletedEvent event) {
        for (UUID id : event.followerIds()) {
            User follower = userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
            counter.decrementFollowingCount(follower);
        }
        for (UUID id : event.followingIds()) {
            User following = userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
            counter.decrementFollowerCount(following);
        }
    }

}