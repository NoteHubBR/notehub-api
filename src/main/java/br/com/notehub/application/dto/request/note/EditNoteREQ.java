package br.com.notehub.application.dto.request.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EditNoteREQ(
        @Pattern(
                regexp = "^(?!.*[\\u00A0\\u2007\\u202F]).*$",
                message = "👀"
        )
        @NotBlank(message = "Não pode ser vazio")
        @Size(min = 2, max = 48, message = "Tamanho inválido")
        String title,

        @Pattern(
                regexp = "^(?!.*[\\u00A0\\u2007\\u202F]).*$",
                message = "👀"
        )
        @Size(max = 255, message = "Além do limite")
        String description,

        @Size(max = 12, message = "Além do limite")
        List<
                @NotBlank(message = "Não pode ser vazio")
                @Pattern(
                        regexp = "^(?!.*[\\p{Zs}\\u00A0\\u2007\\u202F]).*$",
                        message = "Não use espaços"
                )
                @Size(min = 2, max = 20, message = "Tamanho inválido")
                        String> tags,

        boolean closed,

        boolean hidden
) {
}