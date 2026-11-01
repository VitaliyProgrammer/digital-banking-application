package io.github.vitaliyprogrammer.banking.common.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Switches scheduling on once, for the whole application. Every domain with a job (bill payments, rates, savings,
 * cashback, credit statements, the outbox, emails) only declares its own @Scheduled method.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfiguration {
}
