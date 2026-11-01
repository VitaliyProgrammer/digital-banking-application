package io.github.vitaliyprogrammer.banking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.IntegrationTest;
import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.credit.dto.request.CreditApplicationRequestDto;
import io.github.vitaliyprogrammer.banking.credit.service.CreditApplicationService;
import io.github.vitaliyprogrammer.banking.outbox.configuration.OutboxProperties;
import io.github.vitaliyprogrammer.banking.outbox.service.impl.OutboxRelay;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TopUpRequestDto;
import io.github.vitaliyprogrammer.banking.payment.dto.request.TransferRequestDto;
import io.github.vitaliyprogrammer.banking.payment.service.PaymentService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proof tests for phase 9.1. An event exists if and only if its business change committed; the relay hands it
 * to a real RabbitMQ with the outbox id as the message id, and never publishes it twice once confirmed.
 */
class OutboxIntegrationTests extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CreditApplicationService creditApplicationService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private OutboxProperties outboxProperties;

    /** A queue only this test listens on, bound like a real consumer would bind its own. */
    private Queue paymentsQueue;

    @BeforeEach
    void listenToPayments() {
        TopicExchange exchange = new TopicExchange(outboxProperties.exchange(), true, false);
        // Not an AnonymousQueue: that one is exclusive to the connection that declared it, and dies with it.
        paymentsQueue = new Queue("test.payments." + UUID.randomUUID(), false, false, false);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(paymentsQueue);
        amqpAdmin.declareBinding(BindingBuilder.bind(paymentsQueue).to(exchange).with("payment.*"));
    }

    @AfterEach
    void stopListening() {
        amqpAdmin.deleteQueue(paymentsQueue.getName());
    }

    @Test
    void anEventIsWrittenOnlyTogetherWithItsPayment() {
        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID topUp = paymentService.topUp(userId, newKey(), new TopUpRequestDto(account, new BigDecimal("100.00"))).id();

        // A transfer that fails at the ledger: its payment row was already written, then everything rolled back.
        String recipientIban = accountService.getAccount(userId, account).number();
        UUID otherUser = createUser();
        UUID otherAccount = accountService.open(otherUser, new OpenAccountRequestDto(Currency.UAH)).id();
        assertThatThrownBy(() -> paymentService.transfer(otherUser, newKey(),
                new TransferRequestDto(otherAccount, recipientIban, new BigDecimal("500.00"), null)))
                .isInstanceOf(InsufficientFundsException.class);

        // The top-up committed, so its event is there; the failed transfer rolled back, and so did its event.
        assertThat(eventsAbout(topUp)).singleElement()
                .satisfies(event -> assertThat(event.get("event_type")).isEqualTo("payment.completed"));
        assertThat(eventCountOfUser(otherUser)).isZero();
    }

    @Test
    void theRelayPublishesEachEventOnceWithTheOutboxIdAsMessageId() {
        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        UUID topUp = paymentService.topUp(userId, newKey(), new TopUpRequestDto(account, new BigDecimal("100.00"))).id();
        String eventId = eventsAbout(topUp).getFirst().get("id").toString();

        publishEverythingDue();

        List<Message> ours = receivedMessagesWithId(eventId);
        assertThat(ours).singleElement().satisfies(message -> {
            assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("payment.completed");
            assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains(topUp.toString());
        });
        Map<String, Object> row = eventsAbout(topUp).getFirst();
        assertThat(row.get("published_at")).isNotNull();
        assertThat(row.get("attempts")).isEqualTo(1);

        // Running the relay again publishes nothing new for an event the broker already confirmed.
        publishEverythingDue();
        assertThat(receivedMessagesWithId(eventId)).isEmpty();
    }

    @Test
    void aCreditDecisionIsAnnouncedWithItsOutcome() {
        UUID userId = createUser();
        UUID account = accountService.open(userId, new OpenAccountRequestDto(Currency.UAH)).id();
        jdbcTemplate.update("UPDATE accounts SET created_at = now() - interval '60 days' WHERE id = ?", account);
        paymentService.topUp(userId, newKey(), new TopUpRequestDto(account, new BigDecimal("60000.00")));

        UUID application = creditApplicationService.apply(userId,
                new CreditApplicationRequestDto(new BigDecimal("8000.00"))).id();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT payload ->> 'status' FROM outbox_events
                WHERE aggregate_id = ? AND event_type = 'credit.application.decided'
                """, String.class, application)).isEqualTo("APPROVED");
    }

    /** Other test classes leave their events unpublished (the relay is off in tests): publish all of them. */
    private void publishEverythingDue() {
        while (outboxRelay.publishDue() > 0) {
            // next batch
        }
    }

    private List<Message> receivedMessagesWithId(String messageId) {
        List<Message> ours = new ArrayList<>();
        Message message;
        while ((message = rabbitTemplate.receive(paymentsQueue.getName(), 1000)) != null) {
            if (messageId.equals(message.getMessageProperties().getMessageId())) {
                ours.add(message);
            }
        }
        return ours;
    }

    private List<Map<String, Object>> eventsAbout(UUID aggregateId) {
        return jdbcTemplate.queryForList("SELECT * FROM outbox_events WHERE aggregate_id = ?", aggregateId);
    }

    private Integer eventCountOfUser(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE payload ->> 'userId' = ?", Integer.class, userId.toString());
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }
}
