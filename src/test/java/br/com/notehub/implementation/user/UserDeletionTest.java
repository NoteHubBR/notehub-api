package br.com.notehub.implementation.user;

import br.com.notehub.application.implementation.user.UserServiceImpl;
import br.com.notehub.domain.note.NoteService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDeletionTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repository;

    @Mock
    private NoteService noteService;

    @Mock
    private PasswordEncoder encoder;

    private User user;

    private void mockFindById(User u) {
        when(repository.findById(u.getId())).thenReturn(Optional.of(u));
    }

    private void mockMatches(boolean condition) {
        when(encoder.matches(anyString(), anyString())).thenReturn(condition);
    }

    @BeforeEach
    void setup() {
        user = new User("tester@notehub.com.br", "tester", "TESTER", "123");
        user.setId(UUID.randomUUID());
        user.setActive(true);
    }

    @Test
    void shouldDeleteUser_whenPasswordMatches() {

        mockFindById(user);
        mockMatches(true);

        service.delete(user.getId(), user.getPassword());

        verify(repository).findById(user.getId());
        verify(repository).delete(user);
        verify(noteService).deleteAllUserHiddenNotes(user);

    }

    @Test
    void shouldThrowBadCredentialsException_whenPasswordDoesNotMatch() {

        mockFindById(user);
        mockMatches(false);

        assertThatThrownBy(() ->
                service.delete(user.getId(), user.getPassword()))
                .isInstanceOf(BadCredentialsException.class);

        verify(repository).findById(user.getId());
        verify(repository, never()).delete(user);
        verify(noteService, never()).deleteAllUserHiddenNotes(user);

    }

}