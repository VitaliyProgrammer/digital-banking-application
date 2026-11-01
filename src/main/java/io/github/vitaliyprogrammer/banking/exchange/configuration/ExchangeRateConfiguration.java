package io.github.vitaliyprogrammer.banking.exchange.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Scheduling itself is switched on once for the whole application (common SchedulingConfiguration). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class ExchangeRateConfiguration {
}
