package br.com.notehub.application.implementation.follow;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.follow.Follow;
import br.com.notehub.domain.follow.FollowRepository;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static br.com.notehub.infra.exception.CustomExceptions.*;

@Component
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserRepository userRepository;
    private final FollowRepository repository;
    private final Counter counter;
    private final NotificationService notifier;
    private final FeedService feeder;

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
    }

    private boolean isSameUser(UUID followerId, UUID followingId) {
        return Objects.equals(followerId, followingId);
    }

    private boolean areMutuals(UUID requesting, UUID requested) {
        boolean requestedFollowsRequesting = repository.existsByFollowerIdAndFollowingId(requested, requesting);
        boolean requestingFollowsRequested = repository.existsByFollowerIdAndFollowingId(requesting, requested);
        return requestedFollowsRequesting && requestingFollowsRequested;
    }

    @Override
    public void validateBidirectionalFollowAccess(@Nullable User requesting, User requested) {
        if (!requested.isProfilePrivate()) return;
        if (requesting == null) throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        if (!isSameUser(requesting.getId(), requested.getId()) && !areMutuals(requesting.getId(), requested.getId())) {
            throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        }
    }

    @Transactional
    @Override
    public void follow(UUID followerId, String username) {
        User follower = findUser(followerId);
        User following = findUser(username);
        if (Objects.equals(follower.getId(), following.getId())) throw new SelfFollowException();
        if (repository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) throw new AlreadyFollowingException();
        repository.save(new Follow(follower, following));
        counter.updateFollowersAndFollowingCount(follower, following, true);
        notifier.notify(follower, following, follower, MessageNotification.of(follower));
        feeder.onUserFollowed(follower.getId(), following.getId());
    }

    @Transactional
    @Override
    public void unfollow(UUID followerId, String username) {
        User follower = findUser(followerId);
        User following = findUser(username);
        if (!repository.existsByFollowerIdAndFollowingId(follower.getId(), following.getId())) throw new NotFollowingException();
        repository.deleteByFollowerIdAndFollowingId(follower.getId(), following.getId());
        counter.updateFollowersAndFollowingCount(follower, following, false);
        feeder.onUserUnfollowed(follower.getId(), following.getId());
    }

    @Override
    public Page<User> getUserFollowing(Pageable pageable, String q, UUID requestingId, String username) {
        User requesting = (requestingId != null) ? findUser(requestingId) : null;
        User requested = findUser(username);
        validateBidirectionalFollowAccess(requesting, requested);
        List<UUID> ids = repository.findByFollowerId(requested.getId(), Pageable.unpaged())
                .map(f -> f.getFollowing().getId())
                .toList();
        return userRepository.findAllByIdIn(pageable, q, ids);
    }

    @Override
    public Page<User> getUserFollowers(Pageable pageable, String q, UUID requestingId, String username) {
        User requesting = (requestingId != null) ? findUser(requestingId) : null;
        User requested = findUser(username);
        validateBidirectionalFollowAccess(requesting, requested);
        List<UUID> ids = repository.findByFollowingId(requested.getId(), Pageable.unpaged())
                .map(f -> f.getFollower().getId())
                .toList();
        return userRepository.findAllByIdIn(pageable, q, ids);
    }

    @Override
    public Set<String> getUserMutualConnections(UUID id) {
        Set<UUID> following = new HashSet<>(repository.findByFollowerId(id, Pageable.unpaged())
                .map(f -> f.getFollowing().getId())
                .toSet());
        Set<UUID> followers = new HashSet<>(repository.findByFollowingId(id, Pageable.unpaged())
                .map(f -> f.getFollower().getId())
                .toSet());
        following.retainAll(followers);
        return userRepository.findAllById(following).stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> getUserFollowersId(UUID id) {
        return repository.findFollowerIdsByFollowingId(id);
    }

    @Override
    public Set<UUID> getUserFollowingId(UUID id) {
        return repository.findFollowingIdsByFollowerId(id);
    }

}