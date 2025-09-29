package br.com.notehub.application.dto.request.user;

import br.com.notehub.application.validation.constraints.NoForbiddenWords;
import br.com.notehub.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeUserREQ(

        @NoForbiddenWords(message = "Não pode")
        @NotBlank(message = "Não pode ser vazio")
        @Pattern(
                regexp = "^[a-zA-Z0-9_.-]+$",
                message = "Apenas letras, números, _ e ."
        )
        @Size(min = 2, max = 12, message = "Tamanho inválido")
        String username,

        @NoForbiddenWords(message = "Não pode")
        @Pattern(
                regexp = "^(?!.*[\\u00A0\\u2007\\u202F]).*$",
                message = "👀"
        )
        @NotBlank(message = "Não pode ser vazio")
        @Size(min = 2, max = 24, message = "Tamanho inválido")
        String displayName,

        @Pattern(
                regexp = "^https://(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})(?!:)(/\\S*)?$",
                message = "Link inválido"
        )
        String avatar,

        @Pattern(
                regexp = "^https://(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})(?!:)(/\\S*)?$",
                message = "Link inválido"
        )
        String banner,

        @Size(max = 144, message = "Tamanho inválido")
        @Pattern(
                regexp = "^(?!.*[\\u00A0\\u2007\\u202F]).*$",
                message = "👀"
        )
        String message,

        boolean profilePrivate

) {
    public User toUser() {
        return new User(username.toLowerCase(), displayName, avatar, banner, message, profilePrivate);
    }
}