package io.github.vitaliyprogrammer.banking.card.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CardSecurityProperties.class)
public class CardConfiguration {
}
