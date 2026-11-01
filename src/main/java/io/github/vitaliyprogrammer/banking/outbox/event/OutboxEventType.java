package io.github.vitaliyprogrammer.banking.outbox.event;

import java.util.Arrays;
import java.util.Optional;

/**
 * Every event the bank announces to the outside world. The routing key is a dotted name, so a consumer can bind
 * to exactly what it needs: "payment.*" for all payments, "credit.#" for everything about credit.
 */
public enum OutboxEventType {

    PAYMENT_COMPLETED("payment.completed"),

    /** A bill payment the provider rejected, or never received: the money came back. */
    PAYMENT_FAILED("payment.failed"),

    /** APPROVED or DECLINED, by the rules or by a person. */
    CREDIT_APPLICATION_DECIDED("credit.application.decided"),

    /** A statement after its due date: PAID, MINIMUM_PAID or OVERDUE, with what was charged. */
    CREDIT_STATEMENT_SETTLED("credit.statement.settled"),

    /** Support answered the client's ticket. */
    SUPPORT_TICKET_REPLIED("support.ticket.replied"),

    SUPPORT_TICKET_RESOLVED("support.ticket.resolved");

    private final String routingKey;

    OutboxEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }

    /** For consumers: the routing key a message arrived with, or empty for an event this code does not know. */
    public static Optional<OutboxEventType> fromRoutingKey(String routingKey) {
        return Arrays.stream(values()).filter(type -> type.routingKey.equals(routingKey)).findFirst();
    }
}
