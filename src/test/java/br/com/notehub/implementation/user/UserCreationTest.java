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

    private User createUser(String email, String username) {
        User u = new User(email, username, username.toUpperCase(), "123");
        u.setId(UUID.randomUUID());
        u.setActive(true);
        return u;
    }

    private void mockExistsByEmail(User u, boolean condition) {
        when(repository.existsByEmail(u.getEmail())).thenReturn(condition);
    }

    private void mockExistsByUsername(User u, boolean condition) {
        when(repository.existsByUsername(u.getUsername())).thenReturn(condition);
    }

    private void mockSave(User u) {
        when(repository.save(u)).thenReturn(u);
    }

    @BeforeEach
    void setup() {
        user = createUser("tester@notehub.com.br", "tester");
    }

    @Test
    void shouldEncodePasswordAndSaveUser_whenDataIsValid() {

        mockExistsByEmail(user, false);
        mockExistsByUsername(user, false);
        mockSave(user);
        when(encoder.encode(user.getPassword())).thenReturn("encoded");

        User result = service.create(user);

        verify(repository).existsByEmail(user.getEmail());
        verify(repository).existsByUsername(user.getUsername());
        verify(encoder).encode("123");
        verify(repository).save(user);

        assertThat(result).isEqualTo(user);
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getUsername()).isEqualTo(user.getUsername());
        assertThat(result.getDisplayName()).isEqualTo(user.getDisplayName());
        assertThat(result.getPassword()).isEqualTo("encoded");

    }

    @Test
    void shouldThrowException_whenEmailAlreadyExists() {

        mockExistsByEmail(user, true);
        mockExistsByUsername(user, false);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("email");

        verify(repository, never()).save(user);

    }

    @Test
    void shouldThrowException_whenUsernameAlreadyExists() {

        mockExistsByEmail(user, false);
        mockExistsByUsername(user, true);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("username");

        verify(repository, never()).save(user);

    }

    @Test
    void shouldThrowException_whenEmailAndUsernameAlreadyExist() {

        mockExistsByEmail(user, true);
        mockExistsByUsername(user, true);

        assertThatThrownBy(() -> service.create(user))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("both");

        verify(repository, never()).save(user);

    }

    @Test
    void shouldReturnToken_whenGeneratingActivationToken() {
        when(tokenService.generateActivationToken(user)).thenReturn("token");
        String token = service.generateActivationToken(user);
        assertThat(token).isEqualTo("token");
    }

    @Test
    void shouldSetActiveTrueAndLogHistory_whenActivatingUser() {

        User inactive = createUser("inactive@notehub.com.br", "inactive");
        inactive.setActive(false);

        when(repository.findById(inactive.getId())).thenReturn(Optional.of(inactive));
        mockSave(inactive);

        service.activate(inactive.getId());

        verify(repository).save(inactive);
        verify(historian).setHistory(inactive, "active", "false", "true");

    }

}