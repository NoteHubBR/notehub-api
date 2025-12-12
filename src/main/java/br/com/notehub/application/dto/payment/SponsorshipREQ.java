package br.com.notehub.application.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SponsorshipREQ(

        @NotBlank(message = "Não pode ser vazia.")
        String locale,

        @NotBlank(message = "Não pode ser vazia.")
        String currency,

        @NotNull(message = "Não pode ser nulo.")
        @Min(value = 1, message = "Deve ser maior que 0.")
        Long amount

) {
}