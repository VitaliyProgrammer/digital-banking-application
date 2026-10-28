package io.github.vitaliyprogrammer.banking.service;

import java.time.Duration;

/**
 * Counts attempts per key within a time window, shared by all application instances.
 * Used to limit failed logins and wrong PIN entries.
 */
public interface AttemptCounter {

    /**
     * Atomically adds one attempt and returns the count in the current window.
     * The window starts with the first attempt and the counter disappears when it ends.
     */
    long increment(String key, Duration window);

    void reset(String key);
}
