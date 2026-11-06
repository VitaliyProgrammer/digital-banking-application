package io.github.vitaliyprogrammer.banking.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.vitaliyprogrammer.banking.identity.configuration.PasswordResetProperties;
import io.github.vitaliyprogrammer.banking.identity.event.PasswordChangedEvent;
import io.github.vitaliyprogrammer.banking.identity.service.impl.PasswordResetMailer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * A mail server that does not answer for a moment must not cost the client the "password changed" warning.
 * The tries are counted on a mocked sender: the real SMTP path is covered by {@code PasswordResetIntegrationTests}.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetMailerTests {

    private static final PasswordChangedEvent CHANGED = new PasswordChangedEvent("olena@bank.local", "Olena");

    @Mock
    private JavaMailSender mailSender;

    private PasswordResetMailer mailer;

    @BeforeEach
    void setUp() {
        mailer = new PasswordResetMailer(mailSender, new PasswordResetProperties(Duration.ofMinutes(30),
                "http://localhost:8080/demo/reset.html", "no-reply@digital-bank.local", 3, Duration.ofHours(1)));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void oneFailedTryIsRetried() {
        doThrow(new MailSendException("Read timed out")).doNothing().when(mailSender).send(any(MimeMessage.class));

        mailer.sendPasswordChanged(CHANGED);

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void afterThreeTriesTheEmailIsGivenUpQuietly() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        // Runs after commit on its own thread: an exception here would only end up in a log, never with the client.
        assertThatCode(() -> mailer.sendPasswordChanged(CHANGED)).doesNotThrowAnyException();

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void sentEmailIsSentOnce() {
        doNothing().when(mailSender).send(any(MimeMessage.class));

        mailer.sendPasswordChanged(CHANGED);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
