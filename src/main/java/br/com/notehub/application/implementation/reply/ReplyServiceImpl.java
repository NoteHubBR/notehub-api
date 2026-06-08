package br.com.notehub.application.implementation.reply;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.application.dto.request.reply.CreateReplyREQ;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.application.dto.response.reply.CreateReplyRES;
import br.com.notehub.application.dto.response.reply.DetailReplyRES;
import br.com.notehub.domain.comment.Comment;
import br.com.notehub.domain.comment.CommentRepository;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.reply.Reply;
import br.com.notehub.domain.reply.ReplyRepository;
import br.com.notehub.domain.reply.ReplyService;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReplyServiceImpl implements ReplyService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository repository;
    private final FollowService followService;
    private final NotificationService notifier;
    private final Counter counter;

    private void validateAccess(@Nullable UUID idFromToken, UUID idFromRequested) {
        if (idFromToken == null) throw new AccessDeniedException("Usuário sem permissão.");
        if (!Objects.equals(idFromToken, idFromRequested)) {
            throw new AccessDeniedException("Usuário sem permissão.");
        }
    }

    private void assertNoteIsNotClosed(Reply reply) {
        if (reply.getComment().getNote().isClosed()) throw new IllegalStateException("Nota fechada para novas interações.");
    }

    public Reply mapToReply(UUID idFromToken, UUID commentIdFromPath, CreateReplyREQ req) {
        User user = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        Comment comment = commentRepository.findById(commentIdFromPath).orElseThrow(EntityNotFoundException::new);
        return new Reply(user, comment, req.text());
    }

    public Reply mapToSelfReference(UUID idFromToken, UUID replyIdFromPath, CreateReplyREQ req) {
        User user = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        Reply reply = repository.findById(replyIdFromPath).orElseThrow(EntityNotFoundException::new);
        Comment comment = reply.getComment();
        return new Reply(user, comment, reply, req.text());
    }

    @Transactional
    @Override
    public CreateReplyRES create(UUID idFromToken, UUID idFromPath, boolean toReply, CreateReplyREQ req) {
        Reply reply = toReply ? mapToSelfReference(idFromToken, idFromPath, req) : mapToReply(idFromToken, idFromPath, req);
        assertNoteIsNotClosed(reply);
        repository.save(reply);
        counter.updateRepliesCount(reply.getComment(), true);
        if (reply.getToUser() == null) notifier.notify(
                reply.getUser(),
                reply.getComment().getUser(),
                reply.getComment().getNote().getUser(),
                MessageNotification.of(reply));
        if (reply.getToUser() != null) notifier.notify(
                reply.getUser(),
                reply.getToReply().getUser(),
                reply.getComment().getNote().getUser(),
                MessageNotification.of(reply));
        return new CreateReplyRES(reply);
    }

    @Transactional
    @Override
    public void edit(UUID idFromToken, UUID idFromPath, String text) {
        Reply reply = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        assertNoteIsNotClosed(reply);
        validateAccess(idFromToken, reply.getUser().getId());
        reply.setText(text);
        reply.setModifiedAt(Instant.now());
        reply.setModified(true);
        repository.save(reply);
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, UUID idFromPath) {
        Reply reply = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, reply.getUser().getId());
        counter.updateRepliesCount(reply.getComment(), false);
        repository.delete(reply);
    }

    @Transactional(readOnly = true)
    @Override
    public PageRES<DetailReplyRES> getReplies(Pageable pageable, UUID idFromToken, UUID commentIdFromPath) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        Note requested = commentRepository.findById(commentIdFromPath).orElseThrow(EntityNotFoundException::new).getNote();
        User author = requested.getUser();
        if (author != null) {
            if (requested.isHidden()) validateAccess(idFromToken, author.getId());
            if (author.isProfilePrivate()) followService.validateBidirectionalFollowAccess(requesting, author);
        }
        Page<DetailReplyRES> page = repository.findAllByCommentId(pageable, commentIdFromPath).map(DetailReplyRES::new);
        return new PageRES<>(page);
    }

}