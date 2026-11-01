package io.github.vitaliyprogrammer.banking.exchange.configuration;

import io.github.vitaliyprogrammer.banking.payment.configuration.BillPaymentConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Scheduling itself is already switched on by {@link BillPaymentConfiguration}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class ExchangeRateConfiguration {
}
