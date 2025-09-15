package br.com.notehub.adapter.producer.dto;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record SecretKeyGenerationDTO(
        String mailTo,
        String subject,
        String text
) {

    public static String text(String secretKey) {
        try {
            ClassPathResource resource = new ClassPathResource("template/mail/secret-key.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{secret.key}", secretKey);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static SecretKeyGenerationDTO of(String mailTo, String secretKey) {
        return new SecretKeyGenerationDTO(
                mailTo,
                "Chave Secreta",
                text(secretKey)
        );
    }

}