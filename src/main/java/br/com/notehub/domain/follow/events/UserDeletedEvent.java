package br.com.notehub.domain.follow.events;

import java.util.Set;
import java.util.UUID;

public record UserDeletedEvent(
        Set<UUID> followerIds,
        Set<UUID> followingIds
) {
}