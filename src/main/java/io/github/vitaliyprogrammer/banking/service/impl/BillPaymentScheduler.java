package io.github.vitaliyprogrammer.banking.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the worker on a timer. Safe to run on several application instances at once: SKIP LOCKED lets them
 * claim different payments in parallel, and the lease keeps a claimed payment away from everyone else,
 * so no distributed lock (ShedLock) is needed for this job.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "banking.bill-payments.worker-enabled", havingValue = "true")
public class BillPaymentScheduler {

    private final BillPaymentProcessor billPaymentProcessor;

    /** fixedDelay, not fixedRate: the next run starts only after the previous one finished. */
    @Scheduled(fixedDelayString = "${banking.bill-payments.processing-delay}")
    public void processPending() {

        billPaymentProcessor.processPending();
    }

    @Scheduled(fixedDelayString = "${banking.bill-payments.reconciliation-delay}")
    public void reconcileUnknown() {

        billPaymentProcessor.reconcileUnknown();
    }
}
