package br.com.notehub.application.implementation.user;

import br.com.notehub.application.counter.Counter;
import br.com.notehub.application.dto.notification.MessageNotification;
import br.com.notehub.domain.history.UserHistoryService;
import br.com.notehub.domain.note.NoteService;
import br.com.notehub.domain.notification.NotificationService;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.Host;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import br.com.notehub.domain.user.UserService;
import br.com.notehub.infra.exception.CustomExceptions;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserHistoryService historian;
    private final NotificationService notifier;
    private final TokenService tokenService;
    private final NoteService noteService;
    private final PasswordEncoder encoder;
    private final Counter counter;

    @SneakyThrows
    private <T> void changeField(UUID idFromToken, String field, Function<User, T> getter, Consumer<User> setter) {
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        T oldValue = getter.apply(user);
        setter.accept(user);
        T newValue = getter.apply(user);
        if (Objects.equals(oldValue, newValue)) return;
        repository.save(user);
        historian.setHistory(user, field, String.valueOf(oldValue), newValue.toString());
    }

    private void validateHost(Host host) {
        if (!Objects.equals(host, Host.NOTEHUB)) throw new CustomExceptions.HostNotAllowedException();
    }

    private String validatePassword(String oldPassword, String newPassword) {
        if (encoder.matches(newPassword, oldPassword)) throw new CustomExceptions.SamePasswordException();
        return encoder.encode(newPassword);
    }

    private void validateEmail(String oldEmail, String newEmail) {
        repository.findByEmail(newEmail).ifPresent(user -> {
            if (Objects.equals(oldEmail, newEmail)) throw new CustomExceptions.SameEmailExpection();
            throw new DataIntegrityViolationException("email");
        });
    }

    private void validateUsername(UUID idFromToken, String username) {
        repository.findByUsername(username).ifPresent((stored) -> {
            repository.findById(idFromToken).ifPresent((user) -> {
                        if (!Objects.equals(stored, user)) {
                            throw new DataIntegrityViolationException("username");
                        }
                    }
            );
        });
    }

    private void validateActiveField(boolean active) {
        if (!active) throw new EntityNotFoundException();
    }

    private void validateBidirectionalFollowAccess(@Nullable User requesting, User requested) {
        if (!requested.isProfilePrivate()) return;
        if (requesting == null) throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        boolean isSameUser = Objects.equals(requesting.getUsername(), requested.getUsername());
        boolean requestedContainsRequesting = requested.getFollowing().contains(requesting);
        boolean requestingContainsRequested = requesting.getFollowing().contains(requested);
        if (!isSameUser && (!requestedContainsRequesting || !requestingContainsRequested)) {
            throw new AccessDeniedException("Não há vínculo bidirecional entre os usuários.");
        }
    }

    private Page<User> getUserConnections(Pageable pageable, String q, UUID userRequestingId, String userRequestedUsername, Function<User, Set<User>> getter) {
        User requesting = (userRequestingId != null) ? repository.findById(userRequestingId).orElseThrow(EntityNotFoundException::new) : null;
        User requested = repository.findByUsername(userRequestedUsername).orElseThrow(EntityNotFoundException::new);
        validateBidirectionalFollowAccess(requesting, requested);
        List<UUID> ids = getter.apply(requested).stream().map(User::getId).toList();
        return repository.findAllByIdIn(pageable, q, ids);
    }

    private boolean isFollowing(User follower, User following) {
        if (Objects.equals(follower.getId(), following.getId())) {
            throw new CustomExceptions.SelfFollowException();
        }
        return following.getFollowers().contains(follower);
    }

    @Transactional
    @Override
    public User create(User user) {
        boolean existsByEmail = repository.existsByEmail(user.getEmail());
        boolean existsByUsername = repository.existsByUsername(user.getUsername());
        if (existsByEmail && existsByUsername) throw new DataIntegrityViolationException("both");
        if (existsByEmail) throw new DataIntegrityViolationException("email");
        if (existsByUsername) throw new DataIntegrityViolationException("username");
        user.setPassword(encoder.encode(user.getPassword()));
        return repository.save(user);
    }

    @Override
    public String generateActivationToken(User user) {
        return tokenService.generateActivationToken(user);
    }

    @Transactional
    @Override
    public void activate(UUID idFromToken) {
        changeField(idFromToken, "active", User::isActive, user -> user.setActive(true));
    }

    @Transactional
    @Override
    public void changePassword(String email, String newPassword) {
        User entity = repository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        String password = validatePassword(entity.getPassword(), newPassword);
        changeField(entity.getId(), "password", User::getPassword, user -> user.setPassword(password));
    }

    @Transactional
    @Override
    public void changeEmail(String oldEmail, String newEmail) {
        validateEmail(oldEmail, newEmail);
        User entity = repository.findByEmail(oldEmail).orElseThrow(EntityNotFoundException::new);
        validateHost(entity.getHost());
        changeField(entity.getId(), "email", User::getEmail, user -> user.setEmail(newEmail.toLowerCase()));
    }

    @Transactional
    @Override
    public User edit(UUID idFromToken, User user) {
        validateUsername(idFromToken, user.getUsername());
        changeField(idFromToken, "username", User::getUsername, stored -> stored.setUsername(user.getUsername()));
        changeField(idFromToken, "display_name", User::getDisplayName, stored -> stored.setDisplayName(user.getDisplayName()));
        changeField(idFromToken, "avatar", User::getAvatar, stored -> stored.setAvatar(user.getAvatar()));
        changeField(idFromToken, "banner", User::getBanner, stored -> stored.setBanner(user.getBanner()));
        changeField(idFromToken, "message", User::getMessage, stored -> stored.setMessage(user.getMessage()));
        changeField(idFromToken, "profile_private", User::isProfilePrivate, stored -> stored.setProfilePrivate(user.isProfilePrivate()));
        return user;
    }

    @Transactional
    @Override
    public void changeProfileVisibility(UUID idFromToken) {
        changeField(idFromToken, "profile_private", User::isProfilePrivate, user -> user.setProfilePrivate(!user.isProfilePrivate()));
    }

    @Transactional
    @Override
    public void changeUsername(UUID idFromToken, String username) {
        validateUsername(idFromToken, username);
        changeField(idFromToken, "username", User::getUsername, user -> user.setUsername(username.toLowerCase()));
    }

    @Transactional
    @Override
    public void changeDisplayName(UUID idFromToken, String displayName) {
        changeField(idFromToken, "display_name", User::getDisplayName, user -> user.setDisplayName(displayName));
    }

    @Transactional
    @Override
    public void changeAvatar(UUID idFromToken, String avatar) {
        changeField(idFromToken, "avatar", User::getAvatar, user -> user.setAvatar(avatar));
    }

    @Transactional
    @Override
    public void changeBanner(UUID idFromToken, String banner) {
        changeField(idFromToken, "banner", User::getBanner, user -> user.setBanner(banner));
    }

    @Transactional
    @Override
    public void changeMessage(UUID idFromToken, String message) {
        changeField(idFromToken, "message", User::getMessage, user -> user.setMessage(message));
    }

    @Transactional
    @Override
    public void follow(UUID idFromToken, String username) {
        User follower = repository.findByIdWithFollowersAndFollowing(idFromToken).orElseThrow(EntityNotFoundException::new);
        User following = repository.findByUsernameWithFollowersAndFollowing(username).orElseThrow(EntityNotFoundException::new);
        if (isFollowing(follower, following)) throw new CustomExceptions.AlreadyFollowingException();
        counter.updateFollowersAndFollowingCount(follower, following, true);
        notifier.notify(follower, following, follower, MessageNotification.of(follower));
    }

    @Transactional
    @Override
    public void unfollow(UUID idFromToken, String username) {
        User follower = repository.findByIdWithFollowersAndFollowing(idFromToken).orElseThrow(EntityNotFoundException::new);
        User following = repository.findByUsernameWithFollowersAndFollowing(username).orElseThrow(EntityNotFoundException::new);
        if (!isFollowing(follower, following)) throw new CustomExceptions.NotFollowingException();
        counter.updateFollowersAndFollowingCount(follower, following, false);
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, String password) {
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        boolean matches = encoder.matches(password, user.getPassword());
        if (!matches) throw new BadCredentialsException("password");
        user.getFollowing().forEach(following -> {
            following.getFollowers().remove(user);
            following.setFollowersCount(following.getFollowersCount() - 1);
            repository.save(following);
        });
        user.getFollowers().forEach(follower -> {
            follower.getFollowing().remove(user);
            follower.setFollowingCount(follower.getFollowingCount() - 1);
            repository.save(follower);
        });
        if (user.isProfilePrivate()) noteService.deleteAllUserNotes(user);
        else noteService.deleteAllUserHiddenNotes(user);
        repository.delete(user);
    }

    @Override
    public Page<User> findAll(Pageable pageable, String q) {
        return repository.findAllActiveUsersByUsernameOrDisplayName(pageable, q);
    }

    @Override
    public User getUser(String username) {
        User user = repository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        validateActiveField(user.isActive());
        return user;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<User> getUserFollowing(Pageable pageable, String q, UUID idFromToken, String username) {
        return getUserConnections(pageable, q, idFromToken, username, User::getFollowing);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<User> getUserFollowers(Pageable pageable, String q, UUID idFromToken, String username) {
        return getUserConnections(pageable, q, idFromToken, username, User::getFollowers);
    }

    @Override
    public Set<String> getUserMutualConnections(UUID id) {
        User user = repository.findByIdWithFollowersAndFollowing(id).orElseThrow(EntityNotFoundException::new);
        Set<String> following = user.getFollowing().stream().map(User::getUsername).collect(Collectors.toSet());
        Set<String> followers = user.getFollowers().stream().map(User::getUsername).collect(Collectors.toSet());
        following.retainAll(followers);
        return following;
    }

    @Override
    public List<String> getUserDisplayNameHistory(String username) {
        User user = repository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        validateActiveField(user.isActive());
        return historian.getLastFiveUserDisplayName(user);
    }

    @Transactional
    @Override
    public void cleanUsersWithExpiredActivationTime() {
        Instant nowMinus7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        List<User> usersWithExpiredActivationTime = repository.findUsersWithExpiredActivationTime(nowMinus7Days);
        repository.deleteAll(usersWithExpiredActivationTime);
    }

}