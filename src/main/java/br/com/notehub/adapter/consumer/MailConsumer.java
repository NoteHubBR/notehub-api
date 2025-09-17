package br.com.notehub.adapter.consumer;

import br.com.notehub.adapter.consumer.dto.ActivationDTO;
import br.com.notehub.adapter.consumer.dto.EmailChangeDTO;
import br.com.notehub.adapter.consumer.dto.PasswordChangeDTO;
import br.com.notehub.adapter.consumer.dto.SecretKeyGenerationDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailConsumer {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.friendly.name}")
    private String friendlyName;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public void sendActivationMail(ActivationDTO dto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom(String.format("%s <%s>", friendlyName, mailFrom));
        helper.setTo(dto.mailTo());
        helper.setSubject(dto.subject());
        helper.setText(dto.text(), true);
        mailSender.send(message);
    }

    public void sendSecretKeyMail(SecretKeyGenerationDTO dto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom(String.format("%s <%s>", friendlyName, mailFrom));
        helper.setTo(dto.mailTo());
        helper.setSubject(dto.subject());
        helper.setText(dto.text(), true);
        mailSender.send(message);
    }

    public void sendPasswordChangeMail(PasswordChangeDTO dto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom(String.format("%s <%s>", friendlyName, mailFrom));
        helper.setTo(dto.mailTo());
        helper.setSubject(dto.subject());
        helper.setText(dto.text(), true);
        mailSender.send(message);
    }

    public void sendEmailChangeMail(EmailChangeDTO dto) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom(String.format("%s <%s>", friendlyName, mailFrom));
        helper.setTo(dto.mailTo());
        helper.setSubject(dto.subject());
        helper.setText(dto.text(), true);
        mailSender.send(message);
    }

    @RabbitListener(queues = "${broker.queue.activation.name}")
    public void activationQueueListenner(@Payload ActivationDTO dto) throws MessagingException {
        try {
            sendActivationMail(dto);
        } catch (MessagingException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @RabbitListener(queues = "${broker.queue.secret.name}")
    public void secretQueueListenner(@Payload SecretKeyGenerationDTO dto) throws MessagingException {
        try {
            sendSecretKeyMail(dto);
        } catch (MessagingException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @RabbitListener(queues = "${broker.queue.password.name}")
    public void passwordQueueListenner(@Payload PasswordChangeDTO dto) throws MessagingException {
        try {
            sendPasswordChangeMail(dto);
        } catch (MessagingException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @RabbitListener(queues = "${broker.queue.email.name}")
    public void emailQueueListenner(@Payload EmailChangeDTO dto) throws MessagingException {
        try {
            sendEmailChangeMail(dto);
        } catch (MessagingException exception) {
            System.out.println(exception.getMessage());
        }
    }

}