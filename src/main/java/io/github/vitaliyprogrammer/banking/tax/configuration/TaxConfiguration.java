package io.github.vitaliyprogrammer.banking.tax.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TaxProperties.class)
public class TaxConfiguration {
}
