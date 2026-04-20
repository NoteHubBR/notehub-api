package br.com.notehub.application.dto.request.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthSessionsREQ(
        @NotBlank(message = "Não pode ser vazio")
        @Size(min = 4, max = 255, message = "Tamanho inválido")
        String password
) {
}