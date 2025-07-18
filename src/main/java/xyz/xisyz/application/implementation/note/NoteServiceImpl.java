package xyz.xisyz.application.implementation.note;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.xisyz.application.counter.Counter;
import xyz.xisyz.application.dto.request.note.CreateNoteREQ;
import xyz.xisyz.application.dto.response.note.DetailNoteRES;
import xyz.xisyz.application.dto.response.note.LowDetailNoteRES;
import xyz.xisyz.application.dto.response.page.PageRES;
import xyz.xisyz.domain.note.Note;
import xyz.xisyz.domain.note.NoteRepository;
import xyz.xisyz.domain.note.NoteService;
import xyz.xisyz.domain.tag.Tag;
import xyz.xisyz.domain.tag.TagRepository;
import xyz.xisyz.domain.user.User;
import xyz.xisyz.domain.user.UserRepository;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final NoteRepository repository;
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

    private void deleteNoteAndFlush(Note note) {
        repository.delete(note);
        repository.flush();
    }

    private void removeOrphanTags(List<String> names) {
        List<Tag> tags = tagRepository.findAllByNameIn(names);
        tags.stream().filter(tag -> tag.getNotes().isEmpty()).forEach(tagRepository::delete);
    }

    private List<Tag> findOrCreateTags(List<String> tags) {
        if (tags == null) return null;
        List<Tag> immutableList = tags.stream()
                .map(String::toLowerCase).distinct()
                .map(tag -> tagRepository.findByName(tag).orElseGet(() -> new Tag(tag.toLowerCase())))
                .toList();
        return new ArrayList<>(immutableList);
    }

    private void changeField(UUID idFromToken, UUID idFromPath, Consumer<Note> setter) {
        Note note = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, note.getUser().getId());
        setter.accept(note);
        note.setModifiedAt(Instant.now());
        note.setModified(true);
        repository.saveAndFlush(note);
    }

    public Note mapToNote(UUID idFromToken, CreateNoteREQ req) {
        User user = userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        List<Tag> tags = findOrCreateTags(req.tags());
        return new Note(user, req.title(), req.description(), req.markdown(), req.closed(), req.hidden(), tags);
    }

    @Transactional
    @Override
    public LowDetailNoteRES create(UUID idFromToken, CreateNoteREQ req) {
        Note note = mapToNote(idFromToken, req);
        repository.save(note);
        counter.updateNotesCount(note.getUser(), true);
        return new LowDetailNoteRES(note);
    }

    @Transactional
    @Override
    public void edit(UUID idFromToken, UUID idFromPath, String title, String description, List<String> tags, boolean closed, boolean hidden) {
        changeField(idFromToken, idFromPath, note -> {
            List<String> oldTags = note.getTags().stream().map(Tag::getName).toList();
            note.setTitle(title);
            note.setDescription(description);
            note.setTags(findOrCreateTags(tags));
            note.setClosed(closed);
            note.setHidden(hidden);
            removeOrphanTags(oldTags);
        });
    }

    @Transactional
    @Override
    public void changeTitle(UUID idFromToken, UUID idFromPath, String title) {
        changeField(idFromToken, idFromPath, note -> note.setTitle(title));
    }

    @Transactional
    @Override
    public void changeDescription(UUID idFromToken, UUID idFromPath, String description) {
        changeField(idFromToken, idFromPath, note -> note.setDescription(description));
    }

    @Transactional
    @Override
    public void changeMarkdown(UUID idFromToken, UUID idFromPath, String markdown) {
        changeField(idFromToken, idFromPath, note -> note.setMarkdown(markdown));
    }

    @Transactional
    @Override
    public void changeClosed(UUID idFromToken, UUID idFromPath) {
        changeField(idFromToken, idFromPath, note -> note.setClosed(!note.isClosed()));
    }

    @Transactional
    @Override
    public void changeHidden(UUID idFromToken, UUID idFromPath) {
        changeField(idFromToken, idFromPath, note -> note.setHidden(!note.isHidden()));
    }

    @Transactional
    @Override
    public void changeTags(UUID idFromToken, UUID idFromPath, List<String> tags) {
        changeField(idFromToken, idFromPath, note -> {
            List<String> oldTagNames = note.getTags().stream().map(Tag::getName).toList();
            note.setTags(findOrCreateTags(tags));
            removeOrphanTags(oldTagNames);
        });
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, UUID idFromPath) {
        Note note = repository.findById(idFromPath).orElseThrow(EntityNotFoundException::new);
        validateAccess(idFromToken, note.getUser().getId());
        List<String> oldTagNames = note.getTags().stream().map(Tag::getName).toList();
        deleteNoteAndFlush(note);
        removeOrphanTags(oldTagNames);
        counter.updateNotesCount(note.getUser(), false);
    }

    @Transactional
    @Override
    public void deleteAllUserNotes(User user) {
        List<String> tags = tagRepository.findAllByNotesUserId(user.getId()).stream().map(Tag::getName).toList();
        repository.deleteAllByUserId(user.getId());
        repository.flush();
        removeOrphanTags(tags);
    }

    @Transactional
    @Override
    public void deleteAllUserHiddenNotes(User user) {
        List<String> tags = tagRepository.findAllByNotesUserIdAndNotesHiddenTrue(user.getId()).stream().map(Tag::getName).toList();
        repository.deleteAllByUserIdAndHiddenTrue(user.getId());
        repository.flush();
        removeOrphanTags(tags);
    }

    @Override
    public List<String> getAllTags() {
        return tagRepository.findAll().stream().map(Tag::getName).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> getAllPublicUserTags(UUID idFromToken, String username) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        if (requested.isProfilePrivate()) validateBidirectionalFollowAccess(requesting, requested);
        return tagRepository.findAllByNotesUserUsernameAndNotesHiddenFalseOrderByNameAsc(username).stream().map(Tag::getName).toList();
    }

    @Override
    public List<String> getAllPrivateUserTags(UUID idFromToken) {
        return tagRepository.findAllByNotesUserId(idFromToken).stream().map(Tag::getName).toList();
    }

    @Override
    public PageRES<LowDetailNoteRES> findPublicNotes(Pageable pageable, String q) {
        Page<LowDetailNoteRES> page = repository.searchPublicNotesByTitleOrDescription(pageable, q).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPrivateNotes(Pageable pageable, UUID idFromToken, String q) {
        Page<LowDetailNoteRES> page = repository.searchPrivateNotesByTitleOrTag(pageable, idFromToken, q).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPublicNotesByTag(Pageable pageable, String tag) {
        Page<LowDetailNoteRES> page = repository.searchPublicNotesByTag(pageable, tag).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> findPrivateNotesByTag(Pageable pageable, UUID idFromToken, String tag) {
        Page<LowDetailNoteRES> page = repository.searchPrivateNotesByTag(pageable, idFromToken, tag).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Transactional(readOnly = true)
    @Override
    public PageRES<LowDetailNoteRES> findUserNotesBySpecs(UUID idFromToken, Pageable pageable, String username, String q, String tag, String type) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        if (Objects.equals(type, "hidden")) validateAccess(idFromToken, requested.getId());
        if (requested.isProfilePrivate()) validateBidirectionalFollowAccess(requesting, requested);
        Page<LowDetailNoteRES> page = repository.searchUserNotesBySpecs(pageable, username, q, tag, type).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Transactional(readOnly = true)
    @Override
    public DetailNoteRES getNote(UUID idFromToken, UUID idFromPath) {
        User requesting = (idFromToken != null) ? userRepository.findById(idFromToken).orElseThrow(EntityNotFoundException::new) : null;
        Note requested = repository.findNote(idFromPath).orElseThrow(EntityNotFoundException::new);
        User author = requested.getUser();
        if (author != null) {
            if (requested.isHidden()) validateAccess(idFromToken, author.getId());
            if (author.isProfilePrivate()) validateBidirectionalFollowAccess(requesting, author);
        }
        return new DetailNoteRES(requested);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllUserNotesByUsername(Pageable pageable, String username) {
        Page<Note> notes = repository.findAllByUserProfilePrivateFalseAndUserUsernameAndHiddenFalse(pageable, username.toLowerCase());
        Page<LowDetailNoteRES> page = notes.map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllUserNotesById(Pageable pageable, UUID idFromToken) {
        Page<LowDetailNoteRES> page = repository.findAllByUserId(pageable, idFromToken).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllFollowedUsersNotes(Pageable pageable, UUID idFromToken) {
        User user = userRepository.findByIdWithFollowersAndFollowing(idFromToken).orElseThrow(EntityNotFoundException::new);
        Set<UUID> following = user.getFollowing().stream().map(User::getId).collect(Collectors.toSet());
        Set<UUID> followers = user.getFollowers().stream().map(User::getId).collect(Collectors.toSet());
        Set<UUID> mutuals = new HashSet<>(following);
        mutuals.retainAll(followers);
        Page<LowDetailNoteRES> page = repository.findAllForUserFeed(pageable, following, mutuals).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

    @Override
    public PageRES<LowDetailNoteRES> getAllFollowedUserNotes(Pageable pageable, UUID idFromToken, String username) {
        User requesting = userRepository.findByIdWithFollowersAndFollowing(idFromToken).orElseThrow(EntityNotFoundException::new);
        User requested = userRepository.findByUsernameWithFollowersAndFollowing(username).orElseThrow(EntityNotFoundException::new);
        if (requested.isProfilePrivate()) validateBidirectionalFollowAccess(requesting, requested);
        Page<LowDetailNoteRES> page = repository.findAllByUserUsernameAndHiddenFalse(pageable, username).map(LowDetailNoteRES::new);
        return new PageRES<>(page);
    }

}