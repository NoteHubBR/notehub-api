package br.com.notehub.application.dto.response.comment;

import br.com.notehub.application.dto.response.note.LowDetailNoteRES;
import br.com.notehub.application.dto.response.user.DetailUserRES;
import br.com.notehub.domain.comment.Comment;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record DetailCommentRES(
        UUID id,
        DetailUserRES user,
        LowDetailNoteRES note,
        String created_at,
        String text,
        boolean modified,
        int replies_count
) {
    public DetailCommentRES(Comment comment) {
        this(
                comment.getId(),
                comment.getUser() != null ? new DetailUserRES(comment.getUser()) : null,
                new LowDetailNoteRES(comment.getNote()),
                comment.getCreatedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.of("pt-BR"))),
                comment.getText(),
                comment.isModified(),
                comment.getRepliesCount()
        );
    }
}