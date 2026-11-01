package io.github.vitaliyprogrammer.banking.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.credit.entity.status.CreditStatementStatus;
import io.github.vitaliyprogrammer.banking.credit.event.CreditStatementMessage;
import io.github.vitaliyprogrammer.banking.notification.dto.response.NotificationResponseDto;
import io.github.vitaliyprogrammer.banking.notification.exception.NotificationNotFoundException;
import io.github.vitaliyprogrammer.banking.notification.service.NotificationService;
import io.github.vitaliyprogrammer.banking.notification.service.impl.NotificationListener;
import io.github.vitaliyprogrammer.banking.outbox.service.impl.OutboxRelay;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentStatus;
import io.github.vitaliyprogrammer.banking.payment.entity.status.PaymentType;
import io.github.vitaliyprogrammer.banking.payment.event.PaymentMessage;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proof tests for phase 9.2. The same event twice makes one notification; a broken or unknown event is refused
 * (so the listener dead-letters it); the consumer really reads from RabbitMQ; a mail server that is down
 * never loses a notification, and an email is retried with backoff, then given up.
 */
class NotificationIntegrationTests extends IntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void theSameEventDeliveredTwiceMakesOneNotification() {
        UUID userId = createUser();
        UUID eventId = UUID.randomUUID();
        String payload = jsonMapper.writeValueAsString(topUpMessage(userId));

        assertThat(notificationService.record(eventId, "payment.completed", payload)).isTrue();
        assertThat(notificationService.record(eventId, "payment.completed", payload)).isFalse();

        assertThat(notificationsOf(userId)).singleElement().satisfies(notification -> {
            assertThat(notification.title()).isEqualTo("Поповнення");
            assertThat(notification.body()).startsWith("+250.00 UAH");
        });
    }

    @Test
    void anUnknownOrBrokenEventIsRefusedSoTheListenerDeadLettersIt() {
        assertThatThrownBy(() -> notificationService.record(UUID.randomUUID(), "payment.teleported", "{}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> notificationService.record(UUID.randomUUID(), "payment.completed", "not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theConsumerTurnsAPublishedPaymentIntoANotification() throws Exception {
        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        paymentService.topUp(userId, UUID.randomUUID().toString(), new TopUpRequestDto(account, new BigDecimal("250.00")));

        MessageListenerContainer listener = listenerRegistry.getListenerContainer(NotificationListener.LISTENER_ID);
        listener.start();
        try {
            while (outboxRelay.publishDue() > 0) {
                // publish everything due, other test classes' events included
            }
            assertThat(waitForNotificationsOf(userId)).singleElement()
                    .satisfies(notification -> assertThat(notification.eventType()).isEqualTo("payment.completed"));
        } finally {
            listener.stop();
        }
    }

    @Test
    void aMailServerThatIsDownDelaysTheEmailButNeverLosesTheNotification() {
        UUID userId = createUser();
        UUID eventId = UUID.randomUUID();
        notificationService.record(eventId, "credit.statement.settled",
                jsonMapper.writeValueAsString(overdueStatement(userId)));
        // Other tests leave pending emails too; this test is about this one, so the others wait.
        jdbcTemplate.update("""
                UPDATE notifications SET next_email_at = now() + interval '1 day'
                WHERE email_status = 'PENDING' AND source_event_id <> ?
                """, eventId);

        // The mail server is in trouble and refuses everything: the email waits, with a growing delay.
        mailServer.refuseEverything();
        notificationService.sendDueEmails();
        Map<String, Object> afterFirstFailure = emailStateOf(eventId);
        assertThat(afterFirstFailure.get("email_status")).isEqualTo("PENDING");
        assertThat(afterFirstFailure.get("email_attempts")).isEqualTo(1);
        assertThat(afterFirstFailure.get("email_error")).isNotNull();
        assertThat(((Timestamp) afterFirstFailure.get("next_email_at")).toInstant()).isAfter(Instant.now());

        // Not due yet: a second run right away does not hammer the mail server.
        notificationService.sendDueEmails();
        assertThat(emailStateOf(eventId).get("email_attempts")).isEqualTo(1);

        // The last allowed attempt fails too: the email is given up, the notification stays.
        // The time comes from the JVM, like the job's own clock: the database container's clock may drift from it.
        jdbcTemplate.update("UPDATE notifications SET email_attempts = 5, next_email_at = ? WHERE source_event_id = ?",
                Timestamp.from(Instant.now().minusSeconds(1)), eventId);
        notificationService.sendDueEmails();
        assertThat(emailStateOf(eventId).get("email_status")).isEqualTo("FAILED");
        assertThat(notificationsOf(userId)).hasSize(1);
    }

    @Test
    void aClientReadsOnlyTheirOwnNotifications() {
        UUID userId = createUser();
        notificationService.record(UUID.randomUUID(), "payment.completed",
                jsonMapper.writeValueAsString(topUpMessage(userId)));
        notificationService.record(UUID.randomUUID(), "payment.completed",
                jsonMapper.writeValueAsString(topUpMessage(userId)));
        assertThat(notificationService.getUnreadCount(userId)).isEqualTo(2);

        UUID first = notificationsOf(userId).getFirst().id();
        assertThatThrownBy(() -> notificationService.markRead(createUser(), first))
                .isInstanceOf(NotificationNotFoundException.class);

        assertThat(notificationService.markRead(userId, first).readAt()).isNotNull();
        assertThat(notificationService.getUnreadCount(userId)).isEqualTo(1);

        notificationService.markAllRead(userId);
        assertThat(notificationService.getUnreadCount(userId)).isZero();
    }

    private List<NotificationResponseDto> notificationsOf(UUID userId) {
        return notificationService.getNotifications(userId, 0, 50).content();
    }

    /** The consumer runs on its own thread: give it a few seconds, then fail with what is there. */
    private List<NotificationResponseDto> waitForNotificationsOf(UUID userId) throws InterruptedException {
        for (int attempt = 0; attempt < 100 && notificationsOf(userId).isEmpty(); attempt++) {
            Thread.sleep(100);
        }
        return notificationsOf(userId);
    }

    private Map<String, Object> emailStateOf(UUID eventId) {
        return jdbcTemplate.queryForMap(
                "SELECT email_status, email_attempts, email_error, next_email_at FROM notifications "
                        + "WHERE source_event_id = ?", eventId);
    }

    private static PaymentMessage topUpMessage(UUID userId) {
        return new PaymentMessage(UUID.randomUUID(), userId, PaymentType.TOP_UP, PaymentStatus.COMPLETED,
                Currency.UAH, new BigDecimal("250.00"), new BigDecimal("0.00"), "Поповнення рахунку", null,
                Instant.now());
    }

    private static CreditStatementMessage overdueStatement(UUID userId) {
        return new CreditStatementMessage(UUID.randomUUID(), userId, "2026-10", CreditStatementStatus.OVERDUE,
                new BigDecimal("3060.00"), new BigDecimal("0.00"), new BigDecimal("76.50"), new BigDecimal("100.00"),
                LocalDate.of(2026, 11, 30), Instant.now());
    }
}
