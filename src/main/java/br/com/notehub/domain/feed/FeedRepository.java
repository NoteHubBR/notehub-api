package br.com.notehub.domain.feed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    void deleteAllByNoteId(UUID noteId);

    void deleteAllByActorId(UUID actorId);

}