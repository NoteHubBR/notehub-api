package br.com.notehub.application.dto.request.user;

import br.com.notehub.application.validation.constraints.NoForbiddenWords;
import br.com.notehub.application.validation.constraints.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeUsernameREQ(
        @NoForbiddenWords(message = "Não pode")
        @NotBlank(message = "Não pode ser vazio")
        @ValidName
        @Size(min = 2, max = 24, message = "Tamanho inválido")
        String username
) {
}