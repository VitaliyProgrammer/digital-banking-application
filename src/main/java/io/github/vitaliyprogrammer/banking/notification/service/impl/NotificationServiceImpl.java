package io.github.vitaliyprogrammer.banking.notification.service.impl;

import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.common.retry.RetryBackoff;
import io.github.vitaliyprogrammer.banking.credit.event.CreditDecisionMessage;
import io.github.vitaliyprogrammer.banking.credit.event.CreditStatementMessage;
import io.github.vitaliyprogrammer.banking.identity.service.UserService;
import io.github.vitaliyprogrammer.banking.notification.configuration.NotificationProperties;
import io.github.vitaliyprogrammer.banking.notification.dto.response.NotificationDraftDto;
import io.github.vitaliyprogrammer.banking.notification.dto.response.NotificationResponseDto;
import io.github.vitaliyprogrammer.banking.notification.entity.Notification;
import io.github.vitaliyprogrammer.banking.notification.entity.status.EmailStatus;
import io.github.vitaliyprogrammer.banking.notification.exception.NotificationNotFoundException;
import io.github.vitaliyprogrammer.banking.notification.repository.NotificationRepository;
import io.github.vitaliyprogrammer.banking.notification.service.NotificationService;
import io.github.vitaliyprogrammer.banking.outbox.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.payment.event.PaymentMessage;
import io.github.vitaliyprogrammer.banking.support.event.SupportTicketMessage;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService, MeterBinder {

    private final NotificationRepository notificationRepository;

    private final UserService userService;

    private final JavaMailSender mailSender;

    private final JsonMapper jsonMapper;

    private final TransactionTemplate transactionTemplate;

    private final NotificationProperties properties;

    private final Clock clock;

    @Override
    @Transactional
    public boolean record(UUID eventId, String eventType, String payload) {

        OutboxEventType type = OutboxEventType.fromRoutingKey(eventType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + eventType));

        NotificationDraftDto draft = switch (type) {
            case PAYMENT_COMPLETED -> NotificationComposer.paymentCompleted(read(payload, PaymentMessage.class));
            case PAYMENT_FAILED -> NotificationComposer.paymentFailed(read(payload, PaymentMessage.class));
            case CREDIT_APPLICATION_DECIDED -> NotificationComposer.creditDecided(
                    read(payload, CreditDecisionMessage.class));
            case CREDIT_STATEMENT_SETTLED -> NotificationComposer.statementSettled(
                    read(payload, CreditStatementMessage.class));
            case SUPPORT_TICKET_REPLIED -> NotificationComposer.supportReplied(read(payload, SupportTicketMessage.class));
            case SUPPORT_TICKET_RESOLVED -> NotificationComposer.supportResolved(
                    read(payload, SupportTicketMessage.class));
        };

        Instant now = clock.instant();
        EmailStatus emailStatus = draft.emailRequired() ? EmailStatus.PENDING : EmailStatus.NOT_NEEDED;

        return notificationRepository.insertIfAbsent(draft.userId(), eventId, eventType, draft.title(), draft.body(),
                now, emailStatus.name(), draft.emailRequired() ? now : null) == 1;
    }

    /**
     * One batch in one transaction, rows locked (SKIP LOCKED). The first failure ends the batch: the mail server
     * is most likely down. Each failure waits longer (2 s, 4 s, 8 s...), and after the last attempt the email
     * is given up; the notification itself is never lost.
     */
    @Override
    public int sendDueEmails() {

        Integer sent = transactionTemplate.execute(_ -> {
            Instant now = clock.instant();
            int count = 0;

            for (Notification notification : notificationRepository.lockDueEmails(now, properties.mailerBatchSize())) {
                try {
                    sendEmail(notification);
                    notification.emailSent(clock.instant());
                    count++;
                } catch (MailException | MessagingException e) {
                    int attempt = notification.getEmailAttempts() + 1;
                    Instant retryAt = attempt >= properties.maxEmailAttempts()
                            ? null
                            // Counted from the failure, not from the start of the batch: a slow SMTP answer must not
                            // eat up the wait and make the email "due" again at once.
                            : clock.instant().plus(RetryBackoff.delayAfter(attempt, properties.maxEmailBackoff()));
                    notification.emailFailed(e.getMessage(), retryAt);
                    log.warn("Email for notification {} failed, attempt {}: {}",
                            notification.getId(), attempt, e.getMessage());
                    break;
                }
            }

            return count;
        });

        return Objects.requireNonNull(sent);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<NotificationResponseDto> getNotifications(UUID userId, int page, int size) {

        return PageResponseDto.from(
                notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)),
                NotificationResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {

        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public NotificationResponseDto markRead(UUID userId, UUID notificationId) {

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(clock.instant());

        return NotificationResponseDto.from(notification);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {

        return notificationRepository.markAllRead(userId, clock.instant());
    }

    private void sendEmail(Notification notification) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(properties.mailFrom());
        helper.setTo(userService.getById(notification.getUserId()).email());
        helper.setSubject(notification.getTitle());
        helper.setText(notification.getBody());
        mailSender.send(message);
    }

    /** Emails waiting for the mail server: a number that keeps growing means mail is down. */
    @Override
    public void bindTo(MeterRegistry registry) {

        Gauge.builder("bank.notifications.emails.pending", notificationRepository,
                        repository -> repository.countByEmailStatus(EmailStatus.PENDING))
                .description("Email copies of notifications not sent yet")
                .register(registry);
    }

    /** A payload that does not match its type is a broken message, not a reason to retry. */
    private <T> T read(String payload, Class<T> type) {

        try {
            return jsonMapper.readValue(payload, type);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Broken " + type.getSimpleName() + " payload", e);
        }
    }
}
