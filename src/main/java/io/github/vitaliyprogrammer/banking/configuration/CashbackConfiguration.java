package io.github.vitaliyprogrammer.banking.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CashbackProperties.class)
public class CashbackConfiguration {
}
