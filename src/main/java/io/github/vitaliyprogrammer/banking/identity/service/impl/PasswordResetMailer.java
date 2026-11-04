package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.identity.configuration.PasswordResetProperties;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordChangedEvent;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordResetRequestedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the password emails. AFTER_COMMIT: a link whose token did not reach the database is never sent.
 * {@code @Async}: SMTP time never shows in the response, which would otherwise tell which addresses have an account.
 * A failed email is only logged: the client can simply ask again.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailer {

    private final JavaMailSender mailSender;

    private final PasswordResetProperties properties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendResetLink(PasswordResetRequestedEvent event) {

        String link = properties.linkBase() + "?token=" + URLEncoder.encode(event.rawToken(), StandardCharsets.UTF_8);
        long minutes = properties.tokenTtl().toMinutes();

        send(event.email(), "Відновлення пароля",
                "Вітаємо, " + event.firstName() + "!\n\n"
                        + "Щоб задати новий пароль, відкрийте посилання (діє " + minutes + " хв і лише один раз):\n"
                        + link + "\n\n"
                        + "Якщо ви не просили змінити пароль, просто проігноруйте цей лист: ваш пароль лишається тим самим.");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendPasswordChanged(PasswordChangedEvent event) {

        send(event.email(), "Пароль змінено",
                "Вітаємо, " + event.firstName() + "!\n\n"
                        + "Пароль до вашого акаунта щойно змінено, а всі сесії на інших пристроях завершено.\n\n"
                        + "Якщо це були не ви, негайно зверніться до служби підтримки банку.");
    }

    private void send(String to, String subject, String text) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.mailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
            log.info("Password email \"{}\" sent", subject);
        } catch (MailException | MessagingException e) {
            log.warn("Password email \"{}\" was not sent: {}", subject, e.getMessage());
        }
    }
}
