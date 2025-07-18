package xyz.xisyz.application.implementation.comment;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.xisyz.application.counter.Counter;
import xyz.xisyz.application.dto.notification.MessageNotification;
import xyz.xisyz.application.dto.request.comment.CreateCommentREQ;
import xyz.xisyz.application.dto.response.comment.CreateCommentRES;
import xyz.xisyz.application.dto.response.comment.DetailCommentRES;
import xyz.xisyz.application.dto.response.page.PageRES;
import xyz.xisyz.domain.comment.Comment;
import xyz.xisyz.domain.comment.CommentRepository;
import xyz.xisyz.domain.comment.CommentService;
import xyz.xisyz.domain.note.Note;
import xyz.xisyz.domain.note.NoteRepository;
import xyz.xisyz.domain.notification.NotificationService;
import xyz.xisyz.domain.user.User;
import xyz.xisyz.domain.user.UserRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final CommentRepository repository;
    private final NotificationService notifier;
    private final Counter counter;

    private void validateAccess(@Nullable UUID idFromToken, UUID idFromRequested) {
        if (idFromToken == null) throw new AccessDeniedException("Usuário sem permissão.");
        if (!Objects.equals(idFromToken, idFromRequested)) {
            throw new AccessDeniedException("Usuário sem permissão.");
        }
    }

    private void validateBidirectionalFollowAccess(@Nullable User requesting, User requested) {
        if (requesting == null) throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        boolean isSameUser = Objects.equals(requesting.getUsername(), requested.getUsername());
        boolean requestedContainsRequesting = requested.getFollowing().contains(requesting);
        boolean requestingContainsRequested = requesting.getFollowing().contains(requested);
        if (!isSameUser && (!requestedContainsRequesting || !requestingContainsRequested)) {
            throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        }
    }

    private void assertNoteIsNotClosed(Comment comment) {
        if (comment.getNote().isClosed()) throw new IllegalStateException("Nota fechada para novas interações.");
    }

    public Comment mapToComment(UUID idFromToken, UUID noteIdFromPath, CreateCommentREQ req) {
        User user = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        Note note = noteRepository.findById(noteIdFromPath).orElseThrow(EntityNotFoundException::new);
        return new Comment(user, note, req.text());
    }

    @Transactional
    @Override
    public CreateCommentRES create(UUID idFromToken, UUID noteIdFromPath, CreateCommentREQ req) {
        Comment comment = mapToComment(idFromToken, noteIdFromPath, req);
        assertNoteIsNotClosed(comment);
        repository.save(comment);
        counter.updateCommentsCount(comment.getNote(), true);
        notifier.notify(comment.getNote().getUser(), MessageNotification.of(comment));
        return new CreateCommentRES(comment);
    }

    @Transactional
    @Override
    public void edit(UUID idFromToken, UUID idFromPath, String text) {
        Comment comment = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, comment.getUser().getId());
        assertNoteIsNotClosed(comment);
        comment.setText(text);
        comment.setModified(true);
        comment.setModifiedAt(Instant.now());
        repository.save(comment);
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, UUID idFromPath) {
        Comment comment = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, comment.getUser().getId());
        repository.delete(comment);
        counter.updateCommentsCount(comment.getNote(), false);
    }

    @Transactional(readOnly = true)
    @Override
    public PageRES<DetailCommentRES> getComments(Pageable pageable, UUID idFromToken, UUID noteIdFromPath) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        Note requested = noteRepository.findById(noteIdFromPath).orElseThrow(EntityNotFoundException::new);
        User author = requested.getUser();
        if (author != null) {
            if (requested.isHidden()) validateAccess(idFromToken, author.getId());
            if (author.isProfilePrivate()) validateBidirectionalFollowAccess(requesting, author);
        }
        Page<DetailCommentRES> page = repository.findAllByNoteId(pageable, requested.getId()).map(DetailCommentRES::new);
        return new PageRES<>(page);
    }

}