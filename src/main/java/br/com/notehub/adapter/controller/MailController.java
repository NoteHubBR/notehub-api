package br.com.notehub.adapter.controller;

import br.com.notehub.adapter.producer.MailProducer;
import br.com.notehub.domain.user.User;
import br.com.notehub.domain.user.UserService;
import br.com.notehub.infra.exception.CustomExceptions;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@CrossOrigin(origins = {"https://notehub.com.br"})
@Hidden
@RequestMapping("/api/v1/mail")
@RequiredArgsConstructor
public class MailController {

    private final UserService service;
    private final MailProducer producer;

    @Value("${api.server.security.token.secret}")
    private String secret;

    private void validateSecret(HttpServletRequest request) {
        String xSecret = request.getHeader("X-Secret");
        if (Objects.equals(xSecret, secret)) return;
        throw new CustomExceptions.InvalidSecretException();
    }

    @GetMapping("/release")
    public void sendReleaseMail(
            HttpServletRequest request
    ) {
        validateSecret(request);
        for (User user : service.getAllActiveUsers())
            producer.publishTopicMessage(
                    "✨ Novidades",
                    "release",
                    user
            );
    }

}