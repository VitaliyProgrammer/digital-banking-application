package io.github.vitaliyprogrammer.banking.common.retry;

import java.time.Duration;

/**
 * Exponential backoff for retries: 2 s, 4 s, 8 s... up to a maximum. Shared by the outbox relay (events) and the
 * notification emails, so it lives in common rather than inside either of them. Pure arithmetic, unit-tested.
 */
public final class RetryBackoff {

    /** 2^30 s is already decades; stopping the shift here keeps it from overflowing. */
    private static final int MAX_EXPONENT = 30;

    private RetryBackoff() {
    }

    /** The wait after the given number of failed attempts (1 = the first failure). */
    public static Duration delayAfter(int failedAttempts, Duration max) {

        int exponent = Math.clamp(failedAttempts, 1, MAX_EXPONENT);
        Duration delay = Duration.ofSeconds(1L << exponent);

        return delay.compareTo(max) > 0 ? max : delay;
    }
}
