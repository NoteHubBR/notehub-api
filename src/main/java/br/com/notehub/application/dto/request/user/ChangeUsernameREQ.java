package br.com.notehub.application.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUsernameREQ(
        @NotBlank(message = "Não pode ser vazio")
        @Pattern(
                regexp = "^[a-zA-Z0-9_.]+$",
                message = "Apenas letras, números, _ e ."
        )
        @Size(min = 4, max = 12, message = "Tamanho inválido")
        String username
) {
}