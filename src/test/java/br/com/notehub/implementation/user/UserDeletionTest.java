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
    private UUID id;

    @BeforeEach
    void setup() {
        user = new User("tester@notehub.com.br", "tester", "Tester", "1234");
        id = UUID.randomUUID();
        user.setId(id);
    }

    @Test
    void shouldDeleteUser_whenPasswordMatches() {

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);

        service.delete(id, user.getPassword());

        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).delete(user);
        verify(noteService, times(1)).deleteAllUserHiddenNotes(eq(user));

    }

    @Test
    void shouldThrowBadCredentialsException_whenPasswordDoesNotMatch() {

        when(repository.findById(id)).thenReturn(Optional.of(user));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                service.delete(id, user.getPassword()))
                .isInstanceOf(BadCredentialsException.class);

        verify(repository, times(1)).findById(id);
        verify(repository, never()).delete(user);
        verify(noteService, never()).deleteAllUserHiddenNotes(eq(user));

    }

}