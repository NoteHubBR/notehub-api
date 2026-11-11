package br.com.notehub.application.dto.request.note;

import br.com.notehub.application.validation.constraints.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeNameREQ(
        @NotBlank(message = "Não pode ser vazio")
        @ValidName
        @Size(min = 1, max = 255, message = "Tamanho inválido")
        String name
) {
}