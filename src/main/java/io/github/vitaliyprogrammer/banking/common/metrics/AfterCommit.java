package io.github.vitaliyprogrammer.banking.common.metrics;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs an action only once the current transaction has committed. For business counters: a payment that rolled back
 * did not happen, so it must not appear on the dashboard either. Knows no domain; each domain counts its own things.
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    /** Outside of a transaction (a plain call) the action is already final, so it runs at once. */
    public static void run(Runnable action) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
