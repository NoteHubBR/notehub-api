package br.com.notehub.adapter.consumer.dto;

public record MailDTO(
        String mailTo,
        String subject,
        String text
) {
}