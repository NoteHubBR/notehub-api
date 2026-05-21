package br.com.notehub.application.dto.response.feed;

import br.com.notehub.application.dto.response.comment.DetailCommentRES;
import br.com.notehub.application.dto.response.flame.DetailFlameRES;
import br.com.notehub.application.dto.response.note.LowDetailNoteRES;
import br.com.notehub.application.dto.response.user.DetailUserRES;
import br.com.notehub.domain.feed.Feed;
import br.com.notehub.domain.feed.FeedEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record FeedEventRES(
        FeedEvent event,
        DetailUserRES recipient,
        DetailUserRES actor,
        DetailUserRES related,
        LowDetailNoteRES note,
        DetailFlameRES flame,
        DetailCommentRES comment,
        String created_at
) {
    public FeedEventRES(Feed feed) {
        this(
                feed.getEvent(),
                new DetailUserRES(feed.getRecipient()),
                new DetailUserRES(feed.getActor()),
                feed.getRelated() == null ? null : new DetailUserRES(feed.getRelated()),
                feed.getNote() == null ? null : new LowDetailNoteRES(feed.getNote()),
                feed.getFlame() == null ? null : new DetailFlameRES(feed.getFlame()),
                feed.getComment() == null ? null : new DetailCommentRES(feed.getComment()),
                feed.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.of("pt-BR")))
        );
    }
}