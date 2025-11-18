package br.com.notehub.implementation.user;

import br.com.notehub.application.implementation.user.UserServiceImpl;
import br.com.notehub.domain.history.UserHistoryService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserEditionTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repository;

    @Mock
    private UserHistoryService historian;

    private User user;

    private User updateUser(String name, boolean profilePrivate) {
        return new User(name, name.toUpperCase(), null, null, null, profilePrivate);
    }

    private void mockFindById(User user) {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @BeforeEach
    void setup() {
        user = new User("tester@notehub.com.br", "tester", "TESTER", "123");
        user.setId(UUID.randomUUID());
        user.setActive(true);
    }

    @Test
    void shouldSaveHistoryAndPersistChanges_whenDataIsModified() {

        User updated = updateUser("updated", true);

        mockFindById(user);

        assertThat(updated).isEqualTo(service.edit(user.getId(), updated));

        verify(repository, times(6)).findById(user.getId());
        verify(repository, times(3)).save(user);
        verify(historian).setHistory(user, "username", "tester", "updated");
        verify(historian).setHistory(user, "display_name", "TESTER", "UPDATED");
        verify(historian).setHistory(user, "profile_private", "false", "true");

    }

    @Test
    void shouldSkipSaveAndHistory_whenUserDataIsUnchanged() {

        User updated = updateUser("tester", false);

        mockFindById(user);

        assertThat(updated).isEqualTo(service.edit(user.getId(), updated));

        verify(repository, times(6)).findById(user.getId());
        verify(repository, never()).save(user);
        verify(historian, never()).setHistory(any(), any(), any(), any());

    }

    @Test
    void shouldThrowDataIntegrityViolationException_whenUsernameAlreadyExists() {

        User updated = updateUser("tester", false);

        mockFindById(user);
        when(repository.findByUsername("tester")).thenReturn(Optional.of(updated));

        assertThatThrownBy(() -> service.edit(user.getId(), updated))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("username");

        verify(repository).findById(user.getId());
        verify(repository, never()).save(user);
        verify(historian, never()).setHistory(any(), any(), any(), any());

    }

}