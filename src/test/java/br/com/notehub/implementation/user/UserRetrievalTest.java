package br.com.notehub.implementation.user;

import br.com.notehub.application.implementation.user.UserServiceImpl;
import br.com.notehub.domain.history.UserHistoryService;
import br.com.notehub.domain.user.Subscription;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRetrievalTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repository;

    @Mock
    private UserHistoryService historian;

    private User user;
    private Pageable pageable;

    private User createUser(String email, String username) {
        User u = new User(email, username, username.toUpperCase(), "123");
        u.setId(UUID.randomUUID());
        u.setActive(true);
        return u;
    }

    private void mockFindById(User u) {
        when(repository.findById(u.getId())).thenReturn(Optional.of(u));
    }

    private void mockFindByUsername(User u) {
        when(repository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
    }

    @BeforeEach
    void setup() {
        user = createUser("tester@notehub.com.br", "tester");
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void shouldReturnUser_whenUsernameWasFoundAndUserIsActive() {
        mockFindByUsername(user);
        User result = service.getUser(user.getUsername());
        verify(repository).findByUsername(user.getUsername());
        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldThrowEntityNotFoundException_whenUsernameWasNotFoundOrUserIsInactive() {
        when(repository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUser(user.getUsername()))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository).findByUsername(user.getUsername());
    }

    @Test
    void shouldReturnListOfActiveUsers() {
        when(repository.findAllByActiveTrue()).thenReturn(List.of(user));
        List<User> result = service.getAllActiveUsers();
        verify(repository).findAllByActiveTrue();
        assertThat(result).containsExactly(user);
    }

    @Test
    void shouldReturnPageOfActiveUsersByQuery() {
        String query = "tester";
        Page<User> page = new PageImpl<>(List.of(user));
        when(repository.findAllActiveUsersByUsernameOrDisplayName(pageable, query)).thenReturn(page);
        Page<User> result = service.findAll(pageable, query);
        verify(repository).findAllActiveUsersByUsernameOrDisplayName(pageable, query);
        assertThat(result).isEqualTo(page);
    }

    @Test
    void shouldReturnListOfUserUsernameHistory() {
        List<String> expected = List.of("tester", "new");
        mockFindByUsername(user);
        when(historian.getLastFiveUserDisplayName(user)).thenReturn(expected);
        List<String> history = service.getUserDisplayNameHistory(user.getUsername());
        assertThat(history).containsExactlyElementsOf(expected);
    }

    @Test
    void shouldReturnListOfUserSubscriptions() {
        mockFindById(user);
        Set<Subscription> expected = Set.of(Subscription.MAINTENANCE, Subscription.RELEASE);
        Set<Subscription> subscriptions = service.getUserSubscriptions(user.getId());
        verify(repository).findById(user.getId());
        assertThat(subscriptions).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void shouldCleanUsersWithExpiredActivationTime() {
        User u1 = createUser("a@mail.com", "a");
        User u2 = createUser("b@mail.com", "b");
        List<User> expired = List.of(u1, u2);
        when(repository.findUsersWithExpiredActivationTime(any())).thenReturn(expired);
        service.cleanUsersWithExpiredActivationTime();
        verify(repository).findUsersWithExpiredActivationTime(any());
        verify(repository).deleteAll(expired);
    }

}