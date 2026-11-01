package io.github.vitaliyprogrammer.banking.payment.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ProviderProperties.class, BillPaymentProperties.class})
public class BillPaymentConfiguration {
}
