package br.com.notehub.domain.feed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID> {

    @Query("""
            SELECT f FROM Feed f
            LEFT JOIN FETCH f.actor a
            LEFT JOIN FETCH f.note n
            LEFT JOIN FETCH f.comment c
            LEFT JOIN FETCH f.related r
            WHERE f.recipient.id = :recipientId
            ORDER BY f.createdAt DESC
            """)
    Page<Feed> findFeedForUser(Pageable pageable, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query("""
            DELETE FROM Feed f
            WHERE f.actor.id = :actorId
            AND f.recipient.id NOT IN (
                SELECT f1.id FROM User actor
                JOIN actor.followers f1
                JOIN actor.following f2
                WHERE actor.id = :actorId
                AND f1.id = f2.id
            )
            """)
    void deleteActorEventsForNonMutualFollowers(@Param("actorId") UUID actorId);

    @Modifying
    @Query("""
            DELETE FROM Feed f
            WHERE f.actor.id = :followerId
            AND f.related.id = :followingId
            AND f.event = 'User_Followed'
            """)
    void deleteActorFollowEvent(@Param("followerId") UUID followerId, @Param("followingId") UUID followingId);

    @Modifying
    @Query("""
            DELETE FROM Feed f
            WHERE f.actor.id = :actorId
            AND f.recipient.id = :recipientId
            """)
    void deleteAllExRecipientEventsOnUnfollowEventByActor(@Param("actorId") UUID actorId, @Param("recipientId") UUID recipientId);

    void deleteAllByNoteId(UUID noteId);

}