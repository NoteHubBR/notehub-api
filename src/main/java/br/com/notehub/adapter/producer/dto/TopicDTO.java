package br.com.notehub.adapter.producer.dto;

import br.com.notehub.domain.user.User;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record TopicDTO(
        String mailTo,
        String subject,
        String text
) {

    public static String text(String mailTemplate, String client, String username) {
        try {
            ClassPathResource resource = new ClassPathResource(String.format("template/mail/topic/%s.html", mailTemplate));
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template
                    .replace("{api.client.host}", client)
                    .replace("{username}", username);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public TopicDTO(String subject, String template, String client, User user) {
        this(
                user.getEmail(),
                subject,
                text(template, client, user.getUsername())
        );
    }

}