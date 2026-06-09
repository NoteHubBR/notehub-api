package br.com.notehub.domain.follow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :id")
    Set<UUID> findFollowerIdsByFollowingId(@Param("id") UUID followerId);

    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :id")
    Set<UUID> findFollowingIdsByFollowerId(@Param("id") UUID followingId);

    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);

    Page<Follow> findByFollowingId(UUID followingId, Pageable pageable);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

}