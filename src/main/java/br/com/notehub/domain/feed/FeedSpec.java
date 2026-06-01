package br.com.notehub.domain.feed;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class FeedSpec {

    public static Specification<Feed> forRecipient(UUID recipientId) {
        return (root, query, cb) -> cb.equal(root.get("recipient").get("id"), recipientId);
    }

    public static Specification<Feed> withEvents(List<FeedEvent> events) {
        return (root, query, cb) -> events == null || events.isEmpty()
                ? cb.conjunction()
                : root.get("event").in(events);
    }

}