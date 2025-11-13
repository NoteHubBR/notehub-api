package br.com.notehub.implementation.user;

import br.com.notehub.application.implementation.user.UserServiceImpl;
import br.com.notehub.domain.history.UserHistoryService;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreationTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repository;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserHistoryService historian;

    @Mock
    private PasswordEncoder encoder;

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
    void shouldEncodePasswordAndSaveUser_whenDataIsValid() {

        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(repository.existsByUsername(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = service.create(user);

        verify(repository).existsByEmail("tester@notehub.com.br");
        verify(repository).existsByUsername("tester");
        verify(encoder).encode("1234");
        verify(repository).save(any(User.class));

        assertThat(result).isInstanceOf(User.class);
        assertThat(result.getEmail()).isEqualTo("tester@notehub.com.br");
        assertThat(result.getUsername()).isEqualTo("tester");
        assertThat(result.getDisplayName()).isEqualTo("Tester");
        assertThat(result.getPassword()).isEqualTo("encoded");

    }

    @Test
    void shouldThrowException_whenEmailAlreadyExists() {

        when(repository.existsByEmail(anyString())).thenReturn(true);
        when(repository.existsByUsername(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("email");

        verify(repository, never()).save(any());

    }

    @Test
    void shouldThrowException_whenUsernameAlreadyExists() {

        when(repository.existsByEmail(anyString())).thenReturn(false);
        when(repository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("username");

        verify(repository, never()).save(any());

    }

    @Test
    void shouldThrowException_whenEmailAndUsernameAlreadyExist() {

        when(repository.existsByEmail(anyString())).thenReturn(true);
        when(repository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("both");

        verify(repository, never()).save(any());

    }

    @Test
    void shouldReturnToken_whenGeneratingActivationToken() {
        when(tokenService.generateActivationToken(any(User.class))).thenReturn("token");
        String token = service.generateActivationToken(user);
        assertThat(token).isEqualTo("token");
    }

    @Test
    void shouldSetActiveTrueAndLogHistory_whenActivatingUser() {

        User inactiveUser = new User("inactive@notehub.com.br", "inactive", "Inactive", "1234");
        inactiveUser.setId(id);
        inactiveUser.setActive(false);

        when(repository.findById(id)).thenReturn(Optional.of(inactiveUser));
        when(repository.save(any(User.class))).thenReturn(inactiveUser);

        service.activate(id);

        verify(repository).save(any(User.class));
        verify(historian).setHistory(any(), eq("active"), eq("false"), eq("true"));

    }

}