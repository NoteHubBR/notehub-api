package br.com.notehub.implementation.user;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.application.implementation.follow.FollowServiceImpl;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.follow.Follow;
import br.com.notehub.domain.follow.FollowRepository;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import br.com.notehub.infra.exception.CustomExceptions;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserRelationshipTest {

    @InjectMocks
    private FollowServiceImpl service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private NotificationService notifier;

    @Mock
    private Counter counter;

    @Mock
    private FeedService feeder;

    private User follower;
    private User following;

    private User createUser(String email, String username) {
        User u = new User(email, username, username.toUpperCase(), "123");
        u.setId(UUID.randomUUID());
        u.setActive(true);
        return u;
    }

    @BeforeEach
    void setup() {
        follower = createUser("follower@notehub.com.br", "follower");
        following = createUser("following@notehub.com.br", "following");
    }

    @Test
    void shouldFollowUserAndSendNotification_whenNotAlreadyFollowing() {
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(following.getUsername())).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())).thenReturn(false);

        service.follow(follower.getId(), following.getUsername());

        verify(followRepository).save(any(Follow.class));
        verify(counter).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(true));
        verify(notifier).notify(eq(follower), eq(following), eq(follower), eq(MessageNotification.of(follower)));
    }

    @Test
    void shouldUnfollowUserWithoutNotification_whenAlreadyFollowing() {
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(following.getUsername())).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())).thenReturn(true);

        service.unfollow(follower.getId(), following.getUsername());

        verify(followRepository).deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
        verify(counter).updateFollowersAndFollowingCount(eq(follower), eq(following), eq(false));
        verify(notifier, never()).notify(any(), any(), any(), any());
    }

    @Test
    void shouldThrowSelfFollowException_whenTryingToFollowSelf() {
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(follower.getUsername())).thenReturn(Optional.of(follower));

        assertThatThrownBy(() ->
                service.follow(follower.getId(), follower.getUsername()))
                .isInstanceOf(CustomExceptions.SelfFollowException.class);

        verify(followRepository, never()).save(any());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());
    }

    @Test
    void shouldThrowAlreadyFollowingException_whenFollowingUserAgain() {
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(following.getUsername())).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())).thenReturn(true);

        assertThatThrownBy(() ->
                service.follow(follower.getId(), following.getUsername()))
                .isInstanceOf(CustomExceptions.AlreadyFollowingException.class);

        verify(followRepository, never()).save(any());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());
    }

    @Test
    void shouldThrowNotFollowingException_whenUnfollowingUserNotFollowed() {
        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(following.getUsername())).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())).thenReturn(false);

        assertThatThrownBy(() ->
                service.unfollow(follower.getId(), following.getUsername()))
                .isInstanceOf(CustomExceptions.NotFollowingException.class);

        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(any(), any());
        verify(counter, never()).updateFollowersAndFollowingCount(any(), any(), any(Boolean.class));
        verify(notifier, never()).notify(any(), any(), any(), any());
    }

    @Test
    void shouldReturnPageOfUsersThatRequestedUserIsFollowing() {
        User requesting = createUser("req@mail.com", "req");
        User target = createUser("target@mail.com", "target");
        User u1 = createUser("a@mail.com", "a");
        User u2 = createUser("b@mail.com", "b");

        List<UUID> ids = List.of(u1.getId(), u2.getId());
        Page<User> expected = new PageImpl<>(List.of(u1, u2));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Follow> followPage = new PageImpl<>(List.of(
                new Follow(target, u1),
                new Follow(target, u2)
        ));

        when(userRepository.findById(requesting.getId())).thenReturn(Optional.of(requesting));
        when(userRepository.findByUsername(target.getUsername())).thenReturn(Optional.of(target));
        when(followRepository.findByFollowerId(eq(target.getId()), any())).thenReturn(followPage);
        when(userRepository.findAllByIdIn(eq(pageable), eq("q"), argThat(list -> new HashSet<>(list).containsAll(ids)))).thenReturn(expected);

        Page<User> result = service.getUserFollowing(pageable, "q", requesting.getId(), target.getUsername());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnPageOfUsersThatFollowsRequestedUser() {
        User requesting = createUser("req@mail.com", "req");
        User target = createUser("target@mail.com", "target");
        User u1 = createUser("a@mail.com", "a");
        User u2 = createUser("b@mail.com", "b");

        List<UUID> ids = List.of(u1.getId(), u2.getId());
        Page<User> expected = new PageImpl<>(List.of(u1, u2));
        Pageable pageable = PageRequest.of(0, 10);

        Page<Follow> followPage = new PageImpl<>(List.of(
                new Follow(u1, target),
                new Follow(u2, target)
        ));

        when(userRepository.findById(requesting.getId())).thenReturn(Optional.of(requesting));
        when(userRepository.findByUsername(target.getUsername())).thenReturn(Optional.of(target));
        when(followRepository.findByFollowingId(eq(target.getId()), any())).thenReturn(followPage);
        when(userRepository.findAllByIdIn(eq(pageable), eq("q"), argThat(list -> new HashSet<>(list).containsAll(ids)))).thenReturn(expected);

        Page<User> result = service.getUserFollowers(pageable, "q", requesting.getId(), target.getUsername());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequestingUserNotFound() {
        UUID id = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getUserFollowing(pageable, "q", id, "someone"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(userRepository).findById(id);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void shouldThrowEntityNotFoundException_whenRequestedUserNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(follower.getId())).thenReturn(Optional.of(follower));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getUserFollowing(pageable, "q", follower.getId(), "unknown"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(userRepository).findById(follower.getId());
        verify(userRepository).findByUsername("unknown");
    }

    @Test
    void shouldThrowAccessDeniedException_whenAccessIsNotAllowed() {
        User requesting = createUser("req@mail.com", "req");
        User target = createUser("tar@mail.com", "tar");
        target.setProfilePrivate(true);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(requesting.getId())).thenReturn(Optional.of(requesting));
        when(userRepository.findByUsername(target.getUsername())).thenReturn(Optional.of(target));
        when(followRepository.existsByFollowerIdAndFollowingId(target.getId(), requesting.getId())).thenReturn(false);

        assertThatThrownBy(() ->
                service.getUserFollowing(pageable, "q", requesting.getId(), target.getUsername()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldReturnStringSetOfAllUserMutuals() {
        User u1 = createUser("a@mail.com", "a");
        User u2 = createUser("b@mail.com", "b");

        Page<Follow> followingPage = new PageImpl<>(List.of(new Follow(follower, u1), new Follow(follower, u2)));
        Page<Follow> followersPage = new PageImpl<>(List.of(new Follow(u1, follower), new Follow(u2, follower)));

        when(followRepository.findByFollowerId(eq(follower.getId()), any())).thenReturn(followingPage);
        when(followRepository.findByFollowingId(eq(follower.getId()), any())).thenReturn(followersPage);
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(u1, u2));

        Set<String> mutuals = service.getUserMutualConnections(follower.getId());

        assertThat(mutuals).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void shouldReturnEmptySet_whenUserHasNoMutualConnections() {
        User u1 = createUser("a@mail.com", "a");
        User u2 = createUser("b@mail.com", "b");

        Page<Follow> followingPage = new PageImpl<>(List.of(new Follow(follower, u1)));
        Page<Follow> followersPage = new PageImpl<>(List.of(new Follow(u2, follower)));

        when(followRepository.findByFollowerId(eq(follower.getId()), any())).thenReturn(followingPage);
        when(followRepository.findByFollowingId(eq(follower.getId()), any())).thenReturn(followersPage);
        when(userRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList());

        Set<String> mutuals = service.getUserMutualConnections(follower.getId());

        assertThat(mutuals).isEmpty();
    }

}