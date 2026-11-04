package io.github.vitaliyprogrammer.banking.identity.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PasswordResetProperties.class)
public class PasswordResetConfiguration {
}
