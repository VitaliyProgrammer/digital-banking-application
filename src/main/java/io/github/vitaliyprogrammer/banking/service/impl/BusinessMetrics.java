package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.entity.status.CreditApplicationStatus;
import io.github.vitaliyprogrammer.banking.entity.status.DecisionSource;
import io.github.vitaliyprogrammer.banking.entity.status.PaymentType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Counters of what the bank does, not of how the server feels. Every count is made AFTER the transaction commits:
 * a payment that rolled back did not happen, so it must not appear on the dashboard either.
 * Tags hold only small, fixed sets of values (types, statuses): a user id as a tag would create a time series
 * per client and overload Prometheus.
 */
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry registry;

    public void paymentCompleted(PaymentType type) {

        afterCommit(() -> payments(type, "completed").increment());
    }

    /** A bill payment the provider rejected or never received: the money came back. */
    public void paymentFailed(PaymentType type) {

        afterCommit(() -> payments(type, "failed").increment());
    }

    /** Every application, by what the rules said: APPROVED, DECLINED or MANUAL_REVIEW. */
    public void creditApplicationScored(CreditApplicationStatus outcome) {

        afterCommit(() -> Counter.builder("bank.credit.applications")
                .description("Credit applications by the outcome of automatic scoring")
                .tag("outcome", tag(outcome))
                .register(registry)
                .increment());
    }

    /** A final decision: by the rules (AUTO) or by a person (MANUAL). */
    public void creditDecided(CreditApplicationStatus status, DecisionSource source) {

        afterCommit(() -> Counter.builder("bank.credit.decisions")
                .description("Final credit decisions")
                .tag("status", tag(status))
                .tag("source", tag(source))
                .register(registry)
                .increment());
    }

    public void ticketOpened() {

        afterCommit(() -> tickets("opened").increment());
    }

    public void ticketResolved() {

        afterCommit(() -> tickets("resolved").increment());
    }

    private Counter payments(PaymentType type, String result) {

        return Counter.builder("bank.payments")
                .description("Payments that reached a final state")
                .tag("type", tag(type))
                .tag("result", result)
                .register(registry);
    }

    private Counter tickets(String event) {

        return Counter.builder("bank.support.tickets")
                .description("Support tickets opened and resolved")
                .tag("event", event)
                .register(registry);
    }

    private static String tag(Enum<?> value) {

        return value.name().toLowerCase(Locale.ROOT);
    }

    /** Outside of a transaction (a plain call) the action is already final, so count at once. */
    private static void afterCommit(Runnable count) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            count.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                count.run();
            }
        });
    }
}
