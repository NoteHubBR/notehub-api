package br.com.notehub.application.dto.request.token;

import br.com.notehub.application.validation.constraints.NoForbiddenWords;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthREQ(
        @NoForbiddenWords(message = "Não pode")
        @NotBlank
        @Size(min = 2, max = 255, message = "Tamanho inválido")
        String identifier,

        @NotBlank
        @Size(min = 4, max = 255, message = "Tamanho inválido")
        String password
) {
}