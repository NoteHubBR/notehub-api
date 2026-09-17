package br.com.notehub.implementation.note;

import br.com.notehub.application.dto.request.note.CreateNoteREQ;
import br.com.notehub.infra.exception.CustomExceptions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class NoteCreationTest extends AbstractNoteServiceTest {

    @Test
    void shouldThrowNoteNameAlreadyExists_whenCreatingNoteWithDuplicateName() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(repository.existsByUserUsernameAndName("owner", "old-name")).thenReturn(true);

        CreateNoteREQ req = new CreateNoteREQ("old-name", "desc", "# md", false, false, null);

        assertThatThrownBy(() -> service.create(owner.getId(), req))
                .isInstanceOf(CustomExceptions.NoteNameAlreadyExists.class);

        verify(repository, never()).save(any());
    }

}