package br.com.notehub.implementation.user;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.application.implementation.user.UserServiceImpl;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import br.com.notehub.infra.exception.CustomExceptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRelationshipTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repository;

    @Mock
    private NotificationService notifier;

    @Mock
    private Counter counter;

    private User follower;
    private User following;

    private User createUser(String email, String username) {
        User u = new User(email, username, username.toUpperCase(), "123");
        u.setId(UUID.randomUUID());
        u.setActive(true);
        return u;
    }

    private void mockfindByIdWithFollowersAndFollowing(User u) {
        when(repository.findByIdWithFollowersAndFollowing(u.getId())).thenReturn(Optional.of(u));
    }

    private void mockfindByUsernameWithFollowersAndFollowing(User u) {
        when(repository.findByUsernameWithFollowersAndFollowing(u.getUsername())).thenReturn(Optional.of(u));
    }

    @BeforeEach
    void setup() {
        follower = createUser("follower@notehub.com.br", "follower");
        following = createUser("following@notehub.com.br", "following");
    }

    @Test
    void shouldFollowUserAndSendNotification_whenNotAlreadyFollowing() {

        mockfindByIdWithFollowersAndFollowing(follower);
        mockfindByUsernameWithFollowersAndFollowing(following);

        service.follow(follower.getId(), following.getUsername());

        verify(repository).findByIdWithFollowersAndFollowing(follower.getId());
        verify(repository).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(true));
        verify(notifier).notify(eq(follower), eq(following), eq(follower), eq(MessageNotification.of(follower)));

    }

    @Test
    void shouldUnfollowUserWithoutNotification_whenAlreadyFollowing() {

        mockfindByIdWithFollowersAndFollowing(follower);
        mockfindByUsernameWithFollowersAndFollowing(following);

        follower.getFollowing().add(following);
        following.getFollowers().add(follower);
        service.unfollow(follower.getId(), following.getUsername());

        verify(repository).findByIdWithFollowersAndFollowing(follower.getId());
        verify(repository).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(false));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowSelfFollowException_whenTryingToFollowSelf() {

        mockfindByIdWithFollowersAndFollowing(follower);
        mockfindByUsernameWithFollowersAndFollowing(follower);

        assertThatThrownBy(() ->
                service.follow(follower.getId(), follower.getUsername()))
                .isInstanceOf(CustomExceptions.SelfFollowException.class);

        verify(repository).findByIdWithFollowersAndFollowing(follower.getId());
        verify(repository).findByUsernameWithFollowersAndFollowing(follower.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowAlreadyFollowingException_whenFollowingUserAgain() {

        mockfindByIdWithFollowersAndFollowing(follower);
        mockfindByUsernameWithFollowersAndFollowing(following);

        follower.getFollowing().add(following);
        following.getFollowers().add(follower);

        assertThatThrownBy(() ->
                service.follow(follower.getId(), following.getUsername()))
                .isInstanceOf(CustomExceptions.AlreadyFollowingException.class);

        verify(repository).findByIdWithFollowersAndFollowing(follower.getId());
        verify(repository).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowNotFollowingException_whenUnfollowingUserNotFollowed() {

        mockfindByIdWithFollowersAndFollowing(follower);
        mockfindByUsernameWithFollowersAndFollowing(following);

        assertThatThrownBy(() ->
                service.unfollow(follower.getId(), following.getUsername()))
                .isInstanceOf(CustomExceptions.NotFollowingException.class);

        verify(repository).findByIdWithFollowersAndFollowing(follower.getId());
        verify(repository).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

}