package br.com.notehub.domain.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID>, JpaSpecificationExecutor<Feed> {

    @Modifying
    @Query("""
            DELETE FROM Feed f
            WHERE f.actor.id = :actorId
              AND f.recipient.id NOT IN (
                  SELECT fl1.follower.id
                  FROM Follow fl1
                  WHERE fl1.following.id = :actorId
                    AND EXISTS (
                        SELECT 1
                        FROM Follow fl2
                        WHERE fl2.follower.id = :actorId
                          AND fl2.following.id = fl1.follower.id
                    )
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