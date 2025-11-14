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
    private UUID id;

    @BeforeEach
    void setup() {
        user = new User("tester@notehub.com.br", "tester", "Tester", "1234");
        id = UUID.randomUUID();
        user.setId(id);
        user.setActive(true);
    }

    @Test
    void shouldSaveHistoryAndPersistChanges_whenDataIsModified() {

        User updated = new User("updated", "Updated", null, null, null, true);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThat(updated).isEqualTo(service.edit(id, updated));

        verify(repository, times(6)).findById(id);
        verify(repository, times(3)).save(any(User.class));
        verify(historian).setHistory(any(User.class), eq("username"), eq("tester"), eq("updated"));
        verify(historian).setHistory(any(User.class), eq("display_name"), eq("Tester"), eq("Updated"));
        verify(historian).setHistory(any(User.class), eq("profile_private"), eq("false"), eq("true"));

    }

    @Test
    void shouldSkipSaveAndHistory_whenUserDataIsUnchanged() {

        User updated = new User("tester", "Tester", null, null, null, false);

        when(repository.findById(id)).thenReturn(Optional.of(user));

        assertThat(updated).isEqualTo(service.edit(id, updated));

        verify(repository, times(6)).findById(id);
        verify(repository, never()).save(any(User.class));
        verify(historian, never()).setHistory(any(), any(), any(), any());

    }

    @Test
    void shouldThrowException_whenUsernameAlreadyExists() {

        User updated = new User("tester", "Updated", null, null, null, false);

        when(repository.findByUsername("tester")).thenReturn(Optional.of(user));
        when(repository.findById(id)).thenReturn(Optional.of(updated));

        assertThatThrownBy(() -> service.edit(id, updated))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("username");

        verify(repository, times(1)).findById(id);
        verify(repository, never()).save(any(User.class));
        verify(historian, never()).setHistory(any(), any(), any(), any());

    }

}