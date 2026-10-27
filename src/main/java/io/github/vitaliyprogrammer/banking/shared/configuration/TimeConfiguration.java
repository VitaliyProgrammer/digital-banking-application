package io.github.vitaliyprogrammer.banking.shared.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Code asks the injected {@link Clock} for the current time instead of calling {@code Instant.now()},
 * so tests can replace it with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
