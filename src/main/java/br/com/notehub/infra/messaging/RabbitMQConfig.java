package br.com.notehub.infra.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${broker.queue.activation.name}")
    private String activation;

    @Value("${broker.queue.secret.name}")
    private String secret;

    @Value("${broker.queue.password.name}")
    private String password;

    @Value("${broker.queue.email.name}")
    private String email;

    @Bean
    public Queue activationQueue() {
        return new Queue(activation, true);
    }

    @Bean
    public Queue secretQueue() {
        return new Queue(secret, true);
    }

    @Bean
    public Queue passwordQueue() {
        return new Queue(password, true);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(email, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}