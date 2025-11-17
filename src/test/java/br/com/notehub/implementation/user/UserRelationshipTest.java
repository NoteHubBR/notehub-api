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
    private UUID followerId;

    @BeforeEach
    void setup() {
        follower = new User("follower@notehub.com.br", "follower", "Follower", "1234");
        following = new User("following@notehub.com.br", "following", "Following", "1234");
        followerId = UUID.randomUUID();
        follower.setId(followerId);
    }

    @Test
    void shouldFollowUserAndSendNotification_whenNotAlreadyFollowing() {

        when(repository.findByIdWithFollowersAndFollowing(followerId)).thenReturn(Optional.of(follower));
        when(repository.findByUsernameWithFollowersAndFollowing(following.getUsername())).thenReturn(Optional.of(following));

        service.follow(followerId, following.getUsername());

        verify(repository, times(1)).findByIdWithFollowersAndFollowing(followerId);
        verify(repository, times(1)).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, times(1)).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(true));
        verify(notifier, times(1)).notify(eq(follower), eq(following), eq(follower), eq(MessageNotification.of(follower)));

    }

    @Test
    void shouldUnfollowUserWithoutNotification_whenAlreadyFollowing() {

        when(repository.findByIdWithFollowersAndFollowing(followerId)).thenReturn(Optional.of(follower));
        when(repository.findByUsernameWithFollowersAndFollowing(following.getUsername())).thenReturn(Optional.of(following));

        follower.getFollowing().add(following);
        following.getFollowers().add(follower);
        service.unfollow(followerId, following.getUsername());

        verify(repository, times(1)).findByIdWithFollowersAndFollowing(followerId);
        verify(repository, times(1)).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, times(1)).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(false));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowSelfFollowException_whenTryingToFollowSelf() {

        when(repository.findByIdWithFollowersAndFollowing(followerId)).thenReturn(Optional.of(follower));
        when(repository.findByUsernameWithFollowersAndFollowing(following.getUsername())).thenReturn(Optional.of(follower));

        assertThatThrownBy(() ->
                service.follow(followerId, following.getUsername()))
                .isInstanceOf(CustomExceptions.SelfFollowException.class);

        verify(repository, times(1)).findByIdWithFollowersAndFollowing(followerId);
        verify(repository, times(1)).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowAlreadyFollowingException_whenFollowingUserAgain() {

        when(repository.findByIdWithFollowersAndFollowing(followerId)).thenReturn(Optional.of(follower));
        when(repository.findByUsernameWithFollowersAndFollowing(following.getUsername())).thenReturn(Optional.of(following));

        follower.getFollowing().add(following);
        following.getFollowers().add(follower);

        assertThatThrownBy(() ->
                service.follow(followerId, following.getUsername()))
                .isInstanceOf(CustomExceptions.AlreadyFollowingException.class);

        verify(repository, times(1)).findByIdWithFollowersAndFollowing(followerId);
        verify(repository, times(1)).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

    @Test
    void shouldThrowNotFollowingException_whenUnfollowingUserNotFollowed() {

        when(repository.findByIdWithFollowersAndFollowing(followerId)).thenReturn(Optional.of(follower));
        when(repository.findByUsernameWithFollowersAndFollowing(following.getUsername())).thenReturn(Optional.of(following));

        assertThatThrownBy(() ->
                service.unfollow(followerId, following.getUsername()))
                .isInstanceOf(CustomExceptions.NotFollowingException.class);

        verify(repository, times(1)).findByIdWithFollowersAndFollowing(followerId);
        verify(repository, times(1)).findByUsernameWithFollowersAndFollowing(following.getUsername());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());

    }

}