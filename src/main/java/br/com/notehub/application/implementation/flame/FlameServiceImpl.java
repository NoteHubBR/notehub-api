package br.com.notehub.application.implementation.flame;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.application.dto.response.flame.DetailFlameRES;
import br.com.notehub.application.dto.response.page.PageRES;
import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.flame.Flame;
import br.com.notehub.domain.flame.FlameRepository;
import br.com.notehub.domain.flame.FlameService;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.note.Note;
import br.com.notehub.domain.note.NoteRepository;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FlameServiceImpl implements FlameService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final FlameRepository repository;
    private final FollowService followService;
    private final NotificationService notifier;
    private final Counter counter;
    private final FeedService feeder;

    @Transactional
    @Override
    public DetailFlameRES inflame(UUID userIdFromToken, UUID noteIdFromPath) {
        User user = userRepository.findById(userIdFromToken).orElseThrow(EntityNotFoundException::new);
        Note note = noteRepository.findById(noteIdFromPath).orElseThrow(EntityNotFoundException::new);
        if (repository.existsByUserAndNote(user, note)) throw new EntityExistsException();
        Flame flame = repository.save(new Flame(user, note));
        counter.updateFlamesCount(note, true);
        notifier.notify(flame.getUser(), note.getUser(), note.getUser(), MessageNotification.of(flame));
        feeder.onNoteFlamed(flame.getId());
        return new DetailFlameRES(flame);
    }

    @Transactional
    @Override
    public void deflame(UUID userIdFromToken, UUID noteIdFromPath) {
        Flame flame = repository.findByUserIdAndNoteId(userIdFromToken, noteIdFromPath).orElseThrow(EntityNotFoundException::new);
        repository.delete(flame);
        counter.updateFlamesCount(flame.getNote(), false);
    }

    @Transactional(readOnly = true)
    @Override
    public PageRES<DetailFlameRES> getUserFlames(UUID userIdFromToken, Pageable pageable, String username, String q) {
        User requesting = (userIdFromToken != null) ? userRepository.findById(userIdFromToken).orElseThrow(EntityNotFoundException::new) : null;
        User requested = userRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        followService.validateBidirectionalFollowAccess(requesting, requested);
        Page<DetailFlameRES> page = repository.getUserFlames(pageable, username, q).map(DetailFlameRES::new);
        return new PageRES<>(page);
    }

}