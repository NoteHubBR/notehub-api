package br.com.notehub.domain.follow;

import br.com.notehub.domain.user.User;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public interface FollowService {

    void validateBidirectionalFollowAccess(@Nullable User requesting, User requested);

    void follow(UUID followerId, String username);

    void unfollow(UUID followerId, String username);

    Page<User> getUserFollowing(Pageable pageable, String q, UUID requestingId, String username);

    Page<User> getUserFollowers(Pageable pageable, String q, UUID requestingId, String username);

    Set<String> getUserMutualConnections(UUID id);

    Set<UUID> getUserFollowersId(UUID id);

    Set<UUID> getUserFollowingId(UUID id);

}