package br.com.notehub.adapter.consumer.dto;

public record SecretKeyGenerationDTO(
        String mailTo,
        String subject,
        String text
) {
}