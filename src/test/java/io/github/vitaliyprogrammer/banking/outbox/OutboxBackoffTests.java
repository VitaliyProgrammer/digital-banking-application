package io.github.vitaliyprogrammer.banking.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vitaliyprogrammer.banking.service.impl.OutboxBackoff;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxBackoffTests {

    private static final Duration MAX = Duration.ofMinutes(5);

    @Test
    void eachFailureDoublesTheWait() {
        assertThat(OutboxBackoff.delayAfter(1, MAX)).isEqualTo(Duration.ofSeconds(2));
        assertThat(OutboxBackoff.delayAfter(2, MAX)).isEqualTo(Duration.ofSeconds(4));
        assertThat(OutboxBackoff.delayAfter(5, MAX)).isEqualTo(Duration.ofSeconds(32));
    }

    @Test
    void theWaitNeverExceedsTheMaximumEvenAfterAWeekOfOutage() {
        assertThat(OutboxBackoff.delayAfter(9, MAX)).isEqualTo(MAX);
        // Without the cap on the exponent, 1 << 70 would overflow into nonsense.
        assertThat(OutboxBackoff.delayAfter(70, MAX)).isEqualTo(MAX);
    }
}
