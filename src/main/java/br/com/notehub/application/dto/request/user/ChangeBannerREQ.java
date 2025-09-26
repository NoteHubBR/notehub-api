package br.com.notehub.application.dto.request.user;

import jakarta.validation.constraints.Pattern;

public record ChangeBannerREQ(
        @Pattern(
                regexp = "^https://(([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})(?!:)(/\\S*)?$",
                message = "Link inválido"
        )
        String banner
) {
}