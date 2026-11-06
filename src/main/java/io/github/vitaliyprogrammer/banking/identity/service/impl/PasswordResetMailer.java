package io.github.vitaliyprogrammer.banking.identity.service.impl;

import io.github.vitaliyprogrammer.banking.identity.configuration.PasswordResetProperties;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordChangedEvent;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordResetRequestedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
 * A mail server that fails for a moment (a timeout, a dropped connection) gets two more tries. After that the email
 * is only logged: a reset link can simply be asked for again.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailer {

    private static final int MAX_ATTEMPTS = 3;

    /** Grows with each try: 0.5 s, then 1 s. Short, because this runs on the shared async pool. */
    private static final Duration RETRY_PAUSE = Duration.ofMillis(500);

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

    /**
     * "Пароль змінено" is a security warning: if someone else changed the password, this email is how the owner
     * learns about it. Losing it to one slow answer of the mail server would be wrong, so a failed send is retried.
     */
    private void send(String to, String subject, String text) {

        MimeMessage message;
        try {
            message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.mailFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
        } catch (MessagingException e) {
            log.warn("Password email \"{}\" could not be built: {}", subject, e.getMessage());
            return;
        }

        for (int attempt = 1; ; attempt++) {
            try {
                mailSender.send(message);
                log.info("Password email \"{}\" sent", subject);
                return;
            } catch (MailException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Password email \"{}\" was not sent after {} attempts: {}", subject, attempt,
                            e.getMessage());
                    return;
                }
                log.info("Password email \"{}\" failed (attempt {}), trying again: {}", subject, attempt,
                        e.getMessage());
                if (!pause(RETRY_PAUSE.multipliedBy(attempt))) {
                    return;
                }
            }
        }
    }

    /** False when the application is shutting down: then stop trying and let the thread end. */
    private static boolean pause(Duration duration) {

        try {
            Thread.sleep(duration);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
