package br.com.notehub.adapter.producer;

import br.com.notehub.adapter.producer.dto.*;
import br.com.notehub.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailProducer {

    @Value("${broker.queue.activation.name}")
    private String activationRoutingKey;

    @Value("${broker.queue.secret.name}")
    private String secretRoutingKey;

    @Value("${broker.queue.password.name}")
    private String passwordRoutingKey;

    @Value("${broker.queue.email.name}")
    private String emailRoutingKey;

    @Value("${broker.queue.topic.name}")
    private String topicRoutingKey;

    @Value("${api.client.host}")
    private String client;

    private final RabbitTemplate rabbitTemplate;

    public void publishAccountActivationMessage(String jwt, User user) {
        var message = new ActivationDTO(client, jwt, user);
        rabbitTemplate.convertAndSend("", activationRoutingKey, message);
    }

    public void publishAccountSecretKeyGenerationMessage(String mailTo, String secretKey) {
        var message = SecretKeyGenerationDTO.of(client, mailTo, secretKey);
        rabbitTemplate.convertAndSend("", secretRoutingKey, message);
    }

    public void publishAccountPasswordChangeMessage(String mailTo, String token) {
        var message = PasswordChangeDTO.of(client, mailTo, token);
        rabbitTemplate.convertAndSend("", passwordRoutingKey, message);
    }

    public void publishAccountEmailChangeMessage(String mailTo, String token) {
        var message = EmailChangeDTO.of(client, mailTo, token);
        rabbitTemplate.convertAndSend("", emailRoutingKey, message);
    }

    public void publishTopicMessage(String subject, String template, User user) {
        var message = new TopicDTO(subject, template, client, user);
        rabbitTemplate.convertAndSend("", topicRoutingKey, message);
    }

}