package io.github.vitaliyprogrammer.banking.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Scheduling itself is already switched on by {@link BillPaymentConfiguration}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class ExchangeRateConfiguration {
}
