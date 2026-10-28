package io.github.vitaliyprogrammer.banking.configuration;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Code asks the injected {@link Clock} for the current time instead of calling {@code Instant.now()},
 * so tests can replace it with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    /**
     * Ticks in microseconds: PostgreSQL stores TIMESTAMPTZ with microsecond precision, so a value
     * returned right after saving is the same value that is later read back from the database.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
